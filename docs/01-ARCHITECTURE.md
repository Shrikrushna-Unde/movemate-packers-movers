# 01 — Architecture

How the pieces fit together, and why each one is shaped the way it is. Read this first; the
file-by-file documents assume it.

---

## 1. The big picture

```
┌──────────────────────────┐         ┌────────────────────────────────────┐        ┌──────────┐
│  Browser                 │         │  Spring Boot API                   │        │ MySQL 8  │
│  React 18 SPA (static)   │  HTTPS  │  embedded Tomcat                   │  JDBC  │          │
│  Vercel / Netlify        │ ──────► │  Railway / Render (Docker)         │ ─────► │ Railway  │
│                          │  JSON   │                                    │        │          │
│  Authorization: Bearer   │         │  Flyway migrates on startup        │        │          │
└──────────────────────────┘         └────────────────────────────────────┘        └──────────┘
```

Three separately deployable pieces. The frontend is a folder of static files with no server
process; the backend is one stateless JAR in a container; the database is the only stateful
component.

**Why split them at all?** Because the frontend needs a CDN and the backend needs a JVM and a
database connection, and those have nothing to do with each other. Splitting them also means
the same API can serve a future mobile app without a rewrite. The cost is that you have to
think about CORS — which is exactly why `SecurityConfig` has an explicit origin allow-list.

---

## 2. Backend layers

```
   HTTP request
        │
        ▼
┌───────────────────┐  JwtAuthenticationFilter → SecurityContext
│  Security filters │  authorizeHttpRequests → allow / 401 / 403
└───────────────────┘
        │
        ▼
┌───────────────────┐  @RestController
│   web/            │  Validates input (@Valid), picks the status code. No business logic.
└───────────────────┘
        │  DTO in, DTO out
        ▼
┌───────────────────┐  @Service, @Transactional
│   service/        │  All business rules. Pricing, reference generation, workflow, ownership.
└───────────────────┘
        │  entities
        ▼
┌───────────────────┐  Spring Data JPA interfaces + Specifications
│   repository/     │  No SQL in our code except the deliberate @Query aggregates.
└───────────────────┘
        │
        ▼
┌───────────────────┐  @Entity, domain enums
│   domain/         │  Protects its own invariants (Booking.changeStatus).
└───────────────────┘
        │
        ▼
      MySQL  ◄── schema owned by Flyway, not Hibernate
```

### What belongs in each layer

| Layer | Does | Never does |
|---|---|---|
| `web/` | Read the request, call one service method, choose a status code | Business rules, database access, mapping decisions |
| `service/` | Business rules, transactions, authorisation of *data* ("is this your booking?") | HTTP concepts, request parsing |
| `repository/` | Query the database | Business rules |
| `domain/` | Hold state, enforce its own invariants | Know about HTTP or DTOs |
| `dto/` | Carry data across the boundary, validate shape | Contain behaviour |

The single most common mistake in a project like this is a controller that is 80 lines long
because it contains the logic. Here, the longest controller method is six lines. That is the
point: logic in a service can be unit-tested in a millisecond, reused by a scheduled job, and
reasoned about without thinking about HTTP.

---

## 3. Walk through one request

`POST /api/v1/bookings` — the most interesting path in the application.

1. **CORS preflight.** The browser sends `OPTIONS` first because this is a cross-origin POST
   with a `Content-Type: application/json` header. `SecurityConfig` permits all `OPTIONS` and
   the `CorsConfigurationSource` answers with the allowed origin, methods and headers.

2. **`JwtAuthenticationFilter`.** If the request carried `Authorization: Bearer …`, the filter
   verifies the signature and expiry, loads the user from MySQL, and puts an `Authentication`
   into the `SecurityContextHolder`. If there is no token it does nothing — this endpoint is
   public, so an anonymous request is legitimate.

3. **Authorisation.** `requestMatchers(HttpMethod.POST, "/api/v1/bookings").permitAll()` matches,
   so the request proceeds whether or not it is authenticated.

4. **`BookingController.create`.** `@Valid @RequestBody BookingRequest` triggers Bean
   Validation. A bad phone number stops here with a 400 listing the field — the controller body
   never runs. `@AuthenticationPrincipal` injects the principal, or `null` for a guest.

5. **`BookingService.create`** (inside a transaction):
   - calls `quoteService.estimate(request.toQuoteRequest())` — **the price is recalculated
     server-side**; nothing about money comes from the client
   - `QuoteService` applies the cross-field rules Bean Validation cannot express ("a household
     move needs a home size") and delegates the arithmetic to `PricingEngine`
   - `BookingReferenceGenerator` produces an unguessable reference and checks it is unused
   - if a user id was supplied, the owner is loaded **by that id from the token**, never from
     the request body
   - the entity is saved; `@CreatedDate` fills `created_at`

6. **Mapping.** `BookingResponse.from(booking)` builds the response, computing
   `allowedNextStatuses` from the state machine and the customer-facing `timeline`.
   `adminNotes` is absent from the DTO, so internal remarks cannot leak.

7. **201 Created** with a `Location` header pointing at the tracking URL.

If anything threw, one of the handlers in `GlobalExceptionHandler` turned it into the same JSON
error shape the frontend expects.

---

## 4. The two pieces of real domain logic

Everything else in this project is plumbing. These two are the parts worth talking about.

### 4.1 The pricing engine

`PricingEngine` is a pure function: `QuoteRequest` in, `QuoteResponse` out. No database, no
HTTP, no clock dependency except the quote validity date.

```
base       = moveType.baseCharge + homeSize.volumeCharge
distance   = max(0, distanceKm − freeDistanceKm) × perKmRate
packing    = base × packingPercent                           if requested
floors     = (pickupFloor + dropFloor) × perFloorCharge      only when there is no lift
insurance  = max(declaredValue × insurancePercent, minimum)  if requested
storage    = storageDays × storagePerDay
────────────────────────────────────────────────────────────
subtotal   = Σ line items
gst        = subtotal × 18%
total      = subtotal + gst
```

Three design choices in there:

- **Rates live on the enums, global knobs live in configuration.** `MoveType.baseCharge` is a
  domain fact; `app.pricing.gst-percent` is something the business changes. Adding a seventh
  move type is one enum constant and the compiler forces you to supply its price.
- **The response is a list of line items, not a number.** Each carries a machine `code`, a
  human `label` and an `explanation`. The customer can audit the price, and so can you when a
  number looks wrong.
- **`BigDecimal` throughout, `divide` always with an explicit scale and `RoundingMode`.**
  `double` would accumulate representation error, and `divide` without a rounding mode throws
  on any non-terminating result.

And because it is pure, `PricingEngineTest` covers it in eight tests that run in milliseconds.

### 4.2 The booking state machine

```
PENDING ──► CONFIRMED ──► IN_TRANSIT ──► DELIVERED   (terminal)
   │             │
   └─────────────┴──────► CANCELLED                  (terminal)
```

The rules live in `BookingStatus.ALLOWED_TRANSITIONS` (an `EnumMap` of `EnumSet`s), and
`Booking.changeStatus` is the only way to move a booking. Three consequences:

- An illegal transition throws `IllegalStateException` **from the entity**, so no controller,
  test or future batch job can bypass it.
- The API returns `allowedNextStatuses` on every booking, so the admin UI renders its buttons
  from the server's rules instead of hardcoding the workflow in React.
- The customer-facing `timeline` is derived from the same enum, so progress display and
  business rules can never disagree.

This is a **rich domain model** rather than an anaemic one: the object protects its own
invariants instead of trusting every caller to check first.

---

## 5. Security model

| Concern | How it is handled |
|---|---|
| Passwords | BCrypt (`BCryptPasswordEncoder`); the column is named `password_hash` so nobody is tempted |
| Authentication | Stateless JWT, HS256, 24-hour expiry, issuer verified on parse |
| Session | `SessionCreationPolicy.STATELESS` — no `HttpSession`, no `JSESSIONID` |
| Authorisation | URL rules in the filter chain **plus** `@PreAuthorize` on `AdminController` |
| Default | `.anyRequest().authenticated()` — a new endpoint is protected unless you open it |
| CSRF | Disabled, correctly: there is no cookie to replay. On a cookie-session app this would be a vulnerability |
| CORS | Explicit origin allow-list from an environment variable, `allowCredentials=false` |
| Enumeration | Login failures return one vague message; `hideUserNotFoundExceptions=true` equalises timing |
| IDOR | No endpoint takes a user id. `/bookings/me` derives identity from the token |
| Tracking | Reference **and** phone required, so a reference alone is not a credential |
| Mass assignment | Request DTOs have no `role` or `approved` field, so they cannot be set from outside |
| Error leakage | `prod` profile hides messages and stack traces; the catch-all handler logs server-side and returns a generic sentence |

**What the client-side `ProtectedRoute` is for:** keeping a logged-out visitor off an empty
dashboard. It is *not* security — anyone can edit JavaScript. The real protection is that the
API returns 403 regardless of what the browser believes.

---

## 6. Frontend architecture

```
main.jsx
  └─ BrowserRouter          routing context
       └─ AuthProvider      auth state (needs the router, so it sits inside)
            └─ App           the route table
                 └─ Layout   navbar + <Outlet/> + footer, mounted once
                      ├─ public pages
                      ├─ ProtectedRoute → Dashboard
                      └─ ProtectedRoute adminOnly → Admin
```

| Concern | Where it lives | Why there |
|---|---|---|
| HTTP configuration | `api/client.js` | One axios instance: base URL, token header, error normalisation |
| Endpoint list | `api/endpoints.js` | No URL string anywhere else in the app |
| Auth state | `context/AuthContext.jsx` | Needed by the navbar, the guards and three pages |
| Fetch bookkeeping | `hooks/useApiData.js` | Loading / error / reload, written once |
| Formatting | `utils/format.js` | `Intl` only — zero bundle cost |
| Presentation constants | `utils/constants.js` | Status colours and icons, which the API has no business knowing |

**Why Context and not Redux?** Auth state is one object that changes on login and logout.
Redux would add a store, actions and reducers to manage three fields. Being able to justify not
reaching for a library is as valuable as knowing how to use it.

**What the frontend deliberately does NOT own:** move types, home sizes and booking statuses.
Those come from `GET /catalog/options`, generated from the Java enums. Copy-pasting an enum into
JavaScript guarantees the two drift apart, and that drift is always discovered in production.

---

## 7. Data model

```
users ──────────┬──────────── bookings
  id            │ (nullable)   id, reference UNIQUE
  email UNIQUE  │              customer_name / email / phone   ← snapshot, not a join
  password_hash │              move_type, home_size             (enums as STRING)
  role          │              pickup_*, drop_*, distance_km
  enabled       │              estimated_amount DECIMAL(12,2)   ← frozen at booking time
                │              status + admin_notes
                └──────────── testimonials
                   (nullable)   author_name, city, rating, approved

moving_services        contact_messages
  slug UNIQUE            handled flag
  active (soft delete)
```

Three decisions to be able to defend:

- **Customer details are copied onto the booking.** A guest has no account, and even for a
  registered customer the phone number used for one move must stay attached to that move after
  they later edit their profile. This is a deliberate snapshot, not a normalisation failure.
- **`user_id` is nullable with `ON DELETE SET NULL`.** Deleting a user must not delete the
  company's order history.
- **`estimated_amount` is stored, not recomputed.** If the price table changes next month, the
  customer must still see the figure they agreed to.

Indexes follow the queries that actually run: `status` and `created_at` for the admin list,
`customer_email` for lookups, `approved` for the public review feed.

---

## 8. Where to look next

- Every backend file, explained: [02-BACKEND-EXPLAINED.md](02-BACKEND-EXPLAINED.md)
- Every frontend file, explained: [03-FRONTEND-EXPLAINED.md](03-FRONTEND-EXPLAINED.md)
- Getting it live on a free tier: [04-DEPLOYMENT.md](04-DEPLOYMENT.md)
- The questions this code invites: [05-INTERVIEW-QA.md](05-INTERVIEW-QA.md)
