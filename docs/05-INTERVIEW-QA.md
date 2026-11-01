# 05 — Interview questions this code invites

Fifty questions an interviewer can ask about **this specific project**, with answers grounded in
the code. Every one points at a real file, so you can open it and show them.

Use it as a rehearsal script, not a recitation. The goal is to be able to say "here is what I
did, here is why, and here is what I would do differently at ten times the scale."

---

## A. Core Java

**1. Why `BigDecimal` for money and not `double`?**
Binary floating point cannot represent decimal fractions like 0.1 exactly, so sums drift —
`0.1 + 0.2` is `0.30000000000000004`. On an invoice that is a bug and eventually a legal
problem. `BigDecimal` stores an arbitrary-precision unscaled value plus a scale, so 6822.76 is
exactly 6822.76. See `PricingEngine`, and `DECIMAL(12,2)` in `V1__init_schema.sql`.

**2. Why does `BigDecimal.divide` need a rounding mode?**
Because the result may be non-terminating. `BigDecimal.ONE.divide(new BigDecimal(3))` throws
`ArithmeticException` with no scale and rounding mode. `PricingEngine.percentOf` passes
`(HUNDRED, 2, RoundingMode.HALF_UP)`. This is a runtime failure that only appears for certain
inputs, which is what makes it dangerous.

**3. What is the difference between `BigDecimal.equals` and `compareTo`?**
`equals` compares **value and scale**, so `new BigDecimal("0.00").equals(BigDecimal.ZERO)` is
`false`. `compareTo` compares numeric value only. That is exactly why
`QuoteService.validate` uses `declaredValue.compareTo(BigDecimal.ZERO) <= 0`.

**4. Why `record` for DTOs?**
A DTO is an immutable carrier with no identity and no behaviour. The compiler generates the
canonical constructor, accessors, `equals`, `hashCode` and `toString` — about 60 lines saved per
DTO — and immutability is correct for something crossing a boundary. Records arrived in Java 16.

**5. When would you *not* use a record?**
When the type needs mutable state, inheritance, or JPA — records are final, have no no-arg
constructor, and their fields are final, so Hibernate cannot proxy or populate them. Hence the
entities here are ordinary classes.

**6. Why `EnumMap` and `EnumSet` in `BookingStatus`?**
`EnumMap` is backed by an array indexed by ordinal (no hashing, no boxing) and `EnumSet` by a
bit vector. Both are faster and smaller than the hash-based equivalents, and neither can hold a
key outside the enum.

**7. Is it safe to reference enum constants from a static block in the same enum?**
Yes. Static fields are initialised after all constants exist, which is why
`ALLOWED_TRANSITIONS` can be populated that way.

**8. Why `Optional<User>` instead of returning null?**
It makes absence part of the method signature, so the caller cannot forget. `AuthService` chains
`.map(...).orElseThrow(...)`, which reads as a single intent rather than an if-null branch.
Note: `Optional` is for return values, not fields or parameters.

**9. Why `Instant` rather than `LocalDateTime` for audit fields?**
An `Instant` is an unambiguous point on the global timeline (UTC). `LocalDateTime` has no zone,
so "2026-10-04 14:30" is a different real moment in Pune than in London. Audit data must never
be ambiguous. `LocalDate` is right for `moveDate`, because a move date is a calendar day.

**10. What does `.reduce(BigDecimal.ZERO, BigDecimal::add)` do, and why not `sum()`?**
It folds the stream into a single value starting from an identity. There is no
`Collectors.summingBigDecimal` in the JDK, and `mapToDouble(...).sum()` would reintroduce
floating-point error into money.

---

## B. Spring core

**11. What does `@SpringBootApplication` actually do?**
Three annotations: `@Configuration` (this class can declare beans),
`@EnableAutoConfiguration` (inspect the classpath and configure what is found — Tomcat on the
classpath becomes a web server, a JDBC driver plus a URL becomes a connection pool), and
`@ComponentScan` (scan this package downwards). The last one is why every class lives under
`com.movemate`.

**12. How does auto-configuration decide what to configure?**
Via `@Conditional` annotations on auto-configuration classes —
`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`. The key
consequence: **declaring your own bean usually wins**, because most auto-configurations back off
with `@ConditionalOnMissingBean`. That is why defining a `PasswordEncoder` replaces Spring's
default rather than conflicting with it.

**13. Constructor injection or field injection, and why?**
Constructor, everywhere in this project. The field can be `final` so the dependency cannot be
swapped at runtime; the object is impossible to construct in an invalid state; and a unit test
can pass a mock with `new PricingEngine(properties)` — no reflection, no Spring context. Which
is exactly why `PricingEngineTest` runs in milliseconds. Field injection also hides a growing
dependency list, whereas a seven-argument constructor is visible pressure to split the class.

**14. Why is `@Autowired` absent from every constructor here?**
Since Spring 4.3 it is implicit when a class has a single constructor.

**15. What scope are Spring beans by default, and what does that imply?**
Singleton: one instance serving all concurrent requests. So beans must be **stateless or
thread-safe**. That is why `JwtService` holds only an immutable `SecretKey`, and why no service
in this project has a mutable field.

**16. What is `@ConfigurationProperties` for, over `@Value`?**
Grouping related settings into one injectable, testable object; binding once at startup so a
typo fails fast instead of surfacing as a null later; and automatic type conversion. Spring Boot
3 binds records through their canonical constructor. See `PricingProperties`, `JwtProperties`,
`CorsProperties`.

**17. How does `${PORT:8080}` work and why does it matter?**
Spring resolves the environment variable first and falls back to the literal. It is the single
mechanism that lets the same artifact run on a laptop and on Railway with no code change.

**18. What are Spring profiles and how are they used here?**
Named sets of configuration. `application.properties` holds the base, and
`application-prod.properties` holds only the differences — SQL logging off, stack traces hidden,
compression and graceful shutdown on. Activated with `SPRING_PROFILES_ACTIVE=prod`.

---

## C. Spring MVC and REST

**19. What is the difference between `@Controller` and `@RestController`?**
`@RestController` = `@Controller` + `@ResponseBody`: every return value is serialised by an
`HttpMessageConverter` (Jackson) instead of resolved as a view name.

**20. What does `@Valid` actually do, and what happens without it?**
It triggers Bean Validation on the annotated argument. Without it, every `@NotBlank` and
`@Pattern` in the DTO is **inert** — the annotations are just metadata. That is a bug which is
invisible until someone registers with an empty name.

**21. `@Valid` vs `@Validated` — what is the difference?**
`@Valid` is Jakarta's and cascades into an object graph (a request body).
`@Validated` is Spring's, goes on the **class**, and enables validation of individual method
parameters via an AOP proxy — which is what makes the `@NotBlank @RequestParam`s on
`/bookings/track` effective. They also throw different exceptions, which is why
`GlobalExceptionHandler` handles both `MethodArgumentNotValidException` **and**
`ConstraintViolationException`. Handling only the first is a very common gap that produces a 500
where a 400 belongs.

**22. Why `/api/v1` in the path?**
Versioning. When a breaking change is needed you add `/v2` and leave `/v1` serving the mobile app
that has not been updated. It is the cheapest insurance in API design.

**23. Why is `POST /quotes/estimate` a POST when it only reads?**
The input is a ten-field structured object. A query string has length limits, cannot nest, and is
logged in full by every proxy. The REST-purist answer is GET with query params; the practical
answer for a calculator is POST. What matters is that it is idempotent and stores nothing.

**24. PUT vs PATCH — why is the status update a PATCH?**
PUT means the body is the complete new state of the resource; PATCH means a partial
modification. Marking a move delivered should not require resending every address field.

**25. Which status codes does this API use, and why each?**
201 for a created booking or account (with a `Location` header), 200 for login (it creates
nothing), 204 for a delete with no body, 400 for bad input, 401 for "not authenticated", 403 for
"authenticated but not allowed", 404 for missing, 409 for a conflict with existing state.
Returning 200 with `{"success": false}` is the anti-pattern to avoid.

**26. How does one `@RestControllerAdvice` help?**
Every failure gets the same JSON shape — `{ timestamp, status, error, message, path, fieldErrors }`
— so the frontend writes one error handler. Without it, Spring returns three different shapes
(Whitelabel error, validation dump, security 403) and the React code fills with defensive
checks. It also catches exceptions Spring itself throws before your code runs, which a try/catch
in a controller cannot.

**27. Why map `DataIntegrityViolationException` to 409?**
It is almost always a UNIQUE constraint catching a duplicate that slipped past the application
check under concurrency. 409 turns a race condition into a correct, useful response instead of a
500.

**28. Why does the login failure message not say whether the email exists?**
User enumeration. "No account with that email" lets an attacker build a list of registered
addresses. `BadCredentialsException` is mapped to a single vague 401, and
`hideUserNotFoundExceptions(true)` equalises the response time so timing does not leak it either.

---

## D. Spring Data JPA and Hibernate

**29. How does a Spring Data repository work with no implementation?**
Spring Data creates a proxy at startup and derives the query from the method **name**:
`findByActiveTrueOrderByDisplayOrderAsc` → `WHERE active = true ORDER BY display_order ASC`. The
parser understands `And`, `Or`, `Like`, `Between`, `OrderBy`, `True`/`False`, `IgnoreCase`.

**30. What is the N+1 problem and where could it have happened here?**
One query for a list plus one per row for an association — 21 queries for a 20-row list. The
risk here is `Booking.user`: `@ManyToOne` defaults to **EAGER**, so every booking query would
touch `users`. Both `@ManyToOne` associations are explicitly `FetchType.LAZY`, and when the user
is genuinely needed you fetch it deliberately with a `JOIN FETCH` query or an entity graph.

**31. What is `spring.jpa.open-in-view` and why is it `false` here?**
Spring Boot's default `true` keeps the Hibernate session — and a database connection — open for
the entire HTTP request so lazy loading works in the view layer. That hides
`LazyInitializationException` until load testing and holds connections far longer than
necessary. Setting it to `false` forces you to load what you need inside the service, which is
where the transaction boundary belongs.

**32. Why `ddl-auto=none` instead of `update`?**
Because Flyway owns the schema. `update` drifts silently, cannot be code-reviewed, never drops
or renames anything, and behaves differently across Hibernate versions. With Flyway the schema
is reviewable SQL in git and identical on every environment. `validate` would also work, but
`none` avoids a startup failure over a cosmetic mismatch.

**33. What does `@Transactional` do, and what are the gotchas?**
It wraps the method in a database transaction — commit on normal return, rollback on an
unchecked exception. Three gotchas worth naming: it works through a **proxy**, so a call from
within the same class bypasses it entirely; by default it rolls back only on unchecked
exceptions; and `readOnly = true` lets Hibernate skip dirty checking and flags the transaction
read-only on the connection.

**34. Why is there no `save()` call in `BookingService.updateStatus`?**
Inside a transaction the entity loaded by `findById` is **managed** by the persistence context,
so Hibernate's dirty checking detects the change and flushes an UPDATE on commit. Calling
`save()` would work identically. Knowing *why* it is unnecessary is the point.

**35. Why `@Enumerated(EnumType.STRING)`?**
The default `ORDINAL` stores 0, 1, 2 — so reordering the enum constants silently reassigns every
existing row. If `ROLE_CUSTOMER` and `ROLE_ADMIN` swapped position, every customer would become
an admin. STRING is also readable in the raw table.

**36. When would you use a Specification instead of a derived query?**
When filters are dynamic and optional. The admin list has three independent filters; derived
queries would need eight methods for the combinations. A `Specification` is a lambda returning a
`Predicate`, and returning `null` means "no restriction", so an absent filter disappears from
the SQL rather than becoming `1 = 1`. See `BookingSpecifications`.

**37. Why `coalesce(sum(...), 0)` in the repository?**
`SUM` over zero rows returns NULL in SQL, and unboxing that NULL into a primitive would throw.
`findAverageRating` handles the same problem differently, by returning the wrapper `Double` and
letting the service decide what to display.

**38. What is the cost of your free-text search?**
`LIKE '%term%'` with a leading wildcard cannot use a B-tree index, so it is a full table scan.
Acceptable at tens of thousands of rows; at a million you would add a MySQL `FULLTEXT` index or
move search to Elasticsearch/OpenSearch. I would rather state the limit than pretend it is not
there.

---

## E. Spring Security and JWT

**39. Explain a JWT.**
Three base64url segments: `header.payload.signature`. The header names the algorithm, the
payload holds the claims, and the signature is `HMAC-SHA256(header + "." + payload, secret)`.
Tampering invalidates the signature, and only the server knows the secret — so the server can
trust the token without storing a session.

**40. Is a JWT encrypted?**
No. It is **encoded**. Anyone can paste it into jwt.io and read the claims. Never put a secret
in a JWT. (JWE exists for encryption, but it is rarely what people mean.)

**41. What is the main weakness of JWT auth, and how would you address it?**
There is no server-side session, so a token cannot be revoked before it expires. Mitigations:
short expiry with refresh tokens (with rotation and reuse detection), or a denylist in Redis
keyed on a `jti` claim. This project uses a single 24-hour token and mitigates partially by
re-loading the user from the database on every request, so a **disabled account** loses access
immediately even though the token is still cryptographically valid.

**42. Why does `JwtAuthenticationFilter` hit the database on every request?**
To make the role and the `enabled` flag authoritative. The cost is one indexed SELECT; the
benefit is that deactivating an account takes effect immediately rather than when the token
expires. At scale I would cache it (Spring Cache / Redis) rather than trust the claim.

**43. Why `OncePerRequestFilter`?**
A plain servlet filter can execute several times for one request — on a forward, an include or
an async dispatch — redoing the work and causing confusing double-authentication. This base
class guarantees a single execution.

**44. Why does the filter never reject a request?**
Separation of concerns: its job is "authenticate if possible". No token means the context stays
empty and the chain continues; the authorisation layer then decides whether anonymous access is
acceptable for that URL. That is what lets `POST /bookings` be public while `GET /bookings/me`
requires a login, with zero URL logic inside the filter.

**45. Why is CSRF protection disabled, and when would that be wrong?**
CSRF defends against a browser automatically attaching a **cookie** to a cross-site request. We
authenticate with an `Authorization` header that JavaScript must set explicitly, so there is
nothing for another site to replay — and a stateless API has no CSRF token to store. On a
cookie-session application, disabling it would be a genuine vulnerability. The distinction is
the answer.

**46. What is CORS, and is it a security control?**
A browser mechanism: it stops page A from **reading** responses from API B unless B opts in with
`Access-Control-Allow-Origin`. It is enforced by the browser, so curl and Postman ignore it
entirely — which is why it is not a substitute for authentication. Here the allow-list comes
from an environment variable, and `allowCredentials` is false because the token travels in a
header, not a cookie.

**47. Why not `allowedOrigins("*")`?**
The spec forbids combining the wildcard with credentials, and on an authenticated API it would
let any website on the internet call it as the logged-in user. An explicit list per environment
costs one env var.

**48. Why BCrypt rather than SHA-256?**
SHA-256 is designed to be **fast**, which is exactly wrong for passwords — a GPU does billions
per second. BCrypt is deliberately slow (2^10 rounds at the default strength), salts
automatically with the salt stored inside its 60-character output (so identical passwords produce
different hashes and rainbow tables are useless), and its work factor can be raised as hardware
improves. Argon2id is the modern recommendation and Spring supports it.

**49. Can you recover a user's password?**
No — hashing is one-way. `matches(raw, hash)` re-hashes the input and compares. The correct flow
is a time-limited, single-use reset token sent by email. Any site that emails you your existing
password is storing it recoverably, which is a red flag.

**50. Why does `ROLE_` matter?**
Spring Security convention: `hasRole("ADMIN")` checks for the authority `"ROLE_ADMIN"`, while
`hasAuthority("X")` checks for exactly `"X"`. Naming the enum constants `ROLE_ADMIN` /
`ROLE_CUSTOMER` lets `name()` go straight into a `SimpleGrantedAuthority` and both styles work.
Forgetting the prefix is the most common cause of a correctly-authenticated user still getting
403.

**51. Why both a URL rule and `@PreAuthorize` on `AdminController`?**
Defence in depth. If a refactor moves these endpoints and nobody updates the URL matcher, the
annotation still blocks a non-admin. Security controls should fail closed, and redundancy is how
you get there.

**52. Why does the filter chain end with `.anyRequest().authenticated()`?**
Deny by default. Add an endpoint and forget to classify it and it is protected, not exposed.
Ending with `permitAll()` means every future endpoint is public until someone notices.

---

## F. Security of the application itself

**53. How do you prevent SQL injection?**
Everything goes through JPA/Hibernate or the Criteria API, which use **parameterised**
statements — user input is never concatenated into SQL. The one place with user-controlled
pattern matching, `BookingSpecifications.matchesText`, builds the `%term%` string and passes it
as a **bound parameter**, so a quote character is data, not syntax.

**54. What stops one customer reading another's booking?**
Design, not a check that can be forgotten. **No endpoint takes a user id.** `/bookings/me`
derives identity from the verified token, and `/bookings/track` requires the reference *and* the
phone number on that booking. There is no `GET /bookings/{id}` for customers at all — which also
means an incrementable id cannot be used to enumerate the customer base.

**55. What is mass assignment and how is it prevented?**
Binding a request body straight onto an entity, letting a caller set fields they should not —
`{"role":"ROLE_ADMIN"}` on signup, or `{"approved":true}` on a review. Prevented by request DTOs
that simply **do not contain those fields**, plus hardcoding `Role.ROLE_CUSTOMER` in
`AuthService.register`.

**56. Why is the price recalculated on the server when the client already showed one?**
Because a client-supplied total would let anyone book a ₹50,000 move for ₹1 with a modified HTTP
request. `BookingService.create` calls the same `QuoteService` the quote endpoint uses.
**Never trust the client for money** — and note that the two paths share one implementation, so
the quote and the booking cannot disagree.

**57. Why are testimonials moderated?**
It is a public write endpoint whose content is rendered back to other visitors. Without a
moderation gate the homepage is one POST away from spam or abuse. `approved` defaults to `false`
and the field does not exist on the request DTO.

**58. How is the booking reference generated, and why that way?**
`SecureRandom` (not `java.util.Random`, whose output is predictable from a few samples) over a
32-character alphabet that excludes I, O, 0 and 1 — the characters people mishear on the phone.
32^6 ≈ a billion combinations, re-rolled on collision, with a UNIQUE constraint as the final
authority.

**59. Is that collision check atomic?**
No, and the code says so. Between `existsByReference` and the INSERT another request could take
the same value. "Check then act" is never atomic on its own, which is why the UNIQUE constraint
exists and why `DataIntegrityViolationException` is mapped to 409.

**60. What does the `ProtectedRoute` component actually protect?**
The user experience, not the data. Anyone can edit JavaScript and render the admin page — and
they would see nothing but 403s from the API. Client-side guards are a convenience; the server
is the authority.

---

## G. Database

**61. Why Flyway over Hibernate's schema generation?**
The schema becomes reviewable SQL in version control, applied identically everywhere and
recorded in `flyway_schema_history`. Hibernate's `update` cannot be reviewed, never drops or
renames, and differs across versions.

**62. What is the one rule of migrations?**
A migration that has run is **immutable**. Flyway stores a checksum and refuses to start if the
file changed. To alter the schema you add `V3__…`.

**63. Why is `bookings.user_id` nullable?**
Guests can book without an account — which is the whole conversion argument for the site. The FK
is `ON DELETE SET NULL`, so deleting a user does not delete the company's order history.

**64. Why are the customer's name, email and phone duplicated on the booking?**
A deliberate snapshot. A guest has no account at all; and even for a registered customer the
phone number used for one move must stay attached to that move after they later edit their
profile. Normalising it away would silently rewrite history.

**65. Why store `estimated_amount` instead of recomputing it?**
Because if the price table changes next month, the customer must still see the figure they
agreed to. The same reasoning applies to a stored invoice total anywhere.

**66. How did you choose the indexes?**
From the queries that actually run: `status` and `created_at` for the admin list and its sort,
`customer_email` for lookups, `approved` for the public review feed, and the two UNIQUE
constraints on `email` and `reference`. Indexes are not free — each one slows writes and takes
space — so adding them speculatively is as wrong as omitting them.

**67. Why `utf8mb4` and not `utf8`?**
MySQL's `utf8` is a three-byte subset that cannot store emoji or some CJK characters.
`utf8mb4` is real UTF-8. Customer notes contain emoji more often than you would expect.

**68. Why is the connection pool only five connections?**
Free-tier MySQL instances cap total connections low (often around 25), and HikariCP opens the
pool eagerly. Sizing it to 5 leaves headroom for migrations, your IDE and a second instance
during a rolling deploy. Pool size should match the database's capacity, not optimism.

---

## H. React and the frontend

**69. Why Vite over Create React App?**
CRA is deprecated and unmaintained. Vite serves native ES modules in development, so startup is
near-instant regardless of project size, and bundles with Rollup for production.

**70. Why Context instead of Redux?**
Auth state here is one small object that changes on login and logout. Redux would add a store,
actions and reducers to manage three fields. I would reach for Redux Toolkit (or Zustand) when
there is substantial shared client state with complex update logic — being able to justify *not*
adding a library matters as much as knowing how.

**71. What does `useMemo` on the context value do?**
Keeps the object identity stable. Without it a new object is created on every provider render
and **every** consumer re-renders even when nothing it cares about changed. The standard Context
performance fix.

**72. Why `useCallback` on `logout`?**
Its identity must be stable because it is a `useEffect` dependency; an unstable function there
would re-run the effect on every render — in that case re-registering the unauthorized handler
forever.

**73. How is the live quote debounced, and why does the cleanup matter?**
The effect starts a 600 ms `setTimeout` and returns a cleanup that clears it. Each change runs
the previous cleanup first, so the timer only fires once typing stops. **Without the cleanup,
every keystroke would queue another request** — the debounce *is* the cleanup.

**74. Why is `pricingInputs` a JSON string?**
So it can be a dependency. React compares dependencies by reference, and a new object literal
each render would re-trigger the effect endlessly.

**75. Why the `cancelled` flag in effects?**
A response arriving after the user navigated away would call `setState` on an unmounted
component — a memory leak React warns about. StrictMode's deliberate double-invoke in
development makes it easy to hit.

**76. What does StrictMode do, and why not remove it when a request fires twice?**
It double-invokes component bodies and effects **in development only**, to surface side effects
that are not idempotent. A request firing twice is StrictMode telling you the effect has a bug
worth fixing — the fix is the cleanup flag, not disabling the warning.

**77. Why not use the array index as a `key`?**
React uses keys to match elements between renders. With indices, reordering or filtering makes
React reuse the wrong DOM node — which shows up as inputs keeping the previous row's value.
Every list here keys on the database id.

**78. What is a controlled component?**
One whose value comes from React state with an `onChange` to update it, so state is the single
source of truth. Passing `checked`/`value` without `onChange` produces a read-only input and a
React warning — one of the most common React form bugs.

**79. Why does `Login` use the value returned by `login()` instead of reading `user` from context?**
Because state updates are not synchronous: reading `user` immediately after would give the
**previous** value and the role-based redirect would be wrong. The async function returns the
fresh profile.

**80. What do the axios interceptors buy you?**
The token header attached once instead of at thirty call sites, and three genuinely different
failure modes (`error.response`, `error.request`, neither) normalised into one
`{ status, message, fieldErrors }` shape — which is why the UI's error handling is about twenty
lines.

**81. Why is `/auth/` excluded from the 401 auto-logout?**
A wrong password on the login form is also a 401. Without the exclusion, a failed login would
trigger a logout-and-redirect **to the login page the user is already on**.

**82. Why does `useApiData` keep the fetcher in a ref?**
The fetcher is usually an inline arrow function — a new function every render. In the dependency
array that causes an infinite fetch loop; left out entirely it goes stale. A ref gives the
effect the latest version without making it a dependency.

**83. Why does `ServiceDetail` pass `[slug]` as a dependency?**
Navigating between two service pages keeps the component **mounted** (same route, different
param). Without the dependency the effect would not re-run and the page would keep showing the
previous service.

**84. Why does the frontend fetch dropdown options from the API?**
So React never holds a copy of a Java enum. Copy-pasted enums drift apart, and the drift is
always found in production. `/catalog/options` is generated from `MoveType` and `HomeSize`.

**85. Why does a hard refresh on `/services` need host configuration?**
`BrowserRouter` uses the History API, so `/services` is a real path. On refresh the browser asks
the static host for a file called `services`, which does not exist. The host must rewrite
unknown paths to `index.html` with status 200 — that is what `vercel.json`, `netlify.toml` and
`public/_redirects` do.

**86. Why is only `VITE_`-prefixed config exposed?**
It is a safety boundary: it stops a server secret in your shell environment being compiled into
public JavaScript. And because Vite inlines them at build time, changing one needs a rebuild,
not a restart.

**87. How is accessibility handled?**
Real semantic elements (`<table>`, `<ol>`, `<dl>`, `<figure>`, one `<h1>` per page), `htmlFor` +
`useId` linking every label to its input, `aria-invalid` and `aria-describedby` on errors,
`aria-expanded` on the mobile menu, `role="tablist"`/`aria-selected` on the admin tabs,
`role="alert"` on errors, and `:focus-visible` rings kept rather than removed.

**88. What loading and error states exist?**
Skeletons where the content shape is known (better layout stability than a spinner), spinners
where it is not, `Alert` on failure, and `EmptyState` that explains *why* a list is empty and
offers the next action. Note that `TestimonialsSection` deliberately renders nothing on error
while `ServicesGrid` surfaces it — error handling is a product decision, not one global rule.

---

## I. Testing, DevOps and scale

**89. Why are there no `@SpringBootTest` tests?**
Because the logic worth testing does not need Spring. `PricingEngine` takes its configuration as
a constructor argument and touches nothing else, so `new PricingEngine(props)` is enough and the
tests run in milliseconds. Booting a context and a database to verify that 18% of 4,900 is 882
would take ten seconds and fail when the database is down. I would add `@WebMvcTest` for
controller contracts and Testcontainers for repository queries next — those are the right tools
for *those* questions.

**90. What is `@ParameterizedTest` with `@CsvSource` good for?**
Running one assertion over a table of cases, with each row reported as its own test. Far better
than one test with twelve asserts where the first failure hides the rest. `@EnumSource` goes
further: add a sixth status and the test automatically covers it.

**91. Why does the Dockerfile copy `pom.xml` before `src`?**
Docker caches layers. Resolving dependencies is the slow step, and it only depends on the POM —
so copying `src` first would invalidate that cache on every code change.

**92. What does `-XX:MaxRAMPercentage=75` do?**
Sizes the heap from the **container** memory limit rather than the host's total RAM. Without it,
a JVM in a 512 MB container thinks it has gigabytes and gets OOM-killed. The single most useful
JVM-in-Docker flag.

**93. Why run as a non-root user in the image?**
If the application is compromised, the attacker lands as an unprivileged user inside the
container instead of root — which matters for container escape and for anything mounted in.

**94. Why is Actuator a production dependency?**
Railway and Render poll a health URL to decide whether a deploy succeeded and whether to keep
routing traffic. Without `/actuator/health` a broken deploy can be marked healthy.

**95. What would you change to handle 100× the traffic?**
In order of value: add a read replica and point reporting queries at it; cache the catalogue and
the testimonial feed (they change a few times a year and are read on every page load); replace
the seven dashboard COUNTs with one `GROUP BY` projection or a cached snapshot; move free-text
search to a `FULLTEXT` index; cache the per-request user lookup in the JWT filter; and move
notifications to a queue. I would measure before doing any of it — with Actuator metrics and
slow-query logs, not guesses.

**96. What is the most significant thing you would do differently?**
Add refresh tokens with rotation. A 24-hour access token is a pragmatic choice for a portfolio
project, but the right production design is a short access token plus a rotating refresh token,
which gives you both quick revocation and a session that does not expire mid-task.

**97. What part of this code are you least happy with?**
The free-text search, because `LIKE '%term%'` cannot use an index and I know it will not scale;
and the rate limiting on the contact form, which is keyed on email and therefore trivially
bypassed by changing the email. Both are documented as limitations rather than hidden.

**98. How long did this take, and what was hardest?**
Be honest and specific. A good answer: "The pricing engine took the longest, not to write but to
get the *shape* right — moving the per-type rates onto the enums and keeping the global knobs in
configuration, so the formula stayed readable and the whole thing testable without Spring."

**99. Walk me through what happens when someone books a move.**
See §3 of [01-ARCHITECTURE.md](01-ARCHITECTURE.md) — preflight, JWT filter, authorisation,
validation, service (server-side pricing, reference generation, ownership from the token),
mapping to a DTO that omits internal notes, 201 with a `Location` header. Practise this one out
loud; it is the question that shows you understand your own system end to end.

**100. Why should we hire you based on this project?**
Point at the decisions, not the feature list: money handled in `BigDecimal` with explicit
rounding, a schema owned by reviewable migrations, business rules that live in a state machine
the compiler helps enforce, a security chain that denies by default, a price that is never
trusted from the client, and a set of limitations I can name without being prompted. The
features are the easy part; those choices are the job.

---

Next: [06-RESUME-BULLETS.md](06-RESUME-BULLETS.md)
