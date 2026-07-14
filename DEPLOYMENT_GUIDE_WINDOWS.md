# ShipDesk — Production Deployment Guide (Windows Server)

Current production server: **TKL-SRV-COURIER** — `192.168.1.15`

---

## Architecture Overview

```
Internet
  └── FortiGate (103.102.97.163)
        ├── VIP: :5173  → 192.168.1.15:8088  (frontend)
        └── VIP: :8080  → 192.168.1.15:8080  (API direct, if needed)

192.168.1.15 (TKL-SRV-COURIER)
  └── Nginx (port 8088)
        ├── /         → D:\ShipDesk\frontend\dist\  (React SPA)
        └── /api/     → http://127.0.0.1:8080       (Spring Boot)
              └── PostgreSQL (5432) + Redis (6379)
```

---

## Production Services

| Service | Type | Port | Auto-start | Config |
|---|---|---|---|---|
| `ShipDeskBackend` | Windows Service (WinSW) | 8080 | Automatic | `D:\ShipDesk\ShipDeskBackend.xml` |
| `Redis` | Windows Service | 6379 | Automatic | `C:\Program Files\Redis\redis.windows.conf` |
| `NginxAutoStart` | Scheduled Task | 8088 | AtStartup (SYSTEM) | `D:\nginx\conf\nginx.conf` |
| `ShipDeskFrontend` | Windows Service | — | **Disabled** | Replaced by Nginx |

---

## Deploying Updates

This is the process for every release.

### Step 1 — Build on Dev Machine

```powershell
# Backend
cd D:\Application\CourierApp\backend
mvn package -DskipTests
# Output: backend\target\courier-booking-backend-1.0.0.jar

# Frontend
cd D:\Application\CourierApp\frontend
npm run build
# Output: frontend\dist\
```

> **Stop the local backend before building** — the running process locks the JAR and `mvn package` will fail with a rename error.

### Step 2 — Backup Production Database

On the production server (192.168.1.15) via PowerShell:

```powershell
$date = Get-Date -Format "yyyyMMdd_HHmm"
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" `
  -U courier_app `
  -d courierdb `
  -f "D:\ShipDesk\backups\backup_$date.sql"
```

### Step 3 — Copy Files via WinSCP

Connect WinSCP to `192.168.1.15` and copy:

| Local (Dev) | Remote (Prod) |
|---|---|
| `backend\target\courier-booking-backend-1.0.0.jar` | `D:\ShipDesk\backend\courier-booking-backend-1.0.0.jar` |
| `frontend\dist\` (all contents) | `D:\ShipDesk\frontend\dist\` |

### Step 4 — Restart Backend on Prod

```powershell
Restart-Service ShipDeskBackend

# Watch logs to confirm startup:
Get-Content D:\ShipDesk\logs\backend.log -Wait -Tail 50
# Wait for: "Started CourierApplication in X.XXX seconds"
```

Flyway applies any new migration files automatically on startup.

### Step 5 — Reload Nginx (if frontend changed)

```powershell
cd D:\nginx
.\nginx.exe -s reload
```

### Step 6 — Smoke Test

- LAN: `http://192.168.1.15:8088`
- External: `http://103.102.97.163:5173`
- Login, check new features, verify existing data intact

---

## Nginx Configuration

File: `D:\nginx\conf\nginx.conf`

```nginx
worker_processes 1;
events { worker_connections 1024; }
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;

    server {
        listen 8088;
        root D:/ShipDesk/frontend/dist;
        index index.html;

        location / {
            try_files $uri $uri/ /index.html;
        }

        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_read_timeout 60s;
        }
    }
}
```

> Port 80 is blocked by Windows HTTP.sys (PID 4). **Do not change to port 80.**  
> FortiGate VIP handles external port translation (5173 → 8088).

### Nginx Commands

```powershell
cd D:\nginx
.\nginx.exe -t          # test config syntax
.\nginx.exe -s reload   # reload config (no downtime)
.\nginx.exe -s stop     # stop nginx
.\nginx.exe             # start nginx (run from D:\nginx directory)
```

---

## Backend Service (WinSW)

Files on prod:

| File | Purpose |
|---|---|
| `D:\ShipDesk\ShipDeskBackend.xml` | WinSW service definition |
| `D:\ShipDesk\start-backend.bat` | Sets env vars + starts JVM |
| `D:\ShipDesk\logs\backend.log` | Application log |

### Service Commands

```powershell
Start-Service ShipDeskBackend
Stop-Service ShipDeskBackend
Restart-Service ShipDeskBackend
Get-Service ShipDeskBackend
```

---

## Redis

Service name: `Redis` (or `bio-cache` depending on install)

```powershell
# Clear Redis cache (required after model/serializer changes):
redis-cli -a <password> FLUSHDB

# Check Redis is running:
Get-Service Redis
```

> Clear Redis cache whenever you see Jackson deserialization errors (stale `@class` metadata from `GenericJackson2JsonRedisSerializer`).

---

## Database Migrations

Flyway migrations are in `backend/src/main/resources/db/migration/`.  
They run automatically on backend startup — **no manual SQL needed**.

Current highest migration: **V32** (`mfa_forced` column on users)

**Rule:** Never edit existing migration files. Create `V{n+1}__description.sql` for every schema change.

---

## Environment Variables (start-backend.bat)

Key variables set in `D:\ShipDesk\start-backend.bat`:

| Variable | Value |
|---|---|
| `DB_USERNAME` | `courier_app` |
| `DB_PASSWORD` | configured |
| `JWT_SECRET` | 64-char random string |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `REDIS_PASSWORD` | configured |
| `CORS_ORIGINS` | `http://192.168.1.15:8088,http://103.102.97.163:5173,...` |
| `KAFKA_ENABLED` | `false` |

---

## Service Startup Order on Reboot

All services auto-start. Recommended startup order (handled automatically):

1. **Redis** — Windows Service (Automatic)
2. **ShipDeskBackend** — Windows Service (Automatic)
   - Consider `delayed-auto` to ensure Redis is ready first:
   ```powershell
   sc.exe config ShipDeskBackend start= delayed-auto
   ```
3. **Nginx** — Scheduled Task (AtStartup, SYSTEM)

---

## Prod File Locations

| What | Where |
|---|---|
| Backend JAR | `D:\ShipDesk\backend\courier-booking-backend-1.0.0.jar` |
| Frontend dist | `D:\ShipDesk\frontend\dist\` |
| Nginx config | `D:\nginx\conf\nginx.conf` |
| Nginx binary | `D:\nginx\nginx.exe` |
| WinSW service def | `D:\ShipDesk\ShipDeskBackend.xml` |
| Backend bat | `D:\ShipDesk\start-backend.bat` |
| Backend logs | `D:\ShipDesk\logs\backend.log` |
| Redis config | `C:\Program Files\Redis\redis.windows.conf` |
| DB backups | `D:\ShipDesk\backups\` |

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Lookups / departments show empty | Stale Redis cache | `redis-cli -a <pwd> FLUSHDB` |
| Backend 503 on startup | Mail health check | `-Dmanagement.health.mail.enabled=false` in bat |
| Nginx fails to start | Wrong working directory | Must `cd D:\nginx` then `.\nginx.exe` |
| Nginx port 80 denied | HTTP.sys / IIS owns port 80 | Keep port 8088 as configured |
| API not reachable externally | Frontend env var | `.env.production` must have `VITE_API_BASE_URL=/api` |
| JAR build fails (rename error) | Running JVM holds JAR | Stop local backend before `mvn package` |
| Flyway fails at startup | Migration version conflict | Check `flyway_schema_history` table, never edit V*.sql |

---

## Deployment Checklist (Every Update)

- [ ] Stop local backend (so JAR isn't locked during build)
- [ ] `mvn package -DskipTests` on dev machine
- [ ] `npm run build` on dev machine
- [ ] Backup prod DB: `pg_dump -U courier_app -d courierdb -f backup_YYYYMMDD.sql`
- [ ] Copy JAR via WinSCP → `D:\ShipDesk\backend\`
- [ ] Copy `dist\` via WinSCP → `D:\ShipDesk\frontend\dist\`
- [ ] `Restart-Service ShipDeskBackend` on prod
- [ ] Watch logs — confirm `Started CourierApplication in X.XXX seconds`
- [ ] `.\nginx.exe -s reload` (if frontend changed)
- [ ] Smoke test: login, check features, verify data

---

## First-Time Setup Checklist (Fresh Server)

- [ ] Install JDK (current prod: JDK 25.0.3 at `C:\Program Files\Java\jdk-25.0.3`)
- [ ] Install PostgreSQL 18, create `courierdb` DB and `courier_app` user
- [ ] Install Redis for Windows (tporadowski build), configure password in `redis.windows.conf`
- [ ] Download Nginx 1.30.3 stable to `D:\nginx`
- [ ] Create folder structure: `D:\ShipDesk\backend\`, `D:\ShipDesk\frontend\dist\`, `D:\ShipDesk\logs\`, `D:\ShipDesk\backups\`
- [ ] Create `D:\ShipDesk\start-backend.bat` with all env vars
- [ ] Create `D:\ShipDesk\ShipDeskBackend.xml` (WinSW service definition)
- [ ] Install WinSW service: `ShipDeskBackend.exe install`
- [ ] Deploy JAR and frontend dist
- [ ] Start backend manually once — confirm Flyway migrations complete
- [ ] Configure `D:\nginx\conf\nginx.conf` (listen 8088, proxy /api/)
- [ ] Register Nginx auto-start via Scheduled Task (AtStartup, SYSTEM, WorkingDir=D:\nginx)
- [ ] Configure FortiGate VIPs: `:5173 → :8088` and `:8080 → :8080`
- [ ] Test LAN: `http://192.168.1.15:8088`
- [ ] Test external: `http://103.102.97.163:5173`
