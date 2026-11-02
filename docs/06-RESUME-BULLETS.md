# 06 — Putting this on your resume

How to present this project without overstating it. Everything below is defensible against the
code, which is the only test that matters when an interviewer opens the repository.

---

## The project block

Use this shape. Swap in your real URLs.

> **MoveMate — Packers & Movers Platform** · *Personal project*
> `Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · MySQL 8 · Flyway · React 18 · Vite · Tailwind · Docker`
> [Live site](https://…) · [API docs](https://…/swagger-ui.html) · [GitHub](https://github.com/…)
>
> - Built a full-stack moving-services platform with a **server-side pricing engine** that
>   returns an itemised quote (base, distance, packing, floor access, insurance, storage, GST)
>   from eight inputs; prices are recalculated on booking so a client-supplied total can never
>   be trusted.
> - Modelled the booking lifecycle as a **state machine on the domain enum**
>   (`PENDING → CONFIRMED → IN_TRANSIT → DELIVERED | CANCELLED`); illegal transitions are
>   rejected by the entity itself and the API returns the legal next states, so the admin UI
>   cannot offer an invalid action.
> - Secured a **stateless REST API** with JWT authentication, BCrypt password hashing,
>   role-based authorisation and a deny-by-default filter chain; public pricing, authenticated
>   booking and admin access are three distinct authorisation paths.
> - Managed the MySQL schema with **Flyway migrations** (`ddl-auto=none`), with `DECIMAL` money
>   columns, FK `ON DELETE SET NULL` to preserve order history, and indexes chosen from the
>   queries actually run.
> - Implemented **server-side filtering, pagination and search** with JPA Criteria
>   Specifications, and avoided N+1 queries with lazy `@ManyToOne` fetching and
>   `open-in-view=false`.
> - Built the React 18 SPA with protected routes, a central axios client with request/response
>   interceptors, **debounced live pricing**, and loading / error / empty states on every data
>   screen.
> - Containerised the API with a **multi-stage Dockerfile** (~200 MB runtime image,
>   `MaxRAMPercentage` tuned for small containers) and deployed to Railway with the frontend on
>   Vercel; documented the API with OpenAPI / Swagger UI.

### Shorter version (one or two lines, if space is tight)

> **MoveMate — Packers & Movers Platform** · [Live](https://…) · [GitHub](https://…)
> Full-stack Spring Boot 3 + MySQL + React application with a server-side itemised pricing
> engine, JWT auth with role-based access, Flyway-managed schema, Criteria-API search with
> pagination, and an admin panel enforcing a booking state machine. Dockerised and deployed.

---

## Why these bullets work

Each one names a **decision**, not a feature. Compare:

| Weak | Strong | Why |
|---|---|---|
| "Created a booking website using Spring Boot and React" | "Server-side pricing engine; prices recalculated on booking so a client total is never trusted" | The second shows you thought about trust boundaries |
| "Used MySQL for the database" | "Flyway-managed schema with `ddl-auto=none`, `DECIMAL` money columns, FK `ON DELETE SET NULL`" | The second is three specific decisions an interviewer can dig into |
| "Implemented login and signup" | "Stateless JWT auth, BCrypt hashing, deny-by-default filter chain, three distinct authorisation paths" | The second shows you know what you built, not just that it works |
| "Added an admin panel" | "Admin UI renders actions from server-computed legal state transitions" | The second shows a design idea |

**Every bullet is a question you want to be asked.** That is the point — you have fifty answers
ready in [05-INTERVIEW-QA.md](05-INTERVIEW-QA.md).

---

## Skills section wording

Add or confirm these, since the project genuinely demonstrates them:

- **Languages:** Java 17, JavaScript (ES2022), SQL, HTML5, CSS3
- **Backend:** Spring Boot 3, Spring MVC, Spring Security, Spring Data JPA, Hibernate, Bean
  Validation, REST API design, JWT, OpenAPI/Swagger
- **Frontend:** React 18, React Router, Vite, Tailwind CSS, Axios
- **Database:** MySQL 8, Flyway migrations, schema design, indexing, JPA Criteria API
- **Tools & DevOps:** Maven, Git, Docker (multi-stage builds), Docker Compose, JUnit 5, AssertJ,
  Railway, Vercel, Postman

Only claim what this project (plus your other work) actually shows. An interviewer who finds
"Kubernetes" on a resume and nothing to back it up discounts everything else on the list.

---

## What to say out loud

### The 30-second version

> "It's a packers-and-movers platform — Spring Boot and MySQL on the backend, React on the
> front. The interesting part isn't the CRUD; it's the pricing engine. A move's cost depends on
> home size, distance, floor access, packing, insurance and storage, so I built an engine that
> returns an itemised breakdown with an explanation for each line. The quote endpoint and the
> booking endpoint share that one implementation, so the price is recalculated server-side and
> the client's number is never trusted. The other piece I'd point at is the booking lifecycle —
> it's a state machine on the enum, and the entity rejects illegal transitions itself, so no
> caller can corrupt the workflow."

### If they ask what was hardest

> "Getting the shape of the pricing engine right, not writing it. My first version had a big
> switch statement in the service. Moving the per-move-type rates onto the enums and keeping the
> global multipliers in configuration made the formula readable, made adding a move type a
> one-line change the compiler checks, and left the engine a pure function — so it's fully
> unit-tested without Spring or a database."

### If they ask about limitations

Have these ready. Naming your own gaps is a strong signal, and the README already lists them:

> "Three I'd fix first. There are no refresh tokens — a single 24-hour access token, which is
> pragmatic but means a stolen token stays valid until it expires; I mitigate it by re-loading
> the user on every request so a disabled account loses access immediately. The free-text search
> uses `LIKE '%term%'`, which can't use an index — fine at this scale, wrong at a million rows.
> And distance is customer-supplied rather than computed from a maps API, which is a product gap
> as much as a technical one."

---

## Before you share the link

- [ ] The live site loads and the homepage shows services and reviews
- [ ] A full booking works end to end, and tracking finds it
- [ ] Swagger UI opens and the **Authorize** button works
- [ ] Demo credentials are in the README **and** on the login page
- [ ] `git log` has sensible commit messages — a recruiter may read them
- [ ] The README's "Live links" table has real URLs, not placeholders
- [ ] No `.env` file and no secret in the repository: `git log -p | grep -i -E "password|secret"`
      should show only placeholders and env-var names
- [ ] If the backend is on a sleeping free tier, say so on the resume: *"hosted on a free tier —
      the first request may take up to a minute to wake the server."* Honest, and it stops a
      reviewer concluding the site is broken

---

## One honest caveat

Label it as a personal or portfolio project, never as client or production work. Interviewers
check, and a padded claim costs you far more than the project gains you. A well-engineered
personal project with decisions you can defend beats an exaggerated one every time.

---

Back to the [README](../README.md).
