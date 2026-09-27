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

## 🛠 Tech stack

| Layer | Choice |
|---|---|
| Frontend | React 18 + Vite + React Router + Axios (plain CSS design system) |
| Backend | Spring Boot 3.2 (Java 17), Spring Security + JWT, JPA/Hibernate, Bean Validation |
| Database | **PostgreSQL 16** in Docker/prod · **H2 in-memory** for local dev (zero setup) |
| Deploy | Docker + Docker Compose (frontend nginx → backend → postgres) |

Palette follows strict **60-30-10**: warm off-white backgrounds · terracotta/orange primary · deep-red CTA accent.

## 📁 Structure

```
.
├── docker-compose.yml      # db + backend + frontend, one command
├── .env.example            # copy to .env (never commit secrets)
├── backend/                # Spring Boot REST API
│   ├── Dockerfile
│   └── src/main/java/com/canteen/
│       ├── controller/     # Auth, Menu, Order, User(profile/tabs/health)
│       ├── service/        # AuthService, OrderService (tab side-effect here)
│       ├── entity/         # User, MenuItem, CanteenOrder, OrderItem
│       ├── repository/     # Spring Data JPA
│       ├── security/       # JwtService, JwtAuthFilter, UserDetailsService
│       └── config/         # SecurityConfig (JWT+CORS), SeedData (admin+menu)
└── frontend/               # React (Vite)
    ├── Dockerfile + nginx.conf  # serves build, proxies /api -> backend
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

## 🤝 Contributing

1. Fork → feature branch (`feat/…` / `fix/…`) → PR with a clear description + screenshots for UI changes.
2. Keep `frontend/` and `backend/` concerns separate; add validation on both sides.
3. Never commit `.env` or secrets — update `.env.example` instead.
4. Comment non-obvious logic (the tab side-effect lives in `OrderService.updateStatus`).

## ⚠️ Notes / limits

- "Timeout → tab" is implemented as an explicit admin **UNCOLLECTED** action (a real cron auto-timeout is a natural next step — see `OrderRepository.findByStatusAndCreatedAtBefore`).
- Images are URL-based (no upload storage) to keep deployment simple.
