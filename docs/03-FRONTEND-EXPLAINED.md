# 03 — Frontend, file by file

Every file in `frontend/`, what it does and why. Same treatment as the backend document.

---

## Build configuration

### `package.json`

| Dependency | Why it is here |
|---|---|
| `react` / `react-dom` 18.3 | The UI library and its DOM renderer, split because React also targets native and other renderers |
| `react-router-dom` 6 | Client-side routing: nested routes, route params, programmatic navigation |
| `axios` | HTTP client, chosen over `fetch` for **interceptors** — the token header and error normalisation are configured once instead of at every call site |
| `react-hot-toast` | Notifications. One `<Toaster/>` at the root serves the whole tree |
| `lucide-react` | Tree-shakeable SVG icons, imported by name |
| `tailwindcss` / `postcss` / `autoprefixer` | The styling pipeline |
| `vite` + `@vitejs/plugin-react` | Dev server and production bundler |
| `eslint` + plugins | Static analysis, especially `react-hooks/exhaustive-deps` |

`"type": "module"` makes Node treat `.js` files as ES modules, which is why the ESLint config
has to be `.eslintrc.cjs`.

### `vite.config.js`

**Why Vite and not Create React App?** CRA is deprecated and unmaintained. Vite serves raw ES
modules in development, so the dev server starts in under a second regardless of project size,
and HMR updates a component without losing its state. For production it bundles with Rollup into
hashed, tree-shaken, minified assets.

`strictPort: true` fails loudly instead of silently moving to 5174 — a different port no longer
matches the CORS allow-list on the Spring Boot side, and the resulting errors are confusing.

`sourcemap: true` makes a production stack trace readable instead of pointing at minified
one-letter variables, and costs nothing at runtime (the browser fetches them only when devtools
is open).

### `tailwind.config.js`

Tailwind is utility-first: you compose `text-4xl font-bold tracking-tight` on the element
instead of writing a `.hero-title` class elsewhere. Two concrete benefits here:

1. **No dead CSS.** The build scans the `content` globs and ships only the classes actually
   used, so the production stylesheet is a few kilobytes.
2. **No naming debates and no cascade bugs** — the styles live with the markup.

**The `content` array is the one thing to get right.** A file missing from it gets its classes
stripped from the production build and renders unstyled — the most common Tailwind setup
mistake, and it only shows up after `npm run build`.

`theme.extend` defines the brand palette in one place, so a rebrand is one file rather than a
find-and-replace across forty components.

### `postcss.config.js`

Vite runs CSS through PostCSS; these two plugins do the work. `tailwindcss` replaces the
`@tailwind` directives with the utilities it found; `autoprefixer` adds vendor prefixes based on
browserslist. Without this file the `@tailwind` directives would be shipped verbatim and nothing
would be styled.

### `index.html`

The single HTML file. `<div id="root">` is where React mounts — that is what "single page
application" means: one document, routing in JavaScript.

- `<meta name="viewport">` is mandatory for any responsive layout; without it mobile browsers
  render at 980px and zoom out, making every Tailwind breakpoint useless.
- SEO title leads with the service, not the brand, because a moving company lives on local
  search.
- Open Graph tags control how the link renders on WhatsApp and LinkedIn — worth having when you
  are going to paste this URL on a resume.
- `preconnect` to the font host opens the TCP+TLS handshake early so the font request does not
  have to wait for it.
- The favicon is an inline SVG data URI: no extra request, scales crisply.

### `.env` / `.env.example`

Vite only exposes variables prefixed `VITE_` to the browser bundle. **That prefix is a safety
feature, not a convention**: it stops a server secret in your shell environment from being
compiled into public JavaScript.

Corollary worth saying out loud: **never put a secret in a `VITE_` variable.** Anything there is
readable in the browser's Sources tab. And because Vite inlines them at *build* time, changing
one requires a rebuild, not a restart.

### `.eslintrc.cjs`

The rule worth understanding is `react-hooks/exhaustive-deps`: it warns when a `useEffect` reads
a value that is not in its dependency array — the cause of "stale closure" bugs where an effect
keeps using the first render's value forever. When you genuinely need to ignore it (as in
`useApiData`), silence that **one line** with a comment explaining why, rather than disabling the
rule globally.

### `vercel.json` / `netlify.toml` / `public/_redirects`

All three exist to express one rule: **serve `index.html` for any path that is not a real file,
with status 200.**

Why it is non-negotiable: `BrowserRouter` uses the History API, so URLs are real paths
(`/services`, not `/#/services`). A hard refresh on `/services` makes the browser ask the static
host for a file called `services`, which does not exist. Without the rewrite the visitor gets
the host's 404 and React never loads. Status 200 rather than 301 keeps the URL intact so React
Router can read it.

The long `Cache-Control` on `/assets/*` is safe because Vite puts a content hash in every
filename — a changed file gets a new name, so a stale cache can never serve the wrong version.

---

## Application shell

### `src/main.jsx`

Bootstrap. **The nesting order is not arbitrary** — each provider must sit outside anything that
consumes it:

```
StrictMode          development-only checks
  BrowserRouter     provides routing context (useNavigate, <Link>, useParams)
    AuthProvider    calls useNavigate, so it must be INSIDE the router
      App           consumes both
```

Putting `AuthProvider` outside `BrowserRouter` throws "useNavigate() may be used only in the
context of a `<Router>`" — a classic mistake.

**StrictMode** deliberately double-invokes component bodies and effects **in development only**,
to surface side effects that are not idempotent. If a request fires twice locally but once in
production, that is why — and it is telling you the effect has a bug worth fixing, not that
StrictMode should be removed.

### `src/index.css`

The three `@tailwind` directives are placeholders PostCSS replaces:

- `base` — Tailwind's reset (Preflight). It removes default margins, makes images block-level
  and unstyles headings, which is why a Tailwind page looks identical across browsers. It also
  sets `font-family: inherit` on form elements, which browsers do *not* do by default.
- `components` — where `@apply`-based classes land.
- `utilities` — every utility class the content scan found.

**Order matters:** utilities last, so a utility on an element can override a component class.
That is CSS specificity, not magic.

`:focus-visible` (not `:focus`) for the focus ring means mouse users do not see a ring on every
click while keyboard and screen-reader users keep the indicator they depend on. Removing focus
outlines entirely is one of the most common accessibility failures on the web.

`@apply` is used sparingly — only for genuinely global primitives (`.container-page`, `.card`,
`.input-base`). The moment something needs variants or state, a React component is the better
abstraction, which is why `Button` is JSX.

### `src/App.jsx`

The route table — one readable map of the whole application.

```jsx
<Route element={<Layout />}>            // layout route: no path, provides the shell via <Outlet/>
  <Route index element={<Home/>} />     // `index` = the parent's path exactly, i.e. "/"
  <Route path="services/:slug" .../>    // URL parameter, read with useParams()
  <Route element={<ProtectedRoute/>}>   // nested guard: children inherit the check
    <Route path="dashboard" .../>
  </Route>
  <Route path="*" element={<NotFound/>} />   // catch-all, always last
</Route>
```

**Nesting the guard rather than repeating it inside each page is the key idea:** add a new admin
screen under that branch and it is protected by construction, not by remembering.

### `src/components/layout/Layout.jsx`

The shared shell. `<Outlet />` is where the router renders the matched child.

Using a **layout route** means the navbar and footer mount **once** and never re-render on
navigation. Importing them into every page instead would remount them on each route change,
resetting the mobile menu state and re-triggering animations.

It also owns the scroll-to-top effect. An SPA does not reload the document, so the browser keeps
the previous scroll position — click a footer link and you land halfway down the new page. Every
SPA needs this; almost every tutorial app forgets it.

`min-h-screen flex flex-col` + `flex-1` on `<main>` is the standard sticky-footer layout.

### `src/components/layout/Navbar.jsx`

- **`NavLink`, not `Link`**, for the nav items: it provides an `isActive` flag so the current
  page highlights itself. Comparing `useLocation().pathname` by hand reinvents this.
- **`end={link.to === '/'}`** — without `end`, the Home link matches every route, because every
  path starts with `/`.
- The mobile drawer is **conditionally rendered**, not hidden with CSS, so its links are not
  reachable by keyboard while invisible.
- `aria-label` + `aria-expanded` on the hamburger are what make it accessible: the label names
  the control, the attribute reports its state.
- Links use `buttonClasses()` rather than wrapping a `<button>` in a `<Link>` — a button inside
  a link is invalid HTML and breaks keyboard navigation.

### `src/components/layout/Footer.jsx`

`new Date().getFullYear()` rather than a hardcoded year: a site still showing 2024 tells every
visitor nobody is maintaining it.

`tel:` and `mailto:` links open the dialler and mail app on mobile, far better than printing a
number to copy by hand. External links carry `rel="noopener noreferrer"` — `noopener` stops the
new tab reaching back through `window.opener`, `noreferrer` withholds the referrer header.

It also links to Swagger UI, which is deliberate on a portfolio site: a reviewer can confirm the
backend is real without cloning anything.

---

## Data layer

### `src/api/client.js`

The single configured axios instance. Every component imports **this**, not axios. That one rule
gives you the base URL in one place, the `Authorization` header attached automatically, uniform
error handling, and — crucially — **no hardcoded localhost URL anywhere**, which is the mistake
that breaks an app the moment it is deployed.

**Request interceptor** attaches the bearer token, so `headers: { Authorization: … }` is not
copy-pasted into thirty call sites and forgotten in the thirty-first.

**Response interceptor** normalises three genuinely different failures into one shape so the UI
does not have to tell them apart:

1. `error.response` exists — the server answered 4xx/5xx. Our Spring Boot `ApiError` always has
   `{ message, fieldErrors }`, which is exactly why this handling is so short.
2. `error.request` exists but no response — network down, CORS blocked, timeout.
3. Neither — a bug in our code before the request was sent.

The 401 branch clears the token and triggers a logout, **except on `/auth/` URLs** — a wrong
password on the login form is also a 401, and bouncing the user off the login page would be
absurd.

`registerUnauthorizedHandler` exists because this module cannot import `AuthContext` (circular
import), so the context registers a callback instead.

`timeout: 15000` — without a timeout, axios waits indefinitely and a sleeping free-tier backend
leaves the user staring at a spinner with no error and no way to retry.

### `src/api/endpoints.js`

Every backend call in one file. The value is not the few lines of axios it wraps — it is that
**no component contains a URL string**. When the backend renames an endpoint, one line changes.
It also makes the whole API surface readable at a glance, which genuinely helps in a code review
or an interview walkthrough.

Query parameters go through axios `params` rather than string concatenation, so axios
URL-encodes them and a reference containing a special character cannot break the request. Axios
also omits `undefined` values entirely, which is exactly what the backend's optional filters
expect.

### `src/context/AuthContext.jsx`

**Why Context and not Redux?** Auth state is one small object that changes on login and logout.
Redux would add a store, actions and reducers to manage three fields. Context is part of React
and has no dependency. Being able to justify *not* using a library is as valuable as knowing
how to use it.

**Why Context and not props?** The navbar, both route guards, the dashboard and the quote form
all need the user. Threading it through every intermediate component is prop drilling, and it
couples components that have no business knowing about auth.

Details that matter:

- **`loading` starts `true`.** On a page refresh there is a token but no user yet; without this
  flag `ProtectedRoute` would briefly see `user === null` and bounce an authenticated admin to
  the login page on every F5.
- **The token is cached, the profile is not.** The profile is re-fetched from `/auth/me` because
  a cached copy could be stale (role changed, account disabled) and because anything in
  `localStorage` can be edited by the user — trusting a cached `role: "ROLE_ADMIN"` would be a
  client-side authorisation decision.
- **`useCallback` on `logout`** keeps its identity stable, because it is a `useEffect`
  dependency; an unstable function there would re-run the effect on every render.
- **`cancelled` flag in the cleanup** guards against setting state after unmount — a leak React
  warns about, and StrictMode's double-invoke makes it easy to hit.
- **`useMemo` on the context value** keeps the object identity stable. Without it a new object
  is created on every provider render and *every* consumer re-renders. This is the standard
  Context performance fix.
- **`useAuth` throws** when used outside the provider, turning a confusing downstream
  "cannot read property user of null" into a message naming the actual mistake.

**`isAdmin` is a UI convenience only.** The real check is server-side.

### `src/hooks/useApiData.js`

A custom hook for "fetch, track loading and errors, allow a reload". A custom hook is just a
function that calls other hooks, which is the whole point of hooks: behaviour becomes reusable
without inheritance or wrapper components.

The interesting part is `fetcherRef`. The fetcher is usually an inline arrow function, so it is a
**new function on every render** — putting it in the dependency array would cause an infinite
fetch loop. Storing it in a ref gives the effect access to the latest version without making it
a dependency: the standard solution to the "stale closure vs infinite loop" dilemma.

`reloadToken` is a counter that callers bump through `reload()` to refetch after a mutation.

**Honest note:** in production you would likely use TanStack Query, which adds caching,
deduplication and background refetching. Using it here would hide the mechanics this project is
meant to demonstrate — but knowing when to reach for it is the better interview answer.

### `src/utils/format.js`

All formatting uses the built-in `Intl` API, so this file adds **zero kilobytes** to the bundle.
(moment.js alone is ~70 kB.) Knowing that `Intl` covers most formatting needs is a practical,
bundle-size-aware answer to "how do you format dates in React?".

Two real bugs it avoids:

- **`formatDate`** splits `"2026-10-04"` and uses `new Date(year, month-1, day)`.
  `new Date('2026-10-04')` parses as **UTC midnight**, which in a timezone behind UTC displays
  as 3 October — the classic off-by-one-day bug.
- **`todayIso`** pads manually rather than using `toISOString()`, which converts to UTC and can
  return yesterday's date for anyone east of Greenwich in the early morning.

`formatCurrency` uses the `en-IN` locale, so amounts group as 1,25,000 (lakh/crore) rather than
125,000 — exactly the kind of thing it is a mistake to implement with a regex.

### `src/utils/constants.js`

Note what is here and what is **not**. Move types, home sizes and booking statuses are *not*
here — they come from `GET /catalog/options`, because duplicating a Java enum in JavaScript
guarantees the two drift apart. What lives here is purely cosmetic: status colours, icon keys,
marketing copy — things the API has no business knowing.

---

## UI primitives

### `src/components/ui/Button.jsx`

Exports both a component and `buttonClasses()`. The class builder is exported because a styled
`<Link>` must render an `<a>`, not a `<button>`; sharing the class function keeps a link-button
and a real button visually identical without faking either element.

**`type="button"` is the default on purpose.** A `<button>` inside a `<form>` defaults to
`type="submit"` in HTML, so a "Cancel" button with no type submits the form — a bug that is
genuinely hard to spot. Forms opt in with `type="submit"`.

`loading` both shows a spinner and disables the button, so the caller never manages two pieces of
state. `disabled:pointer-events-none` is what prevents a double submit from an impatient second
click.

`{...rest}` is placed after `className` so an explicit prop from the caller wins — and it is
what keeps the wrapper from hiding the native button API.

### `src/components/ui/Field.jsx`

`Input`, `Select`, `Textarea` and `Checkbox` share one `FieldShell`, so the label, hint, error
message and accessibility wiring are implemented once. On a nineteen-field booking form that is
the difference between a maintainable form and nineteen copies of the same five lines.

**The accessibility part most tutorials skip** is three attributes:

| Attribute | Effect |
|---|---|
| `htmlFor` + `id` | Clicking the label focuses the input |
| `aria-invalid` | Tells a screen reader the field is in an error state |
| `aria-describedby` | Points the screen reader at the error text to read out |

`useId()` generates a unique, stable id per instance. Hardcoding `id="email"` breaks the
label-input link as soon as two forms appear on a page, and duplicate ids are invalid HTML.

The error **replaces** the hint rather than stacking, so the layout does not jump.

The `Checkbox` wraps everything in a `<label>` so clicking the description also toggles it — a
small thing that matters a lot on a phone. `checked` + `onChange` makes it a **controlled**
input: React state is the single source of truth. Passing `checked` without `onChange` produces
a read-only input and a React warning, one of the most common React form bugs.

### `src/components/ui/Feedback.jsx`

`Spinner`, `Alert`, `EmptyState`, `Skeleton` — the three states every data-driven screen needs
plus a placeholder.

Most tutorial apps render only the success case, so a slow network shows a blank page and a
failed request shows nothing. Handling all three is the difference between a demo and something
you would put in front of a customer, and it is the first thing a reviewer notices.

- `role="status"` on the spinner makes a screen reader announce the change without stealing
  focus; `role="alert"` on an error announces it immediately.
- **The empty state explains why it is empty and offers the next action.** "No bookings yet —
  get a quote" is useful; a blank rectangle makes the user think the page is broken.
- `Skeleton` is preferred over a spinner where the content's shape is known, because the layout
  does not shift when data arrives (better Cumulative Layout Shift).

### `src/components/ui/Card.jsx`

`Card`, `SectionHeading`, `StatusBadge`, `Badge`, `Stars`.

`SectionHeading` was extracted because the pattern appears nine times. **Rule of thumb: once a
pattern appears a third time, give it a name — but not before**, or the abstraction is shaped by
too few examples.

`StatusBadge` looks the colour up with `?? 'bg-slate-100 …'`, so a status added on the backend
renders in grey instead of crashing on `undefined`. Defensive lookups are cheap; a white screen
in production is not.

### `src/components/ui/ServiceIcon.jsx`

Maps the backend's `icon` string to a React component, keeping the API free of frontend concerns.

The icons are imported **by name**, not with `import * as Icons from 'lucide-react'`. The
namespace import defeats tree-shaking and pulls in the entire library — over a thousand
components. Naming the six we use keeps it to six. A genuinely common and invisible bundle-size
mistake.

### `src/components/ui/Modal.jsx`

Three behaviours most hand-rolled modals get wrong, and that separate "looks like a modal" from
"works like one": Escape closes it, the page behind does not scroll, and clicking the backdrop
closes it while clicking inside does not.

The `useEffect` cleanup is the important half: without removing the keydown listener, every open
adds another; without restoring `overflow`, the page stays frozen after it closes.

Returning `null` when closed unmounts the content entirely, so its inputs are not focusable
while hidden — unlike a CSS-only `display: none`, which leaves them in the tab order.

**Stated limitation:** it does not trap focus or restore focus to the trigger. In production the
native `<dialog>` element or Radix would handle that. Being deliberate about what it does *not*
solve is better than pretending there is no gap.

### `src/components/ProtectedRoute.jsx`

The route guard. **Be clear about what it is**: a UX control that keeps a logged-out visitor off
an empty dashboard. It is **not security** — anyone can edit JavaScript and render the admin
page, and they would see nothing but 403s, because the real protection is
`requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` on the server. *Client-side guards are a
convenience; the server is the authority.*

The `loading` gate is essential (see `AuthContext` above). `state={{ from: location.pathname }}`
lets Login send the user back where they were trying to go, and `replace` stops the Back button
bouncing between the guarded page and the login screen.

---

## Pages and sections

### `src/pages/Home.jsx`

Deliberately almost empty: it composes six sections with no state and no logic. That is the goal
for a page component — it decides *what* appears and in what order, while each section owns its
own data fetching.

The practical benefit: the services grid failing cannot affect the testimonials, and
`ServicesGrid` is reused on `/services` with a different `limit`.

### `src/components/home/Hero.jsx`

One conversion goal, so **one** primary action in the accent colour and one quiet secondary.
Two equally loud buttons split attention and lower the click-through on both.

The background is a pure CSS gradient rather than an image: no extra request, no layout shift
while it loads, sharp at every screen density.

Exactly one `<h1>` per page, containing the primary keyword — it matters for SEO and for
screen-reader users who navigate by heading. The stats use `<dl>/<dt>/<dd>`, the semantically
correct elements for label-value pairs; a grid of divs would look identical and mean nothing to
assistive technology.

### `src/components/home/ServicesGrid.jsx`

The first component that talks to the backend, and it establishes the pattern every data
component follows: skeleton while loading, alert on failure, content on success. **Never assume
the happy path.**

The `key` on each card is the **database id** — stable, unique and unchanged when the list is
reordered. React uses keys to decide which DOM nodes to reuse; an array index would make React
reuse the wrong card after a sort.

### `src/components/home/TestimonialsSection.jsx`

Its failure strategy is deliberately *different* from `ServicesGrid`: if the request fails, the
section renders nothing. A missing testimonial block is invisible, whereas a red error banner on
the homepage damages trust more than the absent reviews would. The services, by contrast, *are*
the page, so an error there must be surfaced.

**Error handling is a product decision, not one rule applied everywhere.**

Uses `<figure>/<blockquote>/<figcaption>`, the correct structure for a quotation with an
attribution.

### `src/components/home/HowItWorks.jsx` / `WhyUs.jsx` / `CtaBand.jsx`

Rendered from arrays in `constants.js` rather than repeated markup. The payoff is not fewer
characters — it is that a layout change happens once and the content is editable by someone who
does not read JSX.

`HowItWorks` uses `<ol>` because the steps are genuinely ordered: a screen reader announces
"list, 4 items, item 1 of 4", conveying the sequence the visual design implies.

### `src/pages/GetQuote.jsx`

The most involved screen, and the one to walk an interviewer through.

**1. One state object for the whole form.** Nineteen `useState` calls would mean nineteen
setters threaded into nineteen inputs, and every "reset" or "prefill" would touch all of them.
One generic `handleChange` reads `name`, `type` and `value` off the event, so each input only
needs a matching `name` attribute.

**2. `setForm((previous) => …)`, the functional form.** `setForm({...form, …})` reads a possibly
stale `form` from the closure and drops updates when two changes land in the same tick.

**3. The debounced live estimate.** `pricingInputs` is a JSON **string** of just the
price-relevant fields, so it can be a `useEffect` dependency — React compares dependencies by
reference, and a new object literal every render would re-trigger the effect endlessly.

The debounce itself is the effect's cleanup function: each change clears the pending timer and
starts a new 600 ms one, so the request fires only once typing stops. Without the cleanup, every
keystroke would simply queue another request. **This is the single most useful performance
pattern in form-heavy UIs.**

**4. Guards before calling.** The effect returns early for input the backend would reject
anyway, so the user does not see an error for a half-filled form.

**5. Type coercion at the boundary.** HTML inputs give strings; the API wants numbers, and
`null` (not `""`) for absent optional values — `""` fails `BigDecimal` parsing, while `null`
correctly means "no insurance".

**6. The price is never sent.** `bookingApi.create` posts the inputs; the server recalculates.

**7. `event.preventDefault()`** — without it the browser does a full page reload on submit and
all React state is lost.

**8. `finally { setSubmitting(false) }`** — re-enabled whether the call succeeded or failed.
Putting it only in the success path is how forms get stuck on "Submitting…" forever.

**9. Field errors from the backend are attached to the right inputs**, which is the payoff of
`GlobalExceptionHandler` flattening validation failures into a map.

**10. Layout:** `order-first lg:order-last` puts the price summary **above** the form on mobile,
so it is visible without scrolling past nineteen inputs, and beside it on desktop.

The clipboard call on the confirmation screen handles its failure path, because the Clipboard
API is async and can be blocked on an insecure origin or by a denied permission.

### `src/components/booking/QuoteSummary.jsx`

Pure presentational: data in, markup out. That makes it reusable and trivial to test.

The whole point is showing the **breakdown**, each line with the backend's own `explanation`
string. In an industry known for surprise charges at delivery, that transparency *is* the
product.

`tabular-nums` makes digits a fixed width so the amounts line up in a column instead of
wobbling; `whitespace-nowrap` stops a price wrapping mid-number.

### `src/components/booking/BookingTimeline.jsx`

Every step, including which are `reached` and which is `current`, comes from the backend.
Deriving it in React would duplicate the lifecycle order on the client, and the two would
disagree the day a status is added. The UI's only job is to paint what the server computed.

A cancelled booking gets its own single-step treatment, because showing "Delivered" as a future
step on a cancelled move is misleading.

### `src/pages/TrackBooking.jsx`

`useSearchParams` lets the confirmation page link to `/track?reference=…` and prefill the field.

**The phone number is deliberately not prefilled from the URL** — a phone number in a URL leaks
into browser history, server logs and any analytics script on the page.

The reference is upper-cased in the submit handler, because references are uppercase and nobody
types them that way; normalising in the UI avoids a pointless "not found" for a correct
reference.

### `src/pages/ServiceDetail.jsx`

`useParams()` reads the `:slug` segment. The key name must match the route exactly — a mismatch
gives you `undefined`, not an error, which is worth checking in both places when a detail page
404s.

**`[slug]` as the `useApiData` dependency is essential.** Navigating from
`/services/home-shifting` to `/services/office-relocation` keeps this component *mounted* (same
route, different param), so without the dependency the effect would not re-run and the page
would keep showing the previous service. A genuinely common SPA bug.

### `src/pages/Login.jsx` / `Register.jsx`

- **`login()` returns the profile**, and the code uses that return value rather than reading
  `user` from the context, which would still hold the *previous* value — state updates are not
  synchronous. One of the most common React misunderstandings.
- `autoComplete="email"` / `"current-password"` / `"new-password"` tell the browser's password
  manager what each field is, so saved credentials actually offer to fill them.
- `confirmPassword` is validated **only** on the client and never sent: it is a typo guard, not
  a business rule, and the server has no use for a second copy of the password.
- A 409 from the backend surfaces as "An account with this email already exists."
- The demo credentials card is intentional on a portfolio project — a reviewer who has to email
  you for a password closes the tab instead. A real product would never do this.

### `src/pages/Dashboard.jsx`

Never sends a user id anywhere: `GET /bookings/me` derives the owner from the JWT, so there is no
parameter to tamper with.

`totalValue` and `activeCount` are **derived at render**, not stored in state. Storing a total in
state means remembering to recompute it on every change — a classic source of stale UI.

`user?.fullName?.split(' ')[0] ?? 'there'` — optional chaining because `user` is null for the
single render before the profile arrives.

### `src/pages/Admin.jsx`

Tabs rather than five nested routes, with the tradeoff stated honestly: the sections are peers an
operator switches between constantly, nobody needs to deep-link to "the enquiries tab", and it
keeps the route table readable. **If a URL per tab were needed** (to share a link, or survive a
refresh on the right tab), nested routes with an `<Outlet/>` would be correct instead.

Each tab component fetches its own data, so opening the panel does not load five datasets — an
unvisited tab never mounts, so its request never fires.

`<ActiveComponent key={activeTab.id} />` — rendering a component from data keeps the file flat,
and the `key` forces a **remount** on tab change so React does not try to reuse one screen's
state for another.

`role="tablist"` + `role="tab"` + `aria-selected` is what makes this an accessible tab control
rather than five buttons that look like tabs.

### `src/components/admin/AdminOverview.jsx`

Every number is aggregated by MySQL. Loading every booking row to display one integer works fine
with 30 bookings and times out with 30,000.

`Object.entries(stats.bookingsByStatus)` turns the map into rows. The backend sends a
`LinkedHashMap` and JSON objects preserve insertion order, so the rows come out in lifecycle
order without the frontend knowing what that order is.

The bar chart is pure CSS — no charting library for one rectangle per row, which would be 100 kB
of JavaScript.

`stats.averageRating ? … : '—'` because the backend returns null with no approved reviews.

### `src/components/admin/AdminBookings.jsx`

**Filtering and paging happen in MySQL, not in JavaScript.** Fetching everything and calling
`.filter()` works with 30 rows, falls over at 30,000, and would send every customer's address to
the client just to display twenty.

- `status || undefined` so an empty filter is **omitted** from the query string rather than sent
  as `status=`, which the backend would try to parse as an enum.
- `page` resets to 0 when a filter changes, otherwise a user on page 4 lands on an empty page 4
  of the filtered set.
- Search runs **on submit**, not per keystroke — right for a table query against a database.
- After a mutation it calls `reload()` rather than patching the row locally: one extra request
  buys guaranteed consistency with the server, including anything another admin changed.
- The Update button **only renders when `allowedNextStatuses` is non-empty**, so the UI cannot
  offer an action the API would reject. A DELIVERED booking simply shows "Closed".
- The status dropdown is built from `allowedNextStatuses`, so the workflow lives in one place.
- A real `<table>` with `<thead>`/`<th>`: a screen reader announces "column 3 of 6, Status". A
  grid of divs conveys nothing. `overflow-x-auto` lets it scroll sideways on a phone rather than
  squashing columns into slivers.

### `src/components/admin/AdminEnquiries.jsx` / `AdminReviews.jsx`

**The `busyId` pattern:** a single id, not a boolean. With one shared `loading` flag, clicking
"Mark handled" on row 3 would put every button on the page into a spinner. Tracking *which* row
is busy keeps the feedback where the user clicked.

`whitespace-pre-line` preserves the line breaks the customer typed; without it the whole message
collapses into one paragraph, because HTML folds whitespace.

Every destructive action goes through a confirmation dialog, and the dialog **names what is about
to be deleted** — which is why `confirmDelete` holds the object rather than a boolean.

### `src/components/admin/AdminServices.jsx`

One form handles create and edit (`editing === null` means create), which is why the backend
exposes an "upsert" DTO. Two separate forms would be two places to add the next field.

### `src/pages/NotFound.jsx`

A good 404 says clearly that the page does not exist **and** offers the routes people most likely
wanted. A bare "404" with no navigation is a dead end that sends visitors back to Google.

Worth knowing: this renders only when the SPA router finds no match. For a **hard refresh** on an
unknown URL, the static host must first rewrite to `index.html` — otherwise the visitor gets the
host's own 404 and React never loads.

---

Next: [04-DEPLOYMENT.md](04-DEPLOYMENT.md)
