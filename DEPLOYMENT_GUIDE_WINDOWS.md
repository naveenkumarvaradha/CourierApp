# ShipDesk — Production Deployment Guide (Windows 10 / 11 VM)

---

## 1. Software to Install on Production Windows Server

| # | Software | Version | Download Link | Purpose |
|---|---|---|---|---|
| 1 | **Java JDK 21** | 21 LTS | https://adoptium.net | Run the Spring Boot JAR |
| 2 | **PostgreSQL** | 16 | https://www.postgresql.org/download/windows/ | Production database |
| 3 | **Nginx for Windows** | Latest stable | https://nginx.org/en/download.html | Serve frontend + API proxy |
| 4 | **NSSM** | Latest | https://nssm.cc/download | Run backend as Windows Service |

> **Node.js and Maven are NOT needed on the production server.**
> Install them only on your development/build machine.

---

## 2. Step-by-Step: Install Software on Production Server

---

### Step 2.1 — Install Java 21

1. Download **Eclipse Temurin JDK 21** (`.msi` installer) from https://adoptium.net
2. Run the installer → select **"Add to PATH"** and **"Set JAVA_HOME"** checkboxes
3. After install, open **Command Prompt** and verify:

```cmd
java -version
```
Should print: `openjdk version "21.x.x"`

---

### Step 2.2 — Install PostgreSQL

1. Download PostgreSQL 16 Windows installer from https://www.postgresql.org/download/windows/
2. Run installer:
   - Installation directory: `C:\Program Files\PostgreSQL\16`
   - Data directory: `C:\Program Files\PostgreSQL\16\data`
   - Set **superuser password** (remember this — e.g. `Admin@Postgres123`)
   - Port: **5432** (default)
   - Uncheck StackBuilder at the end
3. PostgreSQL runs as a Windows Service automatically after install

---

### Step 2.3 — Create Production Database

Open **pgAdmin 4** (installed with PostgreSQL) or use **SQL Shell (psql)**:

```sql
-- In SQL Shell or pgAdmin Query Tool:
CREATE USER shipdesk_prod WITH PASSWORD 'YourStrong@Password2024';
CREATE DATABASE shipdesk_prod OWNER shipdesk_prod;
GRANT ALL PRIVILEGES ON DATABASE shipdesk_prod TO shipdesk_prod;
```

> ✅ Flyway will create ALL tables automatically when the backend starts.
> No manual SQL for schema creation needed.

---

### Step 2.4 — Install Nginx for Windows

1. Download the **stable** zip from https://nginx.org/en/download.html  
   (e.g. `nginx-1.26.2.zip`)
2. Extract to `C:\nginx`  
   Final path should be: `C:\nginx\nginx.exe`
3. Do NOT start it yet — configure it in Step 6 first

---

### Step 2.5 — Install NSSM (Service Manager)

1. Download from https://nssm.cc/download → extract zip
2. Copy `nssm.exe` (from `win64` folder) to `C:\tools\nssm.exe`
3. Add `C:\tools` to System PATH:
   - Right-click **This PC** → Properties → Advanced System Settings
   - Environment Variables → System Variables → **Path** → Edit → New → `C:\tools`

---

## 3. Folder Structure on Production Server

Create these folders manually:

```
C:\shipdesk\
  app\
    app.jar                ← Spring Boot JAR (updated on each deploy)
    application-prod.yml   ← Production config (set once, never overwritten)
    logs\
      app.log
  frontend\                ← Built React files (updated on each deploy)
    index.html
    assets\
```

```cmd
mkdir C:\shipdesk\app\logs
mkdir C:\shipdesk\frontend
```

---

## 4. Production Configuration File

Create `C:\shipdesk\app\application-prod.yml`:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shipdesk_prod
    username: shipdesk_prod
    password: YourStrong@Password2024
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  jackson:
    serialization:
      write-dates-as-timestamps: false
  mail:
    host: smtp.office365.com
    port: 587
    username: YOUR_PROD_EMAIL@yourdomain.com
    password: YOUR_EMAIL_PASSWORD
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

app:
  jwt:
    # Generate with: python -c "import secrets; print(secrets.token_hex(32))"
    secret: REPLACE_WITH_64_CHAR_RANDOM_STRING_HERE
    access-token-expiry-minutes: 30
    refresh-token-expiry-days: 7
  cors:
    allowed-origins: http://YOUR_SERVER_IP
  frontend-url: http://YOUR_SERVER_IP
  name: ShipDesk
  booking:
    number-prefix: CB
  pagination:
    default-page-size: 20
    max-page-size: 200

logging:
  level:
    com.courierapp: INFO
    org.springframework.security: WARN
  file:
    name: C:/shipdesk/app/logs/app.log
```

> **How to generate JWT secret on Windows:**
> Open PowerShell and run:
> ```powershell
> -join ((1..64) | ForEach-Object { '{0:x}' -f (Get-Random -Max 16) })
> ```

---

## 5. Build on Your Development Machine

Do this every time you want to deploy (on your Windows dev machine):

### 5a. Build Backend JAR

```cmd
cd D:\Application\CourierApp\backend

mvn clean package -DskipTests
```

Output: `backend\target\courier-booking-backend-0.0.1-SNAPSHOT.jar`

### 5b. Build Frontend

```cmd
cd D:\Application\CourierApp\frontend

npm install
npm run build
```

Output: `frontend\dist\` folder

---

## 6. First-Time Deployment to Production Server

### Step 6.1 — Copy Files

Copy these from your dev machine to the production server:

| From (Dev Machine) | To (Production Server) |
|---|---|
| `backend\target\courier-booking-backend-0.0.1-SNAPSHOT.jar` | `C:\shipdesk\app\app.jar` |
| `frontend\dist\*` (all contents) | `C:\shipdesk\frontend\` |

Use USB drive, shared network folder, or `scp` / Windows file share.

---

### Step 6.2 — Start Backend Once Manually (First Time Only)

Open **Command Prompt as Administrator** on the production server:

```cmd
cd C:\shipdesk\app

java -jar app.jar ^
  --spring.config.additional-location=file:C:/shipdesk/app/application-prod.yml
```

Watch the console. Wait for:
```
Started CourierApplication in X.XXX seconds
```

Flyway will print all migration steps being applied. Let it complete fully.

Press `Ctrl+C` to stop after you confirm it started successfully.

---

### Step 6.3 — Clean Production Data (First Time Only)

Open **pgAdmin 4** or **SQL Shell** and run:

```sql
-- Connect to: shipdesk_prod database

-- Remove all bookings (counter auto-resets to 1)
TRUNCATE TABLE bookings CASCADE;
TRUNCATE TABLE booking_sequences CASCADE;

-- Remove party master data (keep company-linked parties)
DELETE FROM parties WHERE party_code NOT LIKE 'COMPANY%';

-- Remove booking and party audit logs (optional)
DELETE FROM audit_logs WHERE module IN ('BOOKING', 'PARTY', 'MASTER');

-- Verify
SELECT COUNT(*) FROM bookings;       -- must be 0
SELECT COUNT(*) FROM booking_sequences;  -- must be 0
SELECT COUNT(*) FROM parties WHERE party_code NOT LIKE 'COMPANY%';  -- must be 0
```

> After this:
> - Next booking created will be: `C1-CB-YYYYMMDD-00001`
> - Next party created will be: `PTY000001`

---

### Step 6.4 — Install Backend as Windows Service (NSSM)

Open **Command Prompt as Administrator**:

```cmd
nssm install ShipDeskBackend
```

NSSM GUI will open. Fill in:

| Tab | Field | Value |
|---|---|---|
| **Application** | Path | `C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe` |
| **Application** | Startup directory | `C:\shipdesk\app` |
| **Application** | Arguments | `-jar C:\shipdesk\app\app.jar --spring.config.additional-location=file:C:/shipdesk/app/application-prod.yml` |
| **Details** | Display name | `ShipDesk Backend` |
| **Details** | Description | `ShipDesk Courier App Backend` |
| **Log on** | Log on as | Local System |
| **I/O** | Output (stdout) | `C:\shipdesk\app\logs\app.log` |
| **I/O** | Error (stderr) | `C:\shipdesk\app\logs\app-error.log` |

Click **Install service**, then:

```cmd
nssm start ShipDeskBackend
```

Verify it is running:
```cmd
nssm status ShipDeskBackend
```
Should print: `SERVICE_RUNNING`

---

### Step 6.5 — Configure Nginx

Edit `C:\nginx\conf\nginx.conf` — replace everything inside the `http {}` block:

```nginx
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;

    server {
        listen 80;
        server_name YOUR_SERVER_IP_OR_HOSTNAME;

        # Serve React frontend
        root   C:/shipdesk/frontend;
        index  index.html;

        # React Router — all unknown paths → index.html
        location / {
            try_files $uri $uri/ /index.html;
        }

        # Proxy API requests to Spring Boot
        location /api/ {
            proxy_pass         http://127.0.0.1:8080/api/;
            proxy_set_header   Host $host;
            proxy_set_header   X-Real-IP $remote_addr;
            proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_connect_timeout 60s;
            proxy_read_timeout    60s;
            client_max_body_size  20M;
        }
    }
}
```

Test and start Nginx:

```cmd
cd C:\nginx
nginx.exe -t
nginx.exe
```

---

### Step 6.6 — Run Nginx as Windows Service (NSSM)

```cmd
nssm install ShipDeskNginx
```

| Tab | Field | Value |
|---|---|---|
| **Application** | Path | `C:\nginx\nginx.exe` |
| **Application** | Startup directory | `C:\nginx` |

```cmd
nssm start ShipDeskNginx
```

---

### Step 6.7 — Verify Everything Works

1. Open browser on the production server:  
   `http://localhost` → Should show ShipDesk login page

2. From another machine on the same network:  
   `http://YOUR_SERVER_IP` → Should show ShipDesk login page

3. Check backend health:  
   `http://YOUR_SERVER_IP/api/actuator/health` → `{"status":"UP"}`

---

## 7. Configure Windows Services to Start Automatically on Boot

```cmd
nssm set ShipDeskBackend Start SERVICE_AUTO_START
nssm set ShipDeskNginx   Start SERVICE_AUTO_START
```

Also set PostgreSQL to auto-start (it already does by default after install).

Verify in **Windows Services** (`services.msc`):
- `postgresql-x64-16` → Automatic ✅
- `ShipDeskBackend` → Automatic ✅  
- `ShipDeskNginx` → Automatic ✅

---

## 8. Deploying Future Updates (Every Time You Add Features)

### Step 8.1 — On Your Dev Machine: Build

```cmd
:: Backend
cd D:\Application\CourierApp\backend
mvn clean package -DskipTests

:: Frontend
cd D:\Application\CourierApp\frontend
npm run build
```

### Step 8.2 — Backup Production Database First

On the production server, open **Command Prompt**:

```cmd
set PGPASSWORD=YourStrong@Password2024
"C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" ^
  -U shipdesk_prod ^
  -d shipdesk_prod ^
  -f "C:\shipdesk\backups\backup_%date:~-4,4%%date:~-7,2%%date:~0,2%_%time:~0,2%%time:~3,2%.sql"
```

> Keep backups before EVERY deployment. If something breaks, you can restore.

### Step 8.3 — Stop Backend Service

```cmd
nssm stop ShipDeskBackend
```

### Step 8.4 — Copy New Files to Production Server

| From (Dev) | To (Production) |
|---|---|
| `backend\target\courier-booking-backend-0.0.1-SNAPSHOT.jar` | `C:\shipdesk\app\app.jar` (overwrite) |
| `frontend\dist\*` | `C:\shipdesk\frontend\` (overwrite all) |

### Step 8.5 — Start Backend Service

```cmd
nssm start ShipDeskBackend
```

Watch logs to confirm successful startup:

```cmd
:: Watch live log (PowerShell)
Get-Content C:\shipdesk\app\logs\app.log -Wait -Tail 50
```

Wait for: `Started CourierApplication in X.XXX seconds`

Flyway automatically applies any new database migrations (V23, V24...).  
**No manual SQL needed for schema changes.**

### Step 8.6 — Reload Nginx (if frontend changed)

```cmd
cd C:\nginx
nginx.exe -s reload
```

### Step 8.7 — Smoke Test

Open browser → `http://YOUR_SERVER_IP`  
- Login works ✅  
- New features visible ✅  
- Existing data intact ✅

---

## 9. Windows Firewall — Allow Port 80

If users on other machines cannot access the app, open port 80:

```cmd
netsh advfirewall firewall add rule ^
  name="ShipDesk HTTP" ^
  dir=in ^
  action=allow ^
  protocol=TCP ^
  localport=80
```

---

## 10. Useful Commands — Day-to-Day Operations

```cmd
:: --- Service control ---
nssm start   ShipDeskBackend
nssm stop    ShipDeskBackend
nssm restart ShipDeskBackend
nssm status  ShipDeskBackend

nssm start   ShipDeskNginx
nssm stop    ShipDeskNginx
nssm restart ShipDeskNginx

:: --- View live backend logs (PowerShell) ---
Get-Content C:\shipdesk\app\logs\app.log -Wait -Tail 100

:: --- Nginx commands ---
cd C:\nginx
nginx.exe -t          :: test config
nginx.exe -s reload   :: reload config without restart
nginx.exe -s stop     :: stop nginx

:: --- PostgreSQL (connect to DB) ---
set PGPASSWORD=YourStrong@Password2024
"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U shipdesk_prod -d shipdesk_prod
```

---

## 11. Important Rules — Never Break Production

| Rule | Reason |
|---|---|
| ❌ Never edit files directly on production server | Dev machine is the source of truth |
| ❌ Never run `DROP TABLE` or `ALTER TABLE` manually | Use Flyway migrations only |
| ❌ Never put `application-prod.yml` in git | Contains passwords |
| ❌ Never deploy without taking a backup first | You need to be able to roll back |
| ✅ Always build on dev machine, copy artifacts only | Keep server clean |
| ✅ Always test locally before deploying | Catch errors before production |
| ✅ One new Flyway file per schema change (`VXX__name.sql`) | Safe, auto-applied, trackable |

---

## 12. First-Time Setup Checklist

- [ ] Install Java 21 JDK on production server
- [ ] Install PostgreSQL 16 on production server
- [ ] Create `shipdesk_prod` database and user
- [ ] Install Nginx to `C:\nginx`
- [ ] Install NSSM to `C:\tools\nssm.exe`
- [ ] Create folder structure `C:\shipdesk\`
- [ ] Create `C:\shipdesk\app\application-prod.yml` with real passwords
- [ ] Build JAR on dev machine (`mvn clean package -DskipTests`)
- [ ] Build frontend on dev machine (`npm run build`)
- [ ] Copy `app.jar` to `C:\shipdesk\app\`
- [ ] Copy `dist\*` to `C:\shipdesk\frontend\`
- [ ] Start JAR manually once → confirm Flyway completes → stop it
- [ ] Run cleanup SQL (truncate bookings, delete parties, clear sequences)
- [ ] Install backend as Windows service using NSSM
- [ ] Configure `C:\nginx\conf\nginx.conf`
- [ ] Install Nginx as Windows service using NSSM
- [ ] Set both services to AUTO start
- [ ] Open Windows Firewall for port 80
- [ ] Test from browser: `http://SERVER_IP`
- [ ] Login and create first admin user

---

## 13. Future Deployment Checklist (Every Update)

- [ ] Build JAR on dev machine
- [ ] Build frontend on dev machine
- [ ] Backup production DB
- [ ] `nssm stop ShipDeskBackend`
- [ ] Copy new `app.jar` → overwrite `C:\shipdesk\app\app.jar`
- [ ] Copy new `dist\*` → overwrite `C:\shipdesk\frontend\`
- [ ] `nssm start ShipDeskBackend`
- [ ] Watch log — confirm startup success
- [ ] `nginx.exe -s reload` (if frontend changed)
- [ ] Open browser and verify

---

## 14. Rollback Procedure (If Something Goes Wrong)

```cmd
:: 1. Stop the broken backend
nssm stop ShipDeskBackend

:: 2. Restore the previous JAR (keep a copy before each deploy)
copy C:\shipdesk\app\app.jar.bak C:\shipdesk\app\app.jar

:: 3. Restore the database
set PGPASSWORD=YourStrong@Password2024
"C:\Program Files\PostgreSQL\16\bin\psql.exe" ^
  -U shipdesk_prod ^
  -d shipdesk_prod ^
  -f C:\shipdesk\backups\backup_YYYYMMDD_HHMM.sql

:: 4. Start the service again
nssm start ShipDeskBackend
```

> **Best practice:** Before step 8.4, run:
> ```cmd
> copy C:\shipdesk\app\app.jar C:\shipdesk\app\app.jar.bak
> ```
