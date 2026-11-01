# MoveMate — Packers & Movers Platform

A full-stack packers-and-movers web application: **Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · MySQL 8 · Flyway · React 18 · Vite · Tailwind CSS · Docker**.

Not a CRUD demo. The core of the project is a **server-side pricing engine** that produces an
itemised moving quote, and a **booking lifecycle enforced as a state machine** on the domain
model — the two things that are actually hard about this business domain.

---

## What it does

**For a visitor**
- Instant itemised quote — base charge, distance beyond the free 25 km, packing, staircase
  access, transit insurance, warehousing and GST, each as its own line with a plain-language
  explanation of how it was calculated
- Book a move **without creating an account**, and get a human-readable reference (`MM-2026-4F7K2Q`)
- Track a booking with the reference plus the phone number on it
- Browse services, read moderated customer reviews, send an enquiry

**For a registered customer**
- Dashboard of their own bookings with live status timeline
- Submit a review (published only after admin approval)

**For an admin**
- Dashboard: bookings by status, pipeline value, unhandled enquiries, pending reviews
- Booking list with server-side search, status filter and pagination
- Advance a booking through the workflow — the API only offers legal transitions
- Moderate reviews, work the enquiry inbox, manage the service catalogue

---

## Live links (fill these in after deploying)

| | URL |
|---|---|
| Website | `https://movemate-web.vercel.app` |
| API (Swagger UI) | `https://movemate-api.up.railway.app/swagger-ui.html` |
| Health check | `https://movemate-api.up.railway.app/actuator/health` |

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@movemate.in` | `Admin@123` |
| Customer | `customer@movemate.in` | `Customer@123` |

> These are seeded on first startup by [`DataSeeder`](backend/src/main/java/com/movemate/config/DataSeeder.java).
> In production, override them with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables.

---

## Documentation

Written for someone preparing to **explain this code in an interview**, not just run it.

| Document | What is in it |
|---|---|
| [docs/01-ARCHITECTURE.md](docs/01-ARCHITECTURE.md) | Layers, request flow, why each decision was made |
| [docs/02-BACKEND-EXPLAINED.md](docs/02-BACKEND-EXPLAINED.md) | Every backend file, what it does and why it exists |
| [docs/03-FRONTEND-EXPLAINED.md](docs/03-FRONTEND-EXPLAINED.md) | Every frontend file, same treatment |
| [docs/04-DEPLOYMENT.md](docs/04-DEPLOYMENT.md) | Railway + Vercel, step by step, free tier |
| [docs/05-INTERVIEW-QA.md](docs/05-INTERVIEW-QA.md) | 50 questions this code invites, with answers |
| [docs/06-RESUME-BULLETS.md](docs/06-RESUME-BULLETS.md) | How to put this on a resume without exaggerating |

---

## Running it locally

### Fastest path — zero installation beyond a JDK

The `dev` profile runs the whole application against an **embedded H2 database**: no MySQL
server, no Docker, no cloud account.

```bash
cd packers-movers/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd packers-movers/frontend && npm install && npm run dev
```

Open <http://localhost:5173>. The schema is created by Hibernate and `DataSeeder` populates the
catalogue, reviews and both demo accounts on every start.

> **The tradeoff, stated plainly:** in this profile dev runs on H2 while production runs on
> MySQL, and the dev schema comes from the **entities** while production's comes from the
> **Flyway migrations** — so the two can drift. Production never uses this profile, and any
> schema change must still be written as a migration to reach it. Use the MySQL instructions
> below before shipping anything schema-related.

### Prerequisites

| Tool | Version | Why |
|---|---|---|
| JDK | 17 or 21 | Spring Boot 3 requires 17+ |
| Maven | 3.9+ | Build tool (or use your IDE's bundled Maven) |
| Node.js | 18+ | Frontend build |
| MySQL | 8.0 | Database (or run it via Docker — see below) |

Install on Windows with [winget](https://learn.microsoft.com/windows/package-manager/winget/):

```bash
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install OpenJS.NodeJS.LTS
winget install Oracle.MySQL
```

Close and reopen your terminal afterwards so `PATH` updates, then verify:

```bash
java -version && mvn -v && node -v
```

### Option A — Docker for the database (fewest moving parts)

```bash
cd packers-movers
docker compose up -d mysql
```

That gives you MySQL 8 on `localhost:3306` with an empty `movemate` schema. Then start the
backend from your IDE or with Maven.

### Option B — native MySQL

Create the schema (the app also creates it automatically via `createDatabaseIfNotExist=true`):

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS movemate;"
```

If your MySQL root password is not `root`, set it before starting the backend:

```bash
export DB_PASSWORD=your_password   # PowerShell: $env:DB_PASSWORD="your_password"
```

### Backend

```bash
cd packers-movers/backend
mvn spring-boot:run
```

On first start it will: run the two Flyway migrations, seed six services and six reviews, create
the admin and demo customer accounts, and serve on <http://localhost:8080>.

Check it:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

### Frontend

```bash
cd packers-movers/frontend
npm install
npm run dev
```

Open <http://localhost:5173>.

### Tests

```bash
cd packers-movers/backend
mvn test
```

These are pure unit tests — no database, no Spring context — so they run in about a second.

---

## API reference

Base path `/api/v1`. Full interactive docs at `/swagger-ui.html`.

### Public

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/auth/register` | Create a customer account, returns a JWT |
| `POST` | `/auth/login` | Exchange credentials for a JWT |
| `GET` | `/catalog/services` | Active service catalogue |
| `GET` | `/catalog/services/{slug}` | One service |
| `GET` | `/catalog/options` | Dropdown options generated from the Java enums |
| `POST` | `/quotes/estimate` | Itemised price estimate |
| `POST` | `/bookings` | Create a booking (guest or logged in) |
| `GET` | `/bookings/track?reference=&phone=` | Track a booking |
| `GET` | `/testimonials` | Approved reviews |
| `POST` | `/contact` | Send an enquiry |

### Authenticated (customer)

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/auth/me` | Current profile |
| `GET` | `/bookings/me` | My bookings |
| `POST` | `/testimonials` | Submit a review for moderation |

### Admin (`ROLE_ADMIN`)

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/admin/stats` | Dashboard counts and totals |
| `GET` | `/admin/bookings?status=&query=&page=&size=` | Search bookings |
| `GET` | `/admin/bookings/{reference}` | One booking |
| `PATCH` | `/admin/bookings/{id}/status` | Advance the workflow |
| `GET` | `/admin/enquiries` | Enquiry inbox |
| `PATCH` | `/admin/enquiries/{id}/handled?handled=` | Mark handled |
| `GET` | `/admin/testimonials` | Moderation queue |
| `PATCH` | `/admin/testimonials/{id}/approval?approved=` | Approve / hide |
| `DELETE` | `/admin/testimonials/{id}` | Delete a review |
| `GET` `POST` | `/admin/services` | List / create catalogue entries |
| `PUT` | `/admin/services/{id}` | Update a catalogue entry |
| `PATCH` | `/admin/services/{id}/toggle` | Show / hide (soft delete) |

### Try it from the command line

```bash
curl -X POST http://localhost:8080/api/v1/quotes/estimate \
  -H "Content-Type: application/json" \
  -d '{"moveType":"LOCAL_HOUSE_SHIFTING","homeSize":"TWO_BHK","distanceKm":15,"pickupFloor":0,"dropFloor":0,"liftAvailable":true,"packingRequired":true,"insuranceRequired":false,"storageDays":0}'
```

Returns base 5,500 + packing 1,210 = subtotal 6,710, GST 1,207.80, **total 7,917.80**.

---

## Project layout

```
packers-movers/
├── backend/
│   ├── src/main/java/com/movemate/
│   │   ├── MoveMateApplication.java     entry point
│   │   ├── config/                      security, CORS, OpenAPI, seeding, pricing config
│   │   ├── domain/                      JPA entities + domain enums (the state machine)
│   │   ├── dto/                         request/response records
│   │   ├── exception/                   custom exceptions + @RestControllerAdvice
│   │   ├── repository/                  Spring Data interfaces + Specifications
│   │   ├── security/                    JWT service, filter, principal, entry point
│   │   ├── service/                     business logic (pricing engine lives here)
│   │   └── web/                         REST controllers
│   ├── src/main/resources/
│   │   ├── application.properties       dev config
│   │   ├── application-prod.properties  production overrides
│   │   └── db/migration/                Flyway SQL — the schema's source of truth
│   ├── src/test/java/                   JUnit 5 unit tests
│   ├── Dockerfile                       multi-stage build
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/                         axios instance + every endpoint in one file
│   │   ├── components/                  ui / layout / home / booking / admin
│   │   ├── context/AuthContext.jsx      auth state
│   │   ├── hooks/useApiData.js          loading / error / reload hook
│   │   ├── pages/                        one file per route
│   │   ├── utils/                        formatting + presentation constants
│   │   ├── App.jsx                       the route table
│   │   └── main.jsx                      bootstrap
│   ├── tailwind.config.js
│   └── vite.config.js
├── docs/                                 the six explainer documents
├── docker-compose.yml                    MySQL + API for local development
└── render.yaml                           Render blueprint (see the MySQL caveat inside)
```

---

## Engineering decisions worth asking about

These are deliberate, and each one is explained where it appears in the code:

1. **Prices are recalculated server-side on booking.** The browser's estimate is never trusted —
   `BookingService.create` runs the same `QuoteService` the quote endpoint uses.
2. **Flyway owns the schema**, with `spring.jpa.hibernate.ddl-auto=none`. The schema is
   reviewable SQL in git, not a side effect of entity annotations.
3. **The booking workflow is a state machine on the enum.** `BookingStatus.canTransitionTo`
   plus `Booking.changeStatus` make an illegal transition impossible from any call site.
4. **`open-in-view=false`** and `FetchType.LAZY` on every `@ManyToOne` — the two settings that
   prevent the N+1 query problem from being invisible until production.
5. **Response DTOs, never entities.** `passwordHash` and `adminNotes` cannot leak because they
   are not in the response types at all.
6. **One `@RestControllerAdvice`** gives every error the same JSON shape, which is why the
   frontend's error handling is about twenty lines.
7. **Dropdown options come from the backend enums** (`/catalog/options`), so the React forms
   and the Java enums cannot drift apart.
8. **Deny-by-default security chain**, ending in `.anyRequest().authenticated()`.
9. **`BigDecimal` for all money**, never `double`.
10. **Debounced live pricing** in the quote form — one request after typing stops, not one per
    keystroke.

---

## Known limitations

Stated plainly, because being able to name the gaps in your own work is worth more than
pretending there are none:

- **No refresh tokens.** A single 24-hour access token; expiry means re-login. Refresh tokens
  with rotation would be the next step.
- **JWTs cannot be revoked** before expiry. The filter re-reads the user on every request so a
  disabled account loses access immediately, but a stolen token stays valid until it expires.
- **Distance is customer-supplied**, not computed. A real product would call a maps distance
  API and validate it.
- **No payment gateway.** Bookings are quoted and tracked, not paid for.
- **No email or SMS.** Enquiries and bookings are persisted; notification is a TODO marked in
  `ContactService`.
- **Free-text search uses `LIKE '%term%'`**, which cannot use an index. Fine at this scale,
  wrong at a million rows — MySQL `FULLTEXT` or a search engine would be the fix.
- **Rate limiting is per-email and crude.** Real protection belongs at the proxy, or Bucket4j
  keyed on IP.

---

## Licence

MIT. A portfolio project — not a real trading company.
