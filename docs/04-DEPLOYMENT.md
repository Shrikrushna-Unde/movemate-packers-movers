# 04 — Deployment

Getting this live on a free tier, so you can put a working URL on your resume.

**Recommended split:** backend + MySQL on **Railway**, frontend on **Vercel**.

> **Why not Render for everything?** Render does not offer managed MySQL — only PostgreSQL.
> Since this project uses MySQL, Railway is the path of least resistance. Options B and C at the
> bottom cover Render if you prefer it.

---

## 0. Before you start

### Push the code to GitHub

Both platforms deploy from a Git repository.

```bash
cd "C:/Users/patel/Documents/Birthday web/packers-movers"
git init
git add .
git commit -m "MoveMate packers and movers platform"
git branch -M main
git remote add origin https://github.com/<your-username>/movemate-packers-movers.git
git push -u origin main
```

Check before pushing that `.gitignore` is doing its job:

```bash
git status --short | Select-String ".env"
```

That must print **nothing**. `.env`, `target/` and `node_modules/` are all ignored.

### Generate a JWT secret

HS256 needs at least 32 bytes. Generate one and keep it somewhere safe — you will paste it into
your hosting dashboard, not into a file:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
```

No Node yet? PowerShell works too. Note `RandomNumberGenerator`, not `Get-Random`: the latter
is a deterministic PRNG seeded from the system clock, so a signing key made with it is
guessable by anyone who knows roughly when you generated it.

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b).Replace("+","-").Replace("/","_").TrimEnd("=")
```

---

## 1. Backend + database on Railway

### 1.1 Create the project and the database

1. Sign in at <https://railway.app> with GitHub.
2. **New Project → Deploy MySQL**. Railway provisions MySQL 8 in about thirty seconds.
3. Open the MySQL service → **Variables**. Note these (Railway names them itself):
   `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`.

### 1.2 Deploy the API

1. In the same project: **New → GitHub Repo** → pick your repository.
2. Open the new service → **Settings**:
   - **Root Directory**: `backend`
   - **Builder**: Dockerfile (Railway detects `backend/Dockerfile` automatically)
3. **Settings → Networking → Generate Domain.** That gives you
   `https://movemate-api-production-xxxx.up.railway.app`.

### 1.3 Environment variables

Open the API service → **Variables** → **Raw Editor**, and paste:

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
DB_POOL_SIZE=5
JWT_SECRET=<paste the secret you generated>
JWT_EXPIRATION_MINUTES=1440
ADMIN_EMAIL=admin@movemate.in
ADMIN_PASSWORD=<pick a strong password>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

`${{MySQL.MYSQLHOST}}` is Railway's **variable reference** syntax: it reads the value from the
service named `MySQL` in the same project. The benefit is that rotating the database password
does not require editing the API's variables — and the credentials never appear in your
repository.

`CORS_ALLOWED_ORIGINS` is a placeholder for now; you will come back and set the real frontend
URL in step 3.

### 1.4 Verify

Railway redeploys on every variable change. When the deploy is green:

```bash
curl https://<your-api-domain>/actuator/health
```

Expect `{"status":"UP"}`. Then check that Flyway and the seeder ran:

```bash
curl https://<your-api-domain>/api/v1/catalog/services
```

Expect six services. And open `https://<your-api-domain>/swagger-ui.html` in a browser.

**If the deploy fails, read the logs first.** The usual causes:

| Log says | Cause | Fix |
|---|---|---|
| `Communications link failure` | Wrong `DB_URL` | Check the variable references resolved — Railway shows the computed value |
| `Unable to obtain connection` on a private host | Using `MYSQLHOST` from outside the project network | Use the variable reference form above, which resolves to the internal host |
| `Flyway ... found non-empty schema without schema history` | Tables created some other way | `spring.flyway.baseline-on-migrate=true` is already set; if it persists, drop the schema and redeploy |
| `WeakKeyException` | `JWT_SECRET` shorter than 32 bytes | Generate a longer one |
| Killed / OOM | Heap too large for the container | Already handled by `-XX:MaxRAMPercentage=75` in the Dockerfile |

---

## 2. Frontend on Vercel

1. Sign in at <https://vercel.com> with GitHub and **Add New → Project** → your repository.
2. Configure:
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite (detected)
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
3. **Environment Variables** — add three:

   ```
   VITE_API_BASE_URL  = https://<your-api-domain>/api/v1
   VITE_CONTACT_PHONE = +91XXXXXXXXXX
   VITE_CONTACT_EMAIL = hello@movemate.in
   ```

   Note the `/api/v1` suffix: `api/client.js` uses it as the axios `baseURL`.

   `VITE_CONTACT_PHONE` drives the navbar, the footer, the `tel:` links and the WhatsApp
   click-to-chat URL. Give it the full international form; `utils/constants.js` strips the
   `+` and spaces for `wa.me`, and assumes `+91` if you supply only 10 digits.

4. **Deploy.** You get `https://movemate-web-xxxx.vercel.app`.

`frontend/vercel.json` already supplies the SPA rewrite, so a refresh on `/services` works.

> **Remember:** Vite inlines `VITE_*` variables at **build** time. Changing one requires a
> **redeploy**, not a restart. This catches everyone once.

---

## 3. Close the CORS loop

Go back to Railway → API service → **Variables** and set the real frontend URL:

```
CORS_ALLOWED_ORIGINS=https://movemate-web-xxxx.vercel.app
```

Railway redeploys. Now the browser is allowed to call the API.

**No trailing slash**, and the scheme must match exactly. `https://x.vercel.app/` and
`http://x.vercel.app` are both different origins from `https://x.vercel.app`, and CORS compares
them as strings. This is the single most common deployment mistake on a split frontend/backend
app.

To allow Vercel's preview deployments too, add them comma-separated:

```
CORS_ALLOWED_ORIGINS=https://movemate-web-xxxx.vercel.app,http://localhost:5173
```

---

## 4. Smoke test the live site

Work through this list before putting the URL on your resume:

- [ ] Homepage loads, six service cards appear (proves the API and database are reachable)
- [ ] Testimonials section shows reviews (proves the seed migration ran)
- [ ] `/quote`: change home size and distance — the estimate updates after a short pause
- [ ] Submit a booking → you get a reference like `MM-2026-XXXXXX`
- [ ] `/track` with that reference + the phone you used → the timeline renders
- [ ] `/track` with the same reference and a **wrong** phone → "could not find that booking"
- [ ] Register a new account → you land on the dashboard already signed in
- [ ] Sign in as admin → the Admin link appears in the navbar
- [ ] Admin → Bookings: your test booking is listed; move it Pending → Confirmed
- [ ] Try to reach `/admin` while signed in as the customer → redirected to `/dashboard`
- [ ] Hard-refresh on `/services` → the page loads (proves the SPA rewrite works)
- [ ] `/swagger-ui.html` opens and the **Authorize** button accepts a token
- [ ] Open devtools → Network: no CORS errors in the console

### A useful end-to-end check from the terminal

```bash
# 1. Log in and capture the token
TOKEN=$(curl -s -X POST https://<api>/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@movemate.in","password":"<your admin password>"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 2. Call a protected endpoint with it
curl -s https://<api>/api/v1/admin/stats -H "Authorization: Bearer $TOKEN"

# 3. Confirm it is actually protected
curl -s -o /dev/null -w "%{http_code}\n" https://<api>/api/v1/admin/stats
# expect 401
```

That last line is the one worth screenshotting: it demonstrates the endpoint is genuinely
protected, not just hidden in the UI.

---

## 5. Free-tier realities

Be aware of these, and mention them if asked rather than being surprised:

| Platform | Limitation | Mitigation |
|---|---|---|
| Railway | Trial credit, then a small monthly spend | Keep one API + one MySQL; the free credit covers a portfolio project |
| Render free web service | **Sleeps after 15 minutes idle**, ~50 s cold start | Warn reviewers, or ping `/actuator/health` on a schedule |
| Vercel / Netlify | Static hosting is genuinely free, no sleeping | — |
| Free MySQL hosts | Low connection caps | `DB_POOL_SIZE=5` is already set for this |

**A cold start on the first visit makes a reviewer think the site is broken.** If you use a
sleeping host, either keep it warm or put a line on your resume: *"hosted on a free tier — the
first request may take up to a minute to wake the server."* Honest and professional.

---

## Option B — Backend on Render + MySQL elsewhere

`render.yaml` in the repository root is a Render Blueprint that provisions both services. You
still need MySQL from somewhere else:

- **Railway MySQL** (you can use just the database from a Railway project)
- **Aiven** — free MySQL plan
- **Clever Cloud** — small free MySQL add-on

Then in Render set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` to point at it. **Make sure the URL
includes `useSSL=true`** when the database is on a different provider, because the traffic
crosses the public internet.

Render reads `healthCheckPath: /actuator/health` from the blueprint and marks a deploy failed if
it never returns 200 — which is exactly why Actuator is a dependency.

## Option C — Switch to PostgreSQL

If you would rather use Render end to end (its free Postgres is genuinely convenient), the
migration is small:

1. `pom.xml`: replace `mysql-connector-j` with `org.postgresql:postgresql`, and `flyway-mysql`
   with `flyway-database-postgresql`.
2. `V1__init_schema.sql`: `BIGINT AUTO_INCREMENT` → `BIGSERIAL`, drop
   `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`, `DATETIME(6)` → `TIMESTAMP(6)`, `BOOLEAN` is already
   native.
3. `V2__seed_catalog.sql`: `NOW(6)` → `NOW()`.
4. `DB_URL` → `jdbc:postgresql://host:5432/movemate`.

Everything else — entities, repositories, services, the whole frontend — is unchanged, which is
itself a good advertisement for JPA and for keeping vendor-specific SQL confined to migrations.

---

## 6. Updating a deployment

Both platforms watch your Git branch, so:

```bash
git add .
git commit -m "Describe the change"
git push
```

Railway rebuilds the Docker image and redeploys; Vercel rebuilds the static bundle. A new Flyway
migration (`V3__…`) runs automatically on the next backend start.

**One caution:** never edit a migration that has already run in production. Flyway stores a
checksum and will refuse to start, reporting a validation failure. Always add a new numbered
file.

---

Next: [05-INTERVIEW-QA.md](05-INTERVIEW-QA.md)
