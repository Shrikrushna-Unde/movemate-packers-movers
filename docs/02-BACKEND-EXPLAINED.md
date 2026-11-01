# 02 — Backend, file by file

Every file in `backend/`, what it does, and why it exists. The code itself carries line-level
comments; this document is the map and the "why" behind each choice.

Read it with the files open. The order below is the order I would walk an interviewer through.

---

## Build and configuration

### `pom.xml`

Maven's project descriptor: dependencies, build plugins, Java version.

| Part | Why it is there |
|---|---|
| `spring-boot-starter-parent` 3.3.4 | A curated BOM. It is why no Spring dependency below has a `<version>` — the parent already knows which Hibernate, Jackson and MySQL driver versions work together. Upgrading Spring Boot is one number. |
| `starter-web` | Spring MVC + embedded Tomcat + Jackson. This is what makes the project an HTTP server. |
| `starter-data-jpa` | Hibernate + Spring Data repositories + HikariCP connection pool. |
| `starter-validation` | Hibernate Validator, so `@NotBlank` / `@Email` work. |
| `starter-security` | The filter chain, `BCryptPasswordEncoder`, `@PreAuthorize`. |
| `starter-actuator` | `/actuator/health`. Railway and Render poll a health URL to decide whether a deploy succeeded, so this is not optional in production. |
| `flyway-core` + `flyway-mysql` | Versioned SQL migrations. `flyway-mysql` is a **separate artifact** in Flyway 10 and is required for MySQL — omitting it is a common startup failure. |
| `mysql-connector-j` (runtime) | The JDBC driver. `runtime` scope because it is needed to run, not to compile. |
| `jjwt-api` / `-impl` / `-jackson` | JWT creation and parsing. Split in three so the implementation stays off the compile classpath and cannot be imported by accident. |
| `springdoc-openapi-starter-webmvc-ui` | Generates OpenAPI from the controllers and serves Swagger UI. |
| `starter-test` + `spring-security-test` | JUnit 5, Mockito, AssertJ, MockMvc. |
| `spring-boot-maven-plugin` | Repackages the jar into an executable fat JAR, which is what `java -jar` runs in the Docker image. |

### `application.properties`

Local development configuration. Every externally-controlled value uses `${ENV_VAR:fallback}`,
which is the single trick that lets the same JAR run on a laptop and on Railway with no code
change: the platform injects env vars, the laptop falls back.

Three properties worth knowing by heart:

- **`spring.jpa.hibernate.ddl-auto=none`** — Hibernate must never create or alter tables.
  Flyway owns the schema. `update` is convenient in a tutorial and a liability in production:
  it drifts silently and cannot be code-reviewed.
- **`spring.jpa.open-in-view=false`** — Spring Boot's default is `true`, which keeps a
  Hibernate session (and a database connection) open for the entire HTTP request so that lazy
  loading works in the view layer. That hides `LazyInitializationException` until load testing
  and holds connections far longer than necessary. Turning it off is a strong signal in an
  interview.
- **`server.port=${PORT:8080}`** — cloud platforms assign the port.

### `application-prod.properties`

Only the diff for production: SQL logging off, error messages and stack traces hidden, response
compression on, graceful shutdown so in-flight requests finish when the platform sends SIGTERM
during a redeploy. Activated with `SPRING_PROFILES_ACTIVE=prod`.

### `Dockerfile`

A **multi-stage** build.

- Stage 1 (`maven:3.9.9-eclipse-temurin-17`): copies `pom.xml` **alone** first and runs
  `dependency:go-offline`, then copies `src`. Docker caches per layer, so editing Java code
  reuses the dependency download; copying `src` first would invalidate that cache on every
  change.
- Stage 2 (`eclipse-temurin:17-jre-alpine`): a clean JRE with only the finished jar. ~700 MB
  down to ~200 MB, which matters for cold starts on a free tier.
- Runs as a non-root user.
- **`-XX:MaxRAMPercentage=75`** sizes the heap from the *container* limit. Without it the JVM
  reads the host's total RAM, assumes it has gigabytes, and gets OOM-killed in a 512 MB
  container. This is the single most useful JVM-in-Docker flag.

### `db/migration/V1__init_schema.sql`

The schema. Flyway runs each `V<n>__<name>.sql` exactly once per database and records it in
`flyway_schema_history`.

**The rule: a migration that has run is immutable.** To change the schema you add `V3__…`; you
never edit `V1`.

Choices inside it:

- `users`, not `user` — `user` is reserved in PostgreSQL and several other engines.
- `DECIMAL(12,2)` for money, never `DOUBLE`. Binary floating point cannot represent 0.1 exactly,
  so sums drift. Standard interview question.
- `UNIQUE (email)` and `UNIQUE (reference)` — the only reliable duplicate prevention. An
  application-level "select then insert" check loses the race under concurrency; the database
  never does.
- `ON DELETE SET NULL` on `bookings.user_id` — deleting a user must not delete order history.
- `CHECK (rating BETWEEN 1 AND 5)` — defence in depth behind the DTO validation.
- Indexes chosen from the actual queries: `status`, `created_at`, `customer_email`, `approved`.

### `db/migration/V2__seed_catalog.sql`

Reference data the site cannot render without: six services and six approved reviews. Seeding it
as a migration means a fresh database — a reviewer's laptop, a new Railway instance — comes up
fully populated.

The **admin user is deliberately not here**, because its password must be BCrypt-hashed and
hashing belongs in Java. Hardcoding a hash in SQL commits a password to git forever.

---

## Entry point

### `MoveMateApplication.java`

`@SpringBootApplication` is three annotations in one: `@Configuration`,
`@EnableAutoConfiguration` (inspect the classpath and configure what is found) and
`@ComponentScan` (scan this package downwards). That last one is exactly why every class lives
under `com.movemate`.

Plus `@EnableJpaAuditing` (fills `createdAt` / `updatedAt`; without it those fields stay null
and the NOT NULL insert fails) and `@ConfigurationPropertiesScan` (finds the
`@ConfigurationProperties` records).

---

## `domain/` — entities and the state machine

### `Auditable.java`

`@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)` gives every entity
`created_at` and `updated_at` **on its own table** — the inheritance exists only in Java, there
is no `auditable` table and no join. Compare with `@Entity` + `@Inheritance`, which really does
create extra tables.

Uses `Instant`, not `LocalDateTime`: an `Instant` is an unambiguous point on the global
timeline, while `LocalDateTime` has no zone and so "2026-10-04 14:30" means different real
moments in Pune and London. Audit data must never be ambiguous.

### `Role.java`

The `ROLE_` prefix is Spring Security's convention, not decoration: `hasRole("ADMIN")` checks
for the authority `"ROLE_ADMIN"`. Naming the constants `ROLE_ADMIN` / `ROLE_CUSTOMER` lets us
hand `name()` straight to a `SimpleGrantedAuthority`. Forgetting this prefix is the most common
reason a correctly-authenticated user still gets 403.

### `BookingStatus.java`

The state machine. `ALLOWED_TRANSITIONS` is an `EnumMap<BookingStatus, EnumSet<BookingStatus>>`
built in a static block — safe, because an enum's static fields are initialised after its
constants exist.

Why `EnumMap`/`EnumSet` rather than `HashMap`/`HashSet`? They are backed by an array indexed by
ordinal and a bitmask respectively: no hashing, no boxing, and they cannot hold a key outside
the enum.

`allowedNextStates()` returns `Set.copyOf(...)` — an immutable copy, so a caller cannot mutate
the application's workflow rules.

### `MoveType.java` / `HomeSize.java`

Each constant carries its own pricing facts (`baseCharge`, `perKmRate`, `crewSize`). This is
"tell, don't ask": adding a move type is one line and the compiler forces you to supply its
price, whereas a `switch` scattered across services would silently fall through to a default.

`BigDecimal.valueOf(1500)`, not `new BigDecimal(1500.0)` — the double constructor carries binary
rounding error into the value.

### `User.java`

- `@Table(name = "users")` for the reserved-word reason above.
- `GenerationType.IDENTITY` maps to MySQL `AUTO_INCREMENT`. Worth knowing the tradeoff:
  Hibernate cannot batch inserts with IDENTITY because it must read each generated key back.
  Irrelevant for a booking site; for bulk imports you would choose `SEQUENCE`.
- **It does not implement `UserDetails`.** Mixing a JPA entity with a Spring Security interface
  drags `isAccountNonLocked()` and friends into every query, test and JSON response.
  `AuthenticatedUser` adapts between the two instead.
- `@Enumerated(EnumType.STRING)` — the default (`ORDINAL`) stores 0/1 and breaks the day
  someone reorders the enum, silently promoting every customer to admin.
- Protected no-arg constructor: JPA needs one, but application code should not build a
  half-initialised `User`.

### `Booking.java`

The core table. Four points worth rehearsing:

1. **`FetchType.LAZY` on `@ManyToOne`.** The JPA default is EAGER, which means every booking
   query also touches `users` even when nobody reads the user — 21 queries for a 20-row admin
   list (the N+1 problem). Always mark `@ManyToOne` lazy and fetch deliberately with
   `JOIN FETCH` when you need it.
2. **Customer details are copied onto the booking**, not read through the link. A guest has no
   account; and even for a registered customer, the phone used for one move must stay attached
   to that move after they edit their profile. A deliberate snapshot.
3. **`reference` is the public id.** `/bookings/1` lets anyone enumerate your customer base.
4. **`changeStatus` lives on the entity**, not in the service. The object protects its own
   invariant, so no future caller can put it in an illegal state. Same status twice is a no-op,
   which makes the operation idempotent.

### `MovingService.java`, `Testimonial.java`, `ContactMessage.java`

Catalogue data in a table rather than hardcoded React cards, because it is business content and
the admin changes it without a frontend deploy. `active` is a soft delete: hiding a service must
not orphan existing bookings or break bookmarked URLs.

`Testimonial.approved` defaults to `false` — any public write endpoint that renders content back
to other visitors needs a moderation gate, or the homepage is one POST away from spam.

`ContactMessage` is persisted *before* any notification would be sent, because an email can
bounce or land in spam and then the lead is gone.

---

## `repository/` — data access

### `UserRepository`, `MovingServiceRepository`, `TestimonialRepository`, `ContactMessageRepository`

There is **no implementation class anywhere**, and that is the point. Spring Data builds a proxy
at startup and derives SQL from the method *name*:
`findByActiveTrueOrderByDisplayOrderAsc` → `WHERE active = true ORDER BY display_order ASC`.

Two habits visible here:

- `Optional<User> findByEmail(...)` — absence is part of the signature. Returning null is how
  NullPointerExceptions get into login code.
- `boolean existsByEmail(...)` instead of `findByEmail(...).isPresent()` — issues
  `SELECT COUNT(*)` and never materialises the entity.

And two deliberate `@Query` methods, both aggregating in the database rather than in Java:
`sumEstimatedAmountExcludingStatus` (note `coalesce(..., 0)`: `SUM` over zero rows returns NULL
in SQL, and unboxing that would throw) and `findAverageRating` (returns the wrapper `Double`
because `AVG` over an empty table is NULL).

### `BookingRepository` + `BookingSpecifications`

The admin list filters by status, move type and free text, and any of them can be absent. Three
possible approaches:

1. Four query methods for four combinations — doubles with every new filter.
2. One JPQL query with `(:status is null or b.status = :status)` — works, but forces the
   database to evaluate dead branches and gets ugly fast.
3. **Criteria API `Specification`s composed at runtime** — what this project does.

A `Specification` is a lambda receiving `(root, query, criteriaBuilder)` and returning a
`Predicate`. Returning `null` is meaningful: Spring Data treats a null predicate as "no
restriction", so an absent filter disappears from the SQL rather than becoming `1 = 1`.

`matchesText` uses `lower(col) LIKE lower('%term%')`. Be honest about the cost: a leading
wildcard cannot use a B-tree index, so this is a full scan. Fine at tens of thousands of rows;
beyond that you reach for MySQL `FULLTEXT` or a search engine. **Knowing the limit matters more
than pretending there isn't one.**

---

## `dto/` — the API contract

### Why DTOs at all, instead of exposing entities?

Three reasons, in order of importance:

1. **Security.** If `/auth/register` bound to `User`, a caller could POST
   `{"role":"ROLE_ADMIN"}` and promote themselves. That is *mass assignment*, a real
   vulnerability. And `UserResponse` has no `passwordHash` field, so a hash that is never
   serialised can never leak through a log line or a cached response. Relying on `@JsonIgnore`
   on the entity works until someone removes it in an unrelated refactor.
2. **Contract stability.** Renaming a database column should not break every client.
3. **Shape.** `RegisterRequest` carries a plaintext password; `User` stores a hash. They are
   genuinely different types.

### Why `record` and not `class`?

A DTO is an immutable carrier of data with no identity and no behaviour. The compiler generates
the constructor, accessors, `equals`, `hashCode` and `toString` — about 60 lines saved per DTO —
and immutability is exactly right for something crossing a boundary.

### Validation highlights

| DTO | Rule | Why |
|---|---|---|
| `RegisterRequest` | `^[6-9]\d{9}$` | Indian mobiles are 10 digits starting 6-9. A typo caught here is a crew who can actually reach the customer on move day. |
| `RegisterRequest` | `^(?=.*[A-Za-z])(?=.*\d).{8,}$` | Two lookaheads express independent rules in one pattern without consuming characters. |
| `LoginRequest` | presence only | Complexity rules may have changed since the account was created; rejecting an old-but-correct password with a 400 would lock users out. |
| `QuoteRequest` | boxed `Integer distanceKm` + `@NotNull` | With a primitive, a missing field silently becomes 0 and the customer gets a local price for an intercity move. |
| `QuoteRequest` | `homeSize` has **no** `@NotNull` | It is required for household moves and meaningless for a car transport. Bean Validation cannot express "required depending on another field", so that one rule lives in `QuoteService`. |
| `BookingRequest` | `@FutureOrPresent`, not `@Future` | A same-day emergency move is a real request. |
| `ContactRequest` | `phone` optional but pattern-checked | `@Pattern` skips `null` but **not** an empty string — which is why the frontend sends `null` for untouched optional fields. |
| `TestimonialRequest` | no `approved` field | A field the client cannot send is a field the client cannot set. The safest possible authorisation. |

### `BookingRequest.toQuoteRequest()`

Projects the pricing-relevant subset onto a `QuoteRequest`, so the booking price and the
displayed estimate come from the same code path. Duplicating the formula for bookings is how a
quote page and an invoice start disagreeing in production.

### `BookingResponse`

Two computed fields:

- `allowedNextStatuses` — the admin UI renders its buttons from this, so the workflow lives
  only in the enum and the frontend cannot offer an illegal action. Sorted, so button order is
  stable between requests.
- `timeline` — derived with `step.ordinal() <= current.ordinal()`, which is a legitimate use of
  `ordinal()`: in memory, for ordering. Persisting it is the thing to avoid.

`adminNotes` is absent on purpose. "Customer haggled, gave 5% off" must never reach a
customer-facing endpoint, and leaving the field out of the type is the surest guarantee.

### `PageResponse<T>`

Why not return Spring's `Page<T>` directly? Its JSON shape is an implementation detail of
Spring Data — it serialises `Pageable` and `Sort` objects, Spring Boot 3.3 logs a warning about
exactly this, and the structure has changed between versions, which would break the React app
on a dependency upgrade. A small DTO we own means the API contract is ours.

`static <E, T> PageResponse<T> from(Page<E>, Function<E, T>)` serves bookings, enquiries and
reviews from one generic method: `PageResponse.from(page, BookingResponse::from)`.

### `OptionResponse`

Generates dropdown options from `MoveType` and `HomeSize` so React never hardcodes a copy of a
Java enum. Add a move type in Java and the form updates on the next page load.

---

## `exception/` — error handling

### `ApiError`

One response shape for every failure: `{ timestamp, status, error, message, path, fieldErrors }`.
A client that can rely on that writes one error handler. Without it, Spring returns three
different shapes — its own Whitelabel error, a validation dump and a security 403 — and the
React code fills up with defensive checks.

`@JsonInclude(NON_NULL)` drops nulls, so a plain 404 does not carry an empty `fieldErrors`.

### The exception classes

All extend `RuntimeException`, not `Exception`. A checked exception would force `throws`
declarations up through every service and controller, and there is nothing a caller can do to
recover from "this booking is not here" except return 404. Spring's own
`DataAccessException` hierarchy is unchecked for the same reason.

They carry **no HTTP concepts**. The mapping to a status code lives in the handler, so the
service layer stays usable from a scheduled job or a CLI where HTTP is meaningless.
(`@ResponseStatus` on the exception would work too, but it welds the exception to the web layer.)

| Exception | Status | When |
|---|---|---|
| `ResourceNotFoundException` | 404 | The record does not exist |
| `BadRequestException` | 400 | Cross-field business rule ("insurance needs a declared value") |
| `ConflictException` | 409 | Well-formed but collides with existing state (email taken) |

400 vs 409 matters to a client: 400 means "fix your input format", 409 means "your input is
fine, the world has changed" — which is why the signup form can show "this email is already
registered" for 409 and a generic field error for 400.

### `GlobalExceptionHandler`

`@RestControllerAdvice` registers these handlers across every controller. The alternative —
try/catch in each method — duplicates the block dozens of times and still misses the exceptions
Spring itself throws before your code runs.

Handlers worth noting:

- **`MethodArgumentNotValidException`** (body validation) is flattened into
  `{"customerPhone": "…"}` so React can attach each message to the field that caused it.
- **`ConstraintViolationException`** (parameter validation, from `@Validated` on the class) is a
  *different* exception raised by an AOP proxy. Handling only the first one is a very common
  gap, and the result is a 500 where a 400 belongs.
- **`BadCredentialsException`** returns a deliberately vague "Invalid email or password".
  Replying "no account with that email" lets an attacker enumerate registered addresses.
- **`DataIntegrityViolationException` → 409.** This is almost always our UNIQUE index catching
  a duplicate that slipped past the application check under concurrency. Mapping it to 409
  instead of 500 turns a race condition into a correct, useful response.
- **The catch-all logs the stack trace server-side and returns a generic sentence.** Stack
  traces expose class names, library versions and sometimes SQL — free reconnaissance.

---

## `security/`

### `JwtProperties`

Type-safe binding of `app.jwt.*`. Better than scattering `@Value("${app.jwt.secret}")`: the
settings are one injectable object, binding happens once at startup so a typo fails fast, and
types are converted for you. Spring Boot 3 binds records through their canonical constructor
with no extra annotation.

### `JwtService`

**What a JWT is:** three base64url segments, `header.payload.signature`, where the signature is
`HMAC-SHA256(header + "." + payload, secret)`. Any tampering invalidates the signature, and only
the server knows the secret — so the server can trust the token without storing a session.

**What it is not:** encrypted. The payload is *encoded*; anyone can paste a token into jwt.io
and read the claims. Never put a secret in a JWT.

**Its real limitation:** with no server-side session there is no way to revoke a token before it
expires. The standard answers are short expiry plus refresh tokens, or a denylist in Redis.
Saying this honestly is a plus; claiming JWTs are revocable is a red flag.

Implementation details:
- The `SecretKey` is derived once in the constructor and held `final` — key derivation per
  request is pure overhead, and an immutable key makes the singleton bean thread-safe.
- `Keys.hmacShaKeyFor` throws `WeakKeyException` under 256 bits. Let it: a short secret is a
  vulnerability, not an inconvenience.
- `verifyWith(key)` is the line that matters. Without it you would be trusting whatever the
  client claims — the classic `alg: none` attack.
- `requireIssuer` means a token minted by another system with the same key is still rejected.
- A rejected token logs at **DEBUG**, not ERROR: expiry is routine, and logging it at ERROR
  buries real problems.

### `AuthenticatedUser`

The Spring Security "principal", adapting `User` to `UserDetails`. Keeping them separate means
the entity is about persistence and this class is about authentication. Controllers receive it
with `@AuthenticationPrincipal`, which is how `/bookings/me` knows whose bookings to return
without trusting a client-supplied id.

The three `isAccountNonExpired` / `NonLocked` / `CredentialsNonExpired` flags are hardcoded
`true` and left explicit, so a reviewer sees the decision was made rather than forgotten.

### `AppUserDetailsService`

One method, `loadUserByUsername`. Spring Security's `DaoAuthenticationProvider` calls it during
login to fetch the stored hash, then compares with the configured `PasswordEncoder`. Our JWT
filter calls it on each authenticated request too.

**Constructor injection, not field injection** — and this is the single most common "what would
you improve" answer, so get it right everywhere:
- the field can be `final`, so the dependency cannot be swapped at runtime;
- the object is impossible to construct in an invalid state;
- tests can pass a mock with `new AppUserDetailsService(mockRepo)` — no reflection, no Spring.

### `JwtAuthenticationFilter`

Turns `Authorization: Bearer …` into an authenticated `SecurityContext`.

**Why `OncePerRequestFilter`?** A plain filter can run several times per request — on a forward,
an include or an async dispatch — redoing this work. This base class guarantees one execution.

**Why it never rejects anything.** Its only job is "authenticate if possible". No token means
the context stays empty and the chain continues; a later authorisation filter decides whether
anonymous access is acceptable for that URL. That separation is what lets `POST /bookings` be
public while `GET /bookings/me` requires a login, with no URL if-statements in this class.

**The deliberate database read per request.** One indexed SELECT, in exchange for the role and
`enabled` flag being authoritative — so deactivating an account takes effect immediately rather
than when the token expires. Trusting the role claim alone would be faster and wrong. At scale
you would cache this, not drop it.

### `JwtAuthenticationEntryPoint`

Produces a JSON 401 instead of Spring's default `WWW-Authenticate: Basic`, which makes browsers
pop a native login dialog over a React app.

This also explains something genuinely useful: **security filters run before the
DispatcherServlet, so no `@ControllerAdvice` ever sees these failures.** That is why a 401 from
a filter looks different from a 400 from a controller unless you wire this up.

---

## `config/`

### `SecurityConfig`

The file an interviewer is most likely to open. Every line has a reason:

- **`PasswordEncoder` → BCrypt.** Slow by design (2^10 rounds at the default strength), so
  brute-forcing a stolen database is expensive; salts automatically, with the salt stored inside
  the 60-character output, so identical passwords get different hashes and rainbow tables are
  useless; and the work factor can be raised as hardware improves. Hashing is one-way, which is
  the correct answer to "how do I email a user their forgotten password" — you cannot, you send
  a reset link.
- **`hideUserNotFoundExceptions(true)`** — otherwise Spring short-circuits the password check
  when the user is absent, and the response-time difference reveals which emails exist.
- **`csrf.disable()`** is correct *here*: CSRF defends against a browser silently replaying a
  cookie, and we authenticate with a header that JavaScript must attach explicitly. On a
  cookie-session app, disabling it would be a vulnerability. Know the difference.
- **`.cors(...)` inside the security chain** — not only as a WebMvc setting, or the security
  filters reject the browser's preflight before Spring MVC sees it.
- **`SessionCreationPolicy.STATELESS`** — no `HttpSession`, no `JSESSIONID`. This is what makes
  the API horizontally scalable: any instance can serve any request with no shared session store.
- **Matcher order is significant** — the first match wins, so specific rules precede general
  ones.
- **`.anyRequest().authenticated()` as the tail.** Deny by default: add an endpoint and forget
  to classify it and it is protected, not exposed. Never end this chain with `permitAll()`.
- **`addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`** — the token must
  be processed before authorisation is evaluated.
- **CORS:** `setAllowedOrigins(list)`, never `addAllowedOrigin("*")` on an API that accepts
  credentials — the spec forbids combining the wildcard with credentials, and it would let any
  site call the API as a logged-in user. `allowCredentials=false` because the token is in a
  header, not a cookie. `maxAge=3600` stops a preflight before every call.

CORS is enforced by the **browser**, not by us. It is not a replacement for authentication
(curl ignores it entirely), but misconfiguring it is the number one reason a React app that
works in Postman fails in Chrome.

### `AdminProperties` + `DataSeeder`

Every admin panel has a chicken-and-egg problem: you need an admin to create an admin. The bad
answer is a hardcoded BCrypt hash in a SQL migration, which commits a password to git forever.
Instead the credentials come from configuration and are hashed at startup, so production sets
`ADMIN_PASSWORD` as an env var.

`DataSeeder` implements `CommandLineRunner`, which runs after the context is ready and after
Flyway has migrated, but before traffic is served. It is **idempotent** — it checks before it
inserts — because containers restart, redeploy and scale, so this code runs many times against
the same database. An unconditional INSERT would crash on the UNIQUE constraint on the second
boot and the app would never come up.

It logs the admin **email** and never the password: platform logs are visible to anyone with
dashboard access and are retained long after you forget the line exists.

### `PricingProperties` + `CorsProperties`

Relaxed binding maps `app.pricing.free-distance-km` to `freeDistanceKm` — kebab-case in config,
camelCase in Java, no configuration needed. The split from the enums is deliberate: per-move-type
rates are domain facts, global multipliers (GST, packing percentage) are things the business
changes without a code change.

### `OpenApiConfig`

springdoc already generates the endpoint list from the controllers. This adds what it cannot
infer: the title and description, and the **Authorize** button that lets a reviewer paste a JWT
and actually call the protected endpoints.

This is documentation *generated from code*, so it cannot drift out of date the way a
hand-written API doc does. Put the Swagger URL on your resume next to the live site — a reviewer
can verify the API works without installing anything.

---

## `service/` — business logic

### `PricingEngine`

A `@Component` with no repository, no HTTP and no database: pure input to output. That makes it
trivially unit-testable and is the most important structural idea in the backend — **business
rules do not belong in controllers**.

Arithmetic details that matter:

- `percentOf` calls `divide(HUNDRED, 2, RoundingMode.HALF_UP)`. **`BigDecimal.divide` must be
  given a scale and a rounding mode** — without them a non-terminating result such as 1/3 throws
  `ArithmeticException` at runtime, which only shows up with certain inputs. `HALF_UP` is what
  accountants expect.
- `Math.max(0, distanceKm - freeDistanceKm)` — without the clamp, a short move produces a
  negative charge.
- `premium.max(minimum)` — `BigDecimal.max` applies a floor without an if-statement.
- Summing with `.reduce(BigDecimal.ZERO, BigDecimal::add)` — there is no
  `Collectors.summingBigDecimal`, and `mapToDouble(...).sum()` would reintroduce floating-point
  error into money.
- A charge that does not apply produces **no line item**, rather than a zero one. "Transport:
  Rs 0" on a quote looks like a bug to a customer.
- `List.copyOf(items)` — the response is immutable.

### `QuoteService`

Validates, then delegates. Two classes for one feature because `QuoteService` enforces rules
that depend on *combinations* of fields, while `PricingEngine` stays pure. If they were one
class, every pricing test would also have to satisfy the validation rules and the formula would
be buried in guard clauses.

The rules exist because each describes a submission that is individually valid and collectively
meaningless: a household move with no home size would price a 4 BHK like a 1 RK; insurance with
no declared value would produce a zero premium and a customer who believes they are insured.

Note `declaredValue.compareTo(BigDecimal.ZERO) <= 0`, **not** `.equals`. `BigDecimal.equals`
compares *scale* as well as value, so `new BigDecimal("0.00").equals(BigDecimal.ZERO)` is
`false`. One of the most common BigDecimal bugs and a frequent interview question.

### `BookingReferenceGenerator`

Four requirements, each met deliberately:

- **Not guessable** — `SecureRandom`, not `java.util.Random` (whose output is predictable from
  a few samples).
- **Readable over the phone** — the alphabet excludes I, O, 0 and 1, the four characters humans
  confuse most. 32^6 ≈ one billion combinations.
- **Sortable by eye** — the year prefix tells support how old a booking is.
- **Actually unique** — re-roll on collision, with the UNIQUE constraint as the final authority.

The comment in the code states the honest limitation: between the `existsByReference` check and
the INSERT, another request could take the same value. "Check then act" is never atomic on its
own — which is exactly why the database constraint exists and why
`DataIntegrityViolationException` is mapped to 409.

### `BookingService`

The reason the controllers are thin. Points to rehearse:

- **The price is recalculated server-side.** `quoteService.estimate(request.toQuoteRequest())`.
  Any client-supplied total would let someone book a ₹50,000 move for ₹1 with a modified
  request. *Never trust the client for money.*
- **The owner is loaded by the id from the verified token**, never from the request body. That
  is what stops one customer attaching a booking to another's account.
- **`track` requires reference AND phone**, and returns 404 (not 403) on a mismatch so it does
  not confirm that the reference exists.
- **`search` composes Specifications** and is always paged. `findAll()` on a table that grows
  daily is the most common way a working application falls over in year two.
- **`updateStatus` does not check the transition itself** — it calls `booking.changeStatus` and
  lets the entity enforce the rule, so no other caller can bypass it.
- **There is no `save()` call in `updateStatus`.** Inside a transaction the entity is *managed*,
  so Hibernate's dirty checking flushes the UPDATE on commit. Calling `save()` would work
  identically; knowing why it is unnecessary is the part interviewers probe.
- `emptyToNull` — HTML forms submit `""` for untouched inputs, and storing an empty string means
  `WHERE pincode IS NULL` misses those rows. Pick one representation for "no value" and enforce
  it at the boundary.

### `CatalogService`, `TestimonialService`, `ContactService`

- `CatalogService.update` checks for a slug owned by a *different* row before saving, so the
  admin gets a message naming the clash instead of a generic 409 from the constraint.
- `toggleActive` is a soft delete, for the bookmarked-URL and price-history reasons above.
- `TestimonialService.submit` takes the author name **from the authenticated account**, not the
  request, so nobody can post a review signed "Tata Motors".
- `setApproved(id, boolean)` takes the desired value rather than toggling, which makes it
  idempotent — a retrying client cannot flip the flag twice. Same principle as PUT in REST.
- `ContactService` rate-limits per email over a one-hour window with one indexed COUNT, and the
  comment states the limit honestly: an attacker can change the email, so this stops accidental
  double-submits and lazy spam, not a determined flood.
- `ContactService` persists first and would notify second, so a failed email never loses a lead.

### `AdminStatsService`

Every figure is a COUNT or SUM executed in MySQL. Loading every booking to display one integer
works fine with 30 rows and times out with 30,000.

`LinkedHashMap` for `bookingsByStatus` preserves insertion order, so the JSON comes back in
lifecycle order and the dashboard tiles do not shuffle between page loads.

The class comment also states the honest tradeoff: this issues seven small queries, and at real
scale you would replace them with one `GROUP BY status` projection or cache the response.
Knowing the queries are there is the point.

---

## `web/` — controllers

### Shared conventions

- `@RestController` = `@Controller` + `@ResponseBody`: return values are serialised to JSON.
- `/api/v1` — versioning is the cheapest insurance you can buy. A breaking change adds `/v2`
  and leaves `/v1` serving the mobile app that has not been updated.
- **`@Valid` is what actually triggers validation.** Without it, every `@NotBlank` in the DTO is
  inert — a mistake that is invisible until someone registers with an empty name.
- Status codes are chosen, not defaulted: 201 for a created resource, 200 for login (it creates
  nothing), 204 for a delete with no body.

### `AuthController`

`/auth/me` takes **no user id parameter**, and that is the whole design. `GET /users/{id}` would
need an ownership check, and forgetting that check is how one customer reads another's data.
Deriving identity from the verified token makes the bug impossible.

Login is POST even though it only reads, because a GET would put the password in the URL —
browser history, proxy logs, access logs.

### `CatalogController`

`Cache-Control: max-age=300, public` on the service list. The catalogue changes a few times a
year and is read on every page load, so this is the highest-value caching header in the API, and
`cachePublic()` is safe precisely because the response has no per-user data.

`/catalog/options` is the endpoint that keeps the React dropdowns and the Java enums in sync.

### `QuoteController`

POST for a read-only operation, and worth being able to defend: the input is a ten-field
structured object, which does not belong in a query string (length limits, no nesting, logged in
full by every proxy). The REST-purist answer is GET with query params; the practical answer for
a calculator is POST. What matters is that it is idempotent and stores nothing.

### `BookingController`

- `@AuthenticationPrincipal` is **nullable** here, which is what lets one endpoint serve guests
  and logged-in customers.
- `ResponseEntity.created(location)` builds the URL from the current request with
  `ServletUriComponentsBuilder`, so it is correct behind Railway's proxy with no hardcoded host.
- `@Validated` on the class (Spring) is **not** the same as `@Valid` on a parameter (Jakarta):
  `@Valid` cascades into a request body, `@Validated` enables validation of individual method
  parameters such as the `@NotBlank @RequestParam`s on `/track`.
- `/bookings/me` returns a bare list, not a page — a customer has a handful of moves in a
  lifetime. The admin list, which grows without bound, **is** paged. Choosing per endpoint
  rather than applying one rule everywhere is the point.

### `AdminController`

Two independent protections: the URL rule in `SecurityConfig` **and** class-level
`@PreAuthorize("hasRole('ADMIN')")`. Belt and braces on purpose — if a future refactor moves
these endpoints and nobody updates the URL rule, the annotation still blocks a non-admin.
Security controls should fail closed.

`@PageableDefault(size = 20, sort = "createdAt", direction = DESC)` — Spring Data's argument
resolver parses `?page=&size=&sort=` for us; the defaults mean a caller who omits them gets the
newest 20 rather than the whole table in insertion order.

PATCH (not PUT) for a status change, because we are modifying one aspect rather than replacing
the resource — PUT implies the body is the complete new state, which would mean sending every
address field to mark a move delivered.

`handled` and `approved` are explicit parameters rather than toggles, so a retry is idempotent.

---

## `src/test/` — tests

### `PricingEngineTest`

No `@SpringBootTest`, no database, no mocks — `new PricingEngine(properties)` is enough, because
the engine takes its configuration as a constructor argument and touches nothing else. The
tests run in milliseconds, which is a direct payoff of constructor injection and keeping logic
out of controllers.

A `@SpringBootTest` that boots the context, connects to MySQL and runs migrations just to check
that 18% of 4,900 is 882 would take ten seconds and break when the database is down. **Knowing
which kind of test to reach for is a senior-level signal.**

`@BeforeEach` gives each test a fresh engine — shared mutable state between tests is the classic
cause of "passes alone, fails in the suite".

The `subtotalMatchesLineItems` test guards an invariant rather than a value: if the breakdown
ever stops adding up to the total, the customer sees a quote that contradicts itself, which
destroys trust faster than a wrong price would.

### `BookingStatusTest`

`@ParameterizedTest` + `@CsvSource` runs one assertion over a table of twelve transitions, and
each row is reported as its own test — far better than one test with twelve asserts, where the
first failure hides the rest.

`@EnumSource` feeds every constant in, so adding a sixth status automatically extends the test.
That is the kind of test that keeps paying off after you write it.

The immutability test asserts on **behaviour** (`add` throws `UnsupportedOperationException`)
rather than on the implementation class name, so it stays valid across JDK versions.

---

Next: [03-FRONTEND-EXPLAINED.md](03-FRONTEND-EXPLAINED.md)
