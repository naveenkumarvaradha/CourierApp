# ShipDesk — Enterprise Courier Booking System

Internal courier booking and tracking system for **CTL India Private Limited**.

---

## Tech Stack

### Backend
| Layer | Technology | Version |
|---|---|---|
| Language | Java | 25.0.3 |
| Framework | Spring Boot | 3.3.4 |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| Database | PostgreSQL | 18 (prod) |
| ORM | Spring Data JPA + Hibernate | via Spring Boot 3.3.4 |
| Migrations | Flyway | via Spring Boot |
| Cache | Redis + Spring Cache (Jedis) | 5.0.14 (prod) |
| MFA (2FA) | TOTP — Google Authenticator | totp-spring-boot-starter 1.7.1 |
| PDF Generation | OpenPDF | 1.3.39 |
| Excel Export | Apache POI | 5.3.0 |
| Barcode | ZXing | 3.5.3 |
| Mapping | MapStruct | 1.6.2 |
| Boilerplate | Lombok | 1.18.46 |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.6.0 |
| Messaging | Apache Kafka (disabled in prod) | via Spring Boot |
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

### Infrastructure (Production — TKL-SRV-COURIER)
| Component | Technology | Details |
|---|---|---|
| Server | Windows Server | 192.168.1.15 |
| Java Runtime | JDK 25.0.3 | `C:\Program Files\Java\jdk-25.0.3` |
| Web / Reverse Proxy | Nginx | 1.30.3 — port 8088 |
| Backend Service | WinSW (Windows Service) | ShipDeskBackend — port 8080 |
| Database | PostgreSQL 18 | port 5432, db: `courierdb` |
| Cache | Redis 5.0.14 | port 6379 |
| Firewall / NAT | FortiGate | VIP: `103.102.97.163:5173` → `192.168.1.15:8088` |
| Nginx Auto-start | Windows Scheduled Task | NginxAutoStart (AtStartup, SYSTEM) |

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
│   │   │   ├── admin/
│   │   │   ├── auth/
│   │   │   ├── booking/
│   │   │   └── master/
│   │   ├── security/                   # JWT filter, AppUserPrincipal
│   │   ├── kafka/                      # Event producer/consumer (disabled)
│   │   └── config/                     # Security, Redis, Cache, CORS
│   └── src/main/resources/
│       ├── application.yml             # Config (env vars override at runtime)
│       └── db/migration/               # Flyway SQL migrations V1–V32
├── frontend/                           # React + TypeScript SPA
│   ├── src/
│   │   ├── api/                        # Axios client + API endpoints
│   │   ├── context/                    # AuthContext, NotificationContext
│   │   ├── pages/                      # Pages by module
│   │   │   ├── admin/
│   │   │   ├── auth/
│   │   │   ├── booking/
│   │   │   └── parties/
│   │   ├── store/                      # Redux + RTK Query slices
│   │   ├── types/                      # TypeScript interfaces
│   │   └── App.tsx                     # Route definitions
│   ├── .env.production                 # VITE_API_BASE_URL=/api
│   └── dist/                           # Production build output
├── README.md                           # This file
├── DEPLOYMENT_GUIDE.md                 # Step-by-step prod deploy
└── INSTALLATION_GUIDE.md               # Fresh install guide
```

---

## Features

| Module | Description |
|---|---|
| **Auth** | JWT login, refresh tokens, MFA (TOTP), password reset, account lockout |
| **MFA Management** | Admin can force / disable / reset MFA per user |
| **Bookings** | Create, submit, approve, track courier bookings (multi-level approval) |
| **Parties** | Sender/receiver address book with approval workflow |
| **Admin** | Users, roles, permissions, departments, courier ways, package types |
| **Company Settings** | Address, logo, SMTP config per company |
| **Reports** | Booking summary, Excel/PDF export, scheduled email reports |
| **Flex Fields** | Configurable custom fields per module |
| **Sticker Print** | 4×6" PDF shipping label with Code128 barcode |
| **Redis Cache** | Caches lookups (departments, courier ways, company settings) — TTL 5 min |
| **Kafka** | Event scaffolding (disabled in prod — `KAFKA_ENABLED=false`) |

---

## Architecture

```
Browser
  └── Nginx (port 8088)
        ├── /         → serves frontend/dist/  (React SPA)
        └── /api/     → proxy → Spring Boot (port 8080)
                                    ├── PostgreSQL (port 5432)
                                    └── Redis (port 6379)
```

### Auth Flow
```
POST /api/auth/login
  → password OK + MFA enabled  → mfaRequired=true  + mfaPendingToken
  → password OK + MFA forced   → mfaSetupRequired=true + accessToken → redirect to /profile/mfa
  → password OK, no MFA        → accessToken (15 min) + refreshToken (7 days)

POST /api/auth/refresh  → new accessToken
POST /api/auth/logout   → token blacklisted in Redis
```

---

## Local Development

### Prerequisites
- JDK 25+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+
- Redis 5+

### Backend

```bash
cd backend

# Windows — set env vars in terminal or IDE run config
set DB_USERNAME=courier_app
set DB_PASSWORD=your_password
set JWT_SECRET=your_secret_min_32_chars
set REDIS_HOST=localhost
set REDIS_PORT=6379

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

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `courier_app` |
| `DB_PASSWORD` | PostgreSQL password | `Courier@123` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | insecure default |
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

## Production Deployment

### 1. Build

```bash
# Backend
cd backend
mvn package -DskipTests
# → backend/target/courier-booking-backend-1.0.0.jar

# Frontend
cd frontend
npm run build
# → frontend/dist/
```

### 2. Copy via WinSCP

| Local | Prod (192.168.1.15) |
|---|---|
| `backend/target/courier-booking-backend-1.0.0.jar` | `D:\ShipDesk\backend\courier-booking-backend-1.0.0.jar` |
| `frontend/dist/` (all contents) | `D:\ShipDesk\frontend\dist\` |

### 3. Restart on Prod

```powershell
Restart-Service ShipDeskBackend
# Nginx restarts automatically on reboot via NginxAutoStart scheduled task
# To restart Nginx manually:
cd D:\nginx && .\nginx.exe -s reload
```

### Prod Services Status

| Service | Type | Port | Auto-start |
|---|---|---|---|
| `ShipDeskBackend` | Windows Service (WinSW) | 8080 | Automatic |
| `Redis` | Windows Service | 6379 | Automatic |
| `NginxAutoStart` | Scheduled Task | 8088 | AtStartup (SYSTEM) |
| `ShipDeskFrontend` | Windows Service | — | **Disabled** (replaced by Nginx) |

### Prod Config Files

| File | Purpose |
|---|---|
| `D:\ShipDesk\start-backend.bat` | Env vars + JVM flags for Spring Boot |
| `D:\ShipDesk\ShipDeskBackend.xml` | WinSW service definition |
| `D:\nginx\conf\nginx.conf` | Nginx reverse proxy config |
| `D:\ShipDesk\logs\backend.log` | Application logs |
| `C:\Program Files\Redis\redis.windows.conf` | Redis config (password) |

### Access URLs

| Network | URL |
|---|---|
| LAN | `http://192.168.1.15:8088` |
| External | `http://103.102.97.163:5173` → FortiGate → `192.168.1.15:8088` |

---

## Database Migrations

Flyway runs automatically on startup. Files in `backend/src/main/resources/db/migration/`.

| File | Description |
|---|---|
| V1–V10 | Core schema: users, roles, permissions, companies, bookings, parties |
| V11 | Add `company_id` to users |
| V22 | Add `company_id` to parties |
| V30 | Disable all MFA (reset during migration) |
| V31 | Performance indexes |
| V32 | Add `mfa_forced` column to users |

> **Rule:** Never edit existing migration files. Always create `V{n+1}__description.sql`.

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Departments / lookups show empty | Stale Redis cache | `redis-cli -a <pwd> FLUSHDB` |
| Backend returns 503 on startup | Mail health check failing | Ensure `-Dmanagement.health.mail.enabled=false` in bat |
| Nginx won't start (port 80 blocked) | Windows HTTP.sys / IIS owns port 80 | Use alternate port (8088) as configured |
| Frontend API calls fail externally | API URL not relative | Ensure `.env.production` has `VITE_API_BASE_URL=/api` |
| Redis AUTH error | Password mismatch | Match password in `redis.windows.conf` and `start-backend.bat` |
| Flyway migration fails on startup | Column already exists / wrong version | Check migration history table, never edit existing V*.sql |
| JAR build fails (file locked) | Running Java process holds the jar | Stop local backend instance before `mvn package` |
| Company not showing in New Booking | Non-admin user — old endpoint was ADMIN_VIEW only | Fixed in current version via `/api/bookings/my-company-settings` |
