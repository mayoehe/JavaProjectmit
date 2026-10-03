# 🍛 Campus Canteen Pre-Order System

Pre-order canteen food, pay on pickup, and let uncollected orders fall onto a student **tab** automatically.
Two interfaces: **Student** (menu → cart → order → profile/tab) and **Admin** (menu CRUD, order flow, tabs, revenue dashboard).

## ✨ Features

**Student**
- Register/login with unique college **student ID** (roll / enrollment number) + JWT auth
- Browse today's menu (category filter), cart, place pre-orders
- Pay **in person** on pickup (no online payments)
- Uncollected orders auto-added to **tab balance**
- Profile page: info, tab, active orders, full history

**Admin (canteen staff)**
- Separate admin login, role-guarded APIs (`ROLE_ADMIN`)
- Menu CRUD: name, price, description, category, availability, optional image URL
- Orders: `PENDING → READY → COLLECTED`, or `UNCOLLECTED` (adds total to student tab once), `CANCELLED`
- Student tabs table: outstanding balances, mark paid / partial payment
- Dashboard: counts + collected revenue + tab outstanding
- Excel Data: export/import Users + Menu, one-click day-by-day **Sales Report** (`.xlsx`)

## 🛠 Tech stack

| Layer | Choice |
|---|---|
| Frontend | React 18 + Vite + React Router + Axios (plain CSS design system) |
| Backend | Spring Boot 3.2 (Java 17), Spring Security + JWT, JPA/Hibernate, Bean Validation |
| Database | **PostgreSQL 16** in Docker/prod · **H2 in-memory** for local dev (zero setup) |
| Deploy | Docker + Docker Compose (frontend nginx → backend → postgres) · Frontend also Vercel-ready (Hobby) |

Palette follows strict **60-30-10**: warm off-white backgrounds · terracotta/orange primary · deep-red CTA accent.

## 📁 Structure

```
.
├── docker-compose.yml      # db + backend + frontend, one command
├── .env.example            # copy to .env (never commit secrets)
├── backend/                # Spring Boot REST API
│   ├── Dockerfile
│   └── src/main/java/com/canteen/
│   ├── controller/     # Auth, Menu, Order, User(profile/tabs/health), AdminExcel(sales/export/import)
│       ├── service/        # AuthService, OrderService (tab side-effect here), AdminExcelService, SalesReportService
│       ├── entity/         # User, MenuItem, CanteenOrder, OrderItem
│       ├── repository/     # Spring Data JPA
│       ├── security/       # JwtService, JwtAuthFilter, UserDetailsService
│       └── config/         # SecurityConfig (JWT+CORS), SeedData (admin+menu)
└── frontend/               # React (Vite)
    ├── vercel.json + Dockerfile + nginx.conf  # Vercel (static) or nginx (/api -> backend)
    └── src/
        ├── pages/          # Landing, Auth, Ordering, Profile, Admin (the 4 required pages + auth)
        ├── components/     # Navbar, Protected route
        ├── context/AuthContext.jsx
        ├── api/client.js   # axios + JWT + 401 redirect
        └── styles/app.css  # 60-30-10 warm design system
```

## ✅ Prerequisites

- **Docker + Docker Compose** (recommended — only requirement for the fast path), *or*
- Local dev: **Java 17 + Maven** and **Node 20 + npm**

## 🚀 Run with Docker (recommended)

```bash
cp .env.example .env        # edit secrets if you like
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend:  http://localhost:8080 (`/api/health` → `{"status":"UP"}`)
- Postgres: localhost:5432

**Demo accounts** (seeded on first start):
- Admin: `admin` / `admin123` (override via `ADMIN_ID` / `ADMIN_PASSWORD` in `.env`)
- Students: click **Register** and create e.g. `2024CS101` / any password

## 💻 Run locally without Docker

Backend (H2 in-memory, http://localhost:8080):
```bash
cd backend
mvn spring-boot:run
# H2 console: http://localhost:8080/h2-console (JDBC URL jdbc:h2:mem:canteen, user sa)
```

Frontend (Vite dev, http://localhost:5173):
```bash
cd frontend
cp .env.example .env        # VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

To use local Postgres instead of H2:
```bash
cd backend
SPRING_PROFILES_ACTIVE=postgres SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/canteen \
SPRING_DATASOURCE_USERNAME=canteen SPRING_DATASOURCE_PASSWORD=secret mvn spring-boot:run
```

## ☁️ Deploy

**Option A — any VM with Docker (simplest, production-like):**
```bash
cp .env.example .env   # set strong POSTGRES_PASSWORD + JWT_SECRET + ADMIN_PASSWORD
docker compose up -d --build
```
Point your domain at the VM; frontend `:3000` is the public entrypoint (optionally front with Caddy/Nginx + TLS).

**Option B — Railway/Render-style split deploy:**
1. Create a managed **Postgres**; note its URL/user/password.
2. Deploy `backend/` as a Java service: build `mvn -DskipTests package`, run `java -jar target/*.jar` with env `SPRING_PROFILES_ACTIVE=postgres`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `JWT_SECRET`, `ADMIN_*`, and `app.cors-origins=https://<your-frontend-url>`.
3. Deploy `frontend/` as a static site: build `npm run build`, serve `dist/`; set `VITE_API_URL=https://<your-backend-url>` at build time.

No `localhost` is hardcoded in production builds — the frontend uses same-origin `/api` (nginx) or `VITE_API_URL` when set.

**Option C — Vercel Hobby (frontend) + free backend elsewhere (recommended split):**
> Spring Boot needs a long-running JVM, which Vercel Hobby (static + serverless Node) cannot run. Deploy the **frontend on Vercel**, the **backend on a free Java host** (Render / Railway / Fly.io / Koyeb), and managed/free Postgres (Neon / Supabase / Render Postgres).

1. **Backend first** (example: Render free Web Service):
   - Push this repo to GitHub. Create service from `backend/` (Docker or Java: build `mvn -DskipTests package`, run `java -jar target/*.jar`).
   - Env: `SPRING_PROFILES_ACTIVE=postgres`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (your Postgres), `JWT_SECRET` (32+ chars), `ADMIN_ID/ADMIN_PASSWORD`, `APP_CORS_ORIGINS=https://<your-frontend>.vercel.app`.
   - Note the public URL, e.g. `https://canteen-backend.onrender.com`. Check `/api/health` → `{"status":"UP"}`.
2. **Frontend on Vercel Hobby:**
   - Import the repo in Vercel. Either keep **Root Directory = repo root** (uses root `vercel.json`: build `cd frontend && npm install && npm run build`, output `frontend/dist`) or set **Root Directory = `frontend`** (uses `frontend/vercel.json`). Framework preset: **Vite**.
   - Env (Production + Preview): `VITE_API_URL=https://<your-backend>` (no trailing slash). Redeploy after changing it (Vite bakes it at build time).
   - SPA fallback is configured via `rewrites` → `/index.html`, so `/order`, `/admin`, `/login` deep links work.
3. **Verify:** open `https://<app>.vercel.app` → Register/Login → place order as student; login as admin → Dashboard, Excel Data, **Sales Report** export.

| Setting | Value |
|---|---|
| Root Directory | repo root (or `frontend`) |
| Framework | Vite |
| Build | `cd frontend && npm install && npm run build` (root) or `npm run build` (`frontend` root) |
| Output | `frontend/dist` (root) or `dist` (`frontend` root) |
| Env | `VITE_API_URL=https://<backend>` |

Hobby limits to know: static frontend fits easily; serverless timeout/bandwidth apply only if you add functions (this app uses none). Keep secrets in Vercel env vars, never in code — see `frontend/.env.example` and root `.env.example` (`VITE_API_URL`, `APP_CORS_ORIGINS`).

## 🔌 API overview

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | `{studentId, name, email?, password}` → `{token, …}` |
| POST | `/api/auth/login` | public | `{studentId, password}` → `{token, role, …}` |
| GET | `/api/menu` | auth | Today's available items (`?all=true` for admin incl. hidden) |
| POST/PUT/PATCH/DELETE | `/api/menu…` | admin | Menu CRUD + `/{id}/availability` |
| POST | `/api/orders` | student | `{items:[{menuItemId, quantity}]}` → order (pay on pickup) |
| GET | `/api/orders/my` | student | Own orders (active + history) |
| GET | `/api/orders` | admin | All orders, newest first |
| PATCH | `/api/orders/{id}/status` | admin | `{"status":"READY" \| "COLLECTED" \| "UNCOLLECTED" \| …}` |
| GET | `/api/orders/summary` | admin | Counts + collected revenue + tab outstanding |
| GET | `/api/users/me` | auth | Profile incl. `tabBalance` |
| GET | `/api/students`, `/api/students/tabs` | admin | All students / only debtors |
| PATCH | `/api/students/{id}/tab` | admin | `{}` = clear, `{"amount":50}` = partial payment |
| GET | `/api/health` | public | Liveness probe |

Auth: `Authorization: Bearer <jwt>`.

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/admin/export/users` | admin | Download `canteen-users.xlsx` (single sheet `Users`) |
| GET | `/api/admin/export/menu` | admin | Download `canteen-menu.xlsx` (single sheet `MenuItems`) |
| GET | `/api/admin/export/all` | admin | Download `canteen-export.xlsx` (2 sheets: `Users` + `MenuItems`, recommended) |
| GET | `/api/admin/export/sales-daily` | admin | Download `canteen-sales-daily.xlsx` (sheets `DailySales` + `ItemBreakdown`; `?from=yyyy-MM-dd&to=yyyy-MM-dd` optional) |
| POST | `/api/admin/import/users` | admin | Upload `.xlsx` (`multipart file`) → `{created, updated, skipped, errors[]}` |
| POST | `/api/admin/import/menu` | admin | Upload `.xlsx` (`multipart file`) → `{created, updated, skipped, errors[]}` |

## 🗄️ Admin Excel database workflow (export → edit → import)

Database: relational (H2 in-memory for local dev, PostgreSQL 16 in Docker/prod via `SPRING_PROFILES_ACTIVE=postgres`). Tables: `users`, `menu_items`, `orders`, `order_items` (JPA `ddl-auto:update`, indexed, constrained).

- `users`: `id`, `studentId` unique (roll number; `admin` for staff), `name`, `email`, `passwordHash` (BCrypt, never exported), `role` (`STUDENT|ADMIN`), `tabBalance >= 0`, `status` (`ACTIVE|DISABLED`), `createdAt`, `updatedAt`. Disabled users get `403` on login.
- `menu_items`: `id`, `name`, `price > 0`, `description`, `category`, `available`, `imageUrl?`, `menuDate?` (`yyyy-MM-dd`, null = general/today), `createdAt`, `updatedAt`.

### How to export
1. Login as admin (`admin` / `admin123`), open **Canteen Dashboard → 🗄️ Excel Data**.
2. Click **Export All (.xlsx)** (preferred), or **Export Users** / **Export Menu** separately.
3. Or via API: `GET /api/admin/export/all` with `Authorization: Bearer <admin-token>` (response is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).

### How to edit safely
- Do NOT rename sheets or the header row. Keep `ID` column as-is (empty `ID` = new record).
- **Users sheet** (`Users`): `ID | StudentID/Username | Name | Email | Role | TabBalance | Status | CreatedAt | UpdatedAt | NewPassword (optional - blank keeps existing)`
  - `StudentID/Username*`: required, unique. `Name*`: required for new rows (blank on update = keep existing).
  - `Email`: optional, must contain `@` if filled. `Role`: blank = keep, else `STUDENT|ADMIN`.
  - `TabBalance`: blank = keep, else number `>= 0`. `Status`: blank = keep, else `ACTIVE|DISABLED`.
  - `CreatedAt/UpdatedAt`: read-only, ignored on import. **Password hashes are never exported.**
  - `NewPassword`: blank = keep old password. Filled (`>= 4` chars) = BCrypt-hashed and updated. Required for new users. The last `ADMIN` cannot be demoted/disabled.
- **Menu sheet** (`MenuItems`): `ID | Name | Description | Price | Category | Available | ImageURL | MenuDate (yyyy-MM-dd) | CreatedAt | UpdatedAt`
  - `Name*`, `Price* (> 0)` required for new rows; blank on update = keep existing.
  - `Category`: blank = keep (`General` for new). `Available`: `TRUE/FALSE` (also `Yes/No`, `1/0`, `Available/Hidden`).
  - `ImageURL`: optional. `MenuDate`: `yyyy-MM-dd` or blank (blank = keep on update, `null` on create).
  - `CreatedAt/UpdatedAt`: ignored on import.
- Delete nothing structurally; to add a user/dish, append a row with empty `ID`. Unchanged rows are auto-skipped (`skipped` count).

### How to re-upload
1. In **🗄️ Excel Data → 📥 Import**, choose the edited `.xlsx` for Users/Menu (only `.xlsx`/`.xlsm` accepted).
2. Result shows `created, updated, skipped` + per-row `errors` (e.g. `Row 5: Price must be positive`); valid rows still apply even if others fail.
3. Dashboard auto-refreshes. Or via API: `POST /api/admin/import/users` with `multipart/form-data file=@canteen-users.xlsx`.

## 📊 Day-by-Day Sales Excel export (Admin only)
1. Login as admin, open **Canteen Dashboard → 🗄️ Excel Data → 📊 Sales Report**.
2. Optionally pick **From/To** dates (inclusive, `yyyy-MM-dd`), then click **⬇ Export Sales (.xlsx)**. Leave blank for all-time.
3. Or via API: `GET /api/admin/export/sales-daily?from=2026-09-01&to=2026-09-30` with `Authorization: Bearer <admin-token>`.
- **Sheet `DailySales`:** `Date | Orders | Items Sold | Total Revenue | Collected | Tab/UNCOLLECTED | Open Pipeline (PENDING+READY) | Pending | Ready | Collected | Uncollected | Cancelled | Unique Students | Avg Order` + bold `TOTAL` row.
- **Sheet `ItemBreakdown`:** `Date | Item | Category | Qty Sold | Revenue` (one row per dish per day).
- Cancelled orders are counted in `Orders`/`Cancelled` but excluded from revenue, items, and breakdown. Dates use the server time zone. Students get `403` on this endpoint.

## 🤝 Contributing

1. Fork → feature branch (`feat/…` / `fix/…`) → PR with a clear description + screenshots for UI changes.
2. Keep `frontend/` and `backend/` concerns separate; add validation on both sides.
3. Never commit `.env` or secrets — update `.env.example` instead.
4. Comment non-obvious logic (the tab side-effect lives in `OrderService.updateStatus`).

## ⚠️ Notes / limits

- "Timeout → tab" is implemented as an explicit admin **UNCOLLECTED** action (a real cron auto-timeout is a natural next step — see `OrderRepository.findByStatusAndCreatedAtBefore`).
- Images are URL-based (no upload storage) to keep deployment simple.
