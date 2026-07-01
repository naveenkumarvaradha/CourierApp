# Courier Booking System

Enterprise courier booking web application — Spring Boot 3 (Java 17) backend, React 18 + TypeScript (MUI) frontend, PostgreSQL database with Flyway-managed schema, JWT authentication, and fine-grained role/permission-based access control.

## Modules

- **Admin** — create/manage user accounts (username, password, contact details), define roles and per-module (`ADMIN`/`MASTER`/`BOOKING`/`REPORTS`) x per-action (`CREATE`/`VIEW`/`UPDATE`/`DELETE`/`APPROVE`) permissions, assign roles/direct permissions to users, and configure **approval routing** — which roles or specific users are authorized to approve/reject courier bookings.
- **Master** — courier party address book: sender/receiver/both party records with auto-generated party codes, full address, contact details, GSTIN, and active/inactive status, with search by name/city/pincode.
- **Courier Booking** — book a courier with auto-generated booking number (`CB-YYYYMMDD-NNNNN`), sender/receiver lookup, weight, packages, mode (Air/Surface/Express), charges and payment mode. Lifecycle: `BOOKED → PENDING_APPROVAL → APPROVED → IN_TRANSIT → DELIVERED` (or `CANCELLED`/`REJECTED`), enforced against the admin-configured approval routing. Generates a printable 4x6" PDF shipping label with a Code128 barcode of the booking number.
- **Reports** — booking summary reports with Weekly / Monthly / Yearly / Custom date-range presets: totals, breakdown by status, courier mode, and top sender/receiver parties, plus Excel (.xlsx) export.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Security 6 (JWT), Spring Data JPA, Maven |
| Database | PostgreSQL 16, schema/migrations via Flyway |
| PDF labels | OpenPDF + ZXing (Code128 barcode) |
| Excel export | Apache POI |
| Frontend | React 18, TypeScript, Vite, MUI (Material UI) + MUI X DataGrid, React Router, Axios, React Hook Form |

## Prerequisites

- Java 17 (JDK)
- Node.js 18+ and npm
- PostgreSQL 16 (or compatible)
- Maven (or use the included `mvnw` if present)

## Database Setup

```sql
CREATE USER courier_app WITH PASSWORD 'change_me';
CREATE DATABASE courierdb OWNER courier_app;
```

Flyway will automatically create the schema and seed data the first time the backend starts (migrations in `backend/src/main/resources/db/migration`).

## Running the Backend

Set environment variables (or rely on the defaults in `application.yml` for local dev):

```bash
export DB_USERNAME=courier_app
export DB_PASSWORD=change_me
export JWT_SECRET=$(openssl rand -base64 48)
export CORS_ORIGINS=http://localhost:5173

cd backend
mvn spring-boot:run
```

The API is served at `http://localhost:8080/api`. Swagger UI is available at `http://localhost:8080/api/swagger-ui.html`.

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The app is served at `http://localhost:5173` and talks to the backend at `http://localhost:8080/api` (override via `VITE_API_BASE_URL` in a `.env` file if needed).

## Default Login

| Username | Password |
|---|---|
| `admin` | `Admin@123` |

The seeded `admin` user has the `ADMIN` role with full permissions across all modules, and is configured in approval routing to approve/reject bookings out of the box. Seed migration also creates `BOOKING_CLERK`, `APPROVER`, and `VIEWER` roles with sensible default permission sets — adjust these (or create new roles) from the Admin module as needed.

**Change the default admin password and `JWT_SECRET` before any non-local deployment.**

## Project Structure

```
CourierApp/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/courierapp/
│       │   ├── controller/   REST endpoints (auth, admin, master/parties, bookings, reports)
│       │   ├── service/      business logic + impl
│       │   ├── entity/       JPA entities
│       │   ├── repository/   Spring Data JPA repositories
│       │   ├── dto/          request/response records
│       │   ├── mapper/       MapStruct entity<->DTO mappers
│       │   ├── security/     JWT auth filter, user details, security config
│       │   ├── enums/        ModuleType, ActionType, BookingStatus, CourierMode, etc.
│       │   └── exception/    global exception handling
│       └── resources/
│           ├── application.yml
│           └── db/migration/ Flyway scripts (V1__init.sql schema, V2__seed.sql seed data)
└── frontend/
    └── src/
        ├── api/          axios client + typed endpoint wrappers (incl. JWT refresh interceptor)
        ├── context/       AuthContext (permissions-aware), NotificationContext
        ├── components/    Layout (permission-filtered nav)
        └── pages/
            ├── admin/     Users, Roles & Permissions, Approval Routing
            ├── master/    Parties (address book)
            ├── booking/   Bookings list, Booking create/edit form
            └── reports/   Reports dashboard
```

## Notes

- Authorization is enforced server-side via `@PreAuthorize` against permission codes (e.g. `BOOKING_APPROVE`), not just role names — the frontend nav and action buttons mirror this by checking the logged-in user's `permissions` array from `/api/auth/me`.
- JWT access tokens are short-lived (30 min) with a refresh token (7 days); the Axios interceptor transparently refreshes on a 401 and redirects to login if the refresh also fails.
- All entities carry audit columns (`created_by`/`created_at`/`updated_by`/`updated_at`) populated automatically via Spring Data JPA auditing.
