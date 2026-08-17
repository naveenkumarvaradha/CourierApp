# ShipDesk — Courier Booking System

Internal courier booking and tracking system for **CTL India Private Limited**.

Runs fully locally — no external/production infrastructure required.

---

## Tech Stack

### Backend
| Layer | Technology | Version |
|---|---|---|
| Language | Java | 25.0.3 |
| Framework | Spring Boot | 3.5.3 |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| Database | PostgreSQL | 14+ |
| ORM | Spring Data JPA + Hibernate | via Spring Boot 3.5.3 |
| Migrations | Flyway | via Spring Boot |
| Cache | Redis + Spring Cache (Jedis) | 5+ |
| PDF Generation | OpenPDF | 1.3.39 |
| Excel Export | Apache POI | 5.3.0 |
| Barcode | ZXing | 3.5.3 |
| Mapping | MapStruct | 1.6.2 |
| Boilerplate | Lombok | 1.18.46 |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.6.0 |
| Messaging | Apache Kafka (disabled by default) | via Spring Boot |
| Build | Maven | 3.x |

### Frontend
| Layer | Technology | Version |
|---|---|---|
| Language | TypeScript | 5.6.3 |
| Framework | React | 18.3.1 |
| Build Tool | Vite | 5.4.11 |
| UI Library | MUI (Material UI) | 6.1.6 |
| Data Grid | MUI X Data Grid | 7.22.2 |
| State Management | Redux Toolkit + RTK Query | 2.12.0 |
| Routing | React Router DOM | 6.28.0 |
| Forms | React Hook Form | 7.53.2 |
| HTTP Client | Axios | 1.7.7 |
| Date Utils | Day.js | 1.11.13 |
| CSS | Tailwind CSS + MUI Emotion | 3.4.19 |

---

## Project Structure

```
CourierApp/
├── backend/                            # Spring Boot application
│   ├── src/main/java/com/courierapp/
│   │   ├── controller/                 # REST endpoints
│   │   ├── service/                    # Business logic
│   │   │   └── impl/
│   │   ├── entity/                     # JPA entities
│   │   ├── repository/                 # Spring Data JPA
│   │   ├── dto/                        # Request / Response DTOs
│   │   │   ├── admin/                  # incl. Unit
│   │   │   ├── auth/
│   │   │   ├── booking/
│   │   │   ├── dc/                     # Delivery Challan
│   │   │   ├── dcreceipt/              # DC Receipt
│   │   │   └── master/
│   │   ├── security/                   # JWT filter, AppUserPrincipal
│   │   ├── kafka/                      # Event producer/consumer (disabled)
│   │   └── config/                     # Security, Redis, Cache, CORS
│   └── src/main/resources/
│       ├── application.yml             # Config (env vars override at runtime)
│       └── db/migration/               # Flyway SQL migrations V1–V41
├── frontend/                           # React + TypeScript SPA
│   ├── src/
│   │   ├── api/                        # Axios client + API endpoints
│   │   ├── context/                    # AuthContext, NotificationContext
│   │   ├── pages/                      # Pages by module
│   │   │   ├── admin/                  # incl. Units, Admin hub
│   │   │   ├── auth/
│   │   │   ├── booking/                # Courier Booking
│   │   │   ├── dc/                     # DC Booking + DC Receipt
│   │   │   └── parties/
│   │   ├── store/                      # Redux + RTK Query slices
│   │   ├── types/                      # TypeScript interfaces
│   │   └── App.tsx                     # Route definitions
│   └── dist/                           # Build output (npm run build)
├── README.md                           # This file
└── docs/
    ├── INSTALLATION_GUIDE.md           # Fresh install guide
    └── DEV_ENVIRONMENT.md              # Installed toolchain & config reference
```

---

## Features

| Module | Description |
|---|---|
| **Auth** | JWT login, refresh tokens, password reset, account lockout |
| **Bookings** | Create, submit, approve, track courier bookings (multi-level approval) |
| **Parties** | Sender/receiver address book with approval workflow |
| **Units** | Company branch/office addresses; selectable as booking sender and DC sender/receiver |
| **DC Booking** | Standalone Delivery Challan module (own shipment fields, sender = Unit, receiver = Party or Unit), Returnable/Non-Returnable type, multi-level approval, Draft → Pending Approval → Approved → Issued → Delivered |
| **DC Receipt** | Confirms return of Returnable DCs once Issued/Delivered; moves DC to terminal `Returned` status; single-step confirm with undo |
| **Admin** | Users, roles, permissions, departments, courier ways, package types, units |
| **Company Settings** | Address, logo, SMTP config per company |
| **Reports** | Booking summary, Excel/PDF export, scheduled email reports |
| **Flex Fields** | Configurable custom fields per module |
| **Sticker Print** | 4×6" PDF shipping label with Code128 barcode |
| **Redis Cache** | Caches lookups (departments, courier ways, company settings) — TTL 5 min |
| **Kafka** | Event scaffolding (disabled by default — `KAFKA_ENABLED=false`) |

---

## Architecture (local)

```
Browser
  ├── http://localhost:5173  → Vite dev server (React SPA)
  └── http://localhost:8080/api → Spring Boot
                                    ├── PostgreSQL (localhost:5432)
                                    └── Redis (localhost:6379)
```

### Auth Flow
```
POST /api/auth/login    → accessToken (15 min) + refreshToken (7 days)
POST /api/auth/refresh  → new accessToken
POST /api/auth/logout   → token blacklisted in Redis
```

> MFA (TOTP) was implemented at one point (see V28–V32 migrations) but was fully removed in
> V33 — the `mfa_secret`/`mfa_enabled`/`mfa_forced` columns are dropped and no MFA code
> remains in the backend or frontend. Login only ever returns a token pair, never an MFA
> challenge.

---

## Local Development

See [docs/INSTALLATION_GUIDE.md](docs/INSTALLATION_GUIDE.md) for full toolchain setup, and [docs/DEV_ENVIRONMENT.md](docs/DEV_ENVIRONMENT.md) for what's installed on this machine specifically.

### Prerequisites
- JDK 25+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+
- Redis 5+

### Database (first time only)

```bash
psql -U postgres
```
```sql
CREATE USER courier_app WITH PASSWORD 'Courier@123';
CREATE DATABASE courierdb OWNER courier_app;
```

### Backend

```bash
cd backend

export DB_USERNAME=courier_app
export DB_PASSWORD=Courier@123
export JWT_SECRET=replace-with-a-long-random-string-at-least-64-characters-long
export REDIS_HOST=localhost
export REDIS_PORT=6379
export CORS_ORIGINS=http://localhost:5173

mvn spring-boot:run
# API: http://localhost:8080/api
# Swagger: http://localhost:8080/api/swagger-ui.html
```

### Frontend

```bash
cd frontend
npm install

npm run dev
# http://localhost:5173
# Calls backend at http://localhost:8080/api (auto-detected)

npm run build
# Output: frontend/dist/
```

### Default login

Flyway seeds one admin account on first startup (`V2__seed.sql`):

| Field | Value |
|---|---|
| Company | `My Company` |
| Username | `admin` |
| Password | `Admin@123` |

Change this password after first login.

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `courier_app` |
| `DB_PASSWORD` | PostgreSQL password | `Courier@123` |
| `JWT_SECRET` | JWT signing key — **min 64 chars** (HS512 requires a key ≥ 512 bits; shorter keys throw `WeakKeyException` at login) | insecure default |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | `` (blank) |
| `CORS_ORIGINS` | Allowed origins (comma-separated) | `http://localhost:5173` |
| `FRONTEND_URL` | Base URL for email links | `http://localhost:5173` |
| `KAFKA_ENABLED` | Enable Kafka publishing | `false` |
| `MAIL_HOST` | SMTP host | `smtp.office365.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | — |
| `MAIL_PASSWORD` | SMTP password | — |

---

## Database Migrations

Flyway runs automatically on startup. Files in `backend/src/main/resources/db/migration/`.

| File | Description |
|---|---|
| V1–V10 | Core schema: users, roles, permissions, companies, bookings, parties |
| V11 | Add `company_id` to users |
| V22 | Add `company_id` to parties |
| V28–V32 | MFA (TOTP) implemented, then reset/reworked |
| V31 | Performance indexes |
| V33 | MFA fully removed — `mfa_secret`/`mfa_enabled`/`mfa_forced` columns dropped |
| V34 | Company `units` table + backfill from company settings |
| V35 | Add `unit_id` to bookings |
| V36 | Delivery Challan module: `delivery_challans`, `dc_sequence`, permissions |
| V37 | DC approval workflow columns + `DELIVERY_CHALLAN_APPROVE` + routing seed |
| V38 | DC redesigned as standalone module (drop booking FK, add shipment fields) |
| V39 | Add `dc_type` (Returnable / Non-Returnable) |
| V40 | DC Receipt module: `dc_receipts`, `dc_receipt_sequence`, permissions |
| V41 | Grant DC/Receipt permissions to `BOOKING_CREATOR` role |

> **Rule:** Never edit existing migration files. Always create `V{n+1}__description.sql`.

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Login fails with "Invalid username or password" | Wrong credentials, or DB wasn't seeded | Use `admin` / `Admin@123` / company `My Company`; confirm `V2__seed.sql` ran (`SELECT * FROM flyway_schema_history`) |
| Departments / lookups show empty | Stale Redis cache | `redis-cli -a <pwd> FLUSHDB` |
| Backend returns 503 on startup | Mail health check failing | Set `-Dmanagement.health.mail.enabled=false` on startup |
| Redis AUTH error | Password mismatch | Match `REDIS_PASSWORD` to your Redis config |
| Flyway migration fails on startup | Column already exists / wrong version | Check migration history table, never edit existing V*.sql |
| JAR build fails (file locked) | Running Java process holds the jar | Stop the running backend before `mvn package` |
| Company not showing in New Booking | Non-admin user — old endpoint was ADMIN_VIEW only | Fixed in current version via `/api/bookings/my-company-settings` |
