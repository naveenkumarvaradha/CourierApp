# ShipDesk — Production Deployment Guide

---

## 1. Software to Install on the New Server

| Software | Version | Purpose |
|---|---|---|
| **Java (JDK)** | 25 LTS | Run the Spring Boot backend JAR |
| **PostgreSQL** | 15 or 16 | Production database |
| **Nginx** | Latest stable | Serve frontend + reverse proxy to backend |
| **Node.js** | 20 LTS | Build the frontend (on build machine only) |
| **Maven** | 3.9+ | Build the backend JAR (on build machine only) |

> **Node.js and Maven are only needed on your development/build machine, NOT on the production server.**
> The production server only needs Java, PostgreSQL, and Nginx.

---

## 2. Install Software on Production Server (Ubuntu/Debian)

```bash
# -- Java 25 --
sudo apt update
sudo apt install -y openjdk-25-jdk
java -version   # should print openjdk 25

# -- PostgreSQL 16 --
sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable postgresql
sudo systemctl start postgresql

# -- Nginx --
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

---

## 3. Create the Production Database

```bash
# Switch to postgres user
sudo -u postgres psql

-- Inside psql:
CREATE USER shipdesk_prod WITH PASSWORD 'StrongPassword@2024';
CREATE DATABASE shipdesk_prod OWNER shipdesk_prod;
GRANT ALL PRIVILEGES ON DATABASE shipdesk_prod TO shipdesk_prod;
\q
```

> Flyway will automatically create all tables when the backend starts for the first time.
> **No need to run any SQL scripts manually for schema.**

---

## 4. Production Data Setup (After First Start)

After the backend starts and Flyway runs all migrations, run this SQL
to clean out the dev/test data while keeping all configuration:

```sql
-- Connect as shipdesk_prod:
-- psql -U shipdesk_prod -d shipdesk_prod

-- ① Remove all bookings and their sequences (counter resets to 1 automatically)
TRUNCATE TABLE bookings CASCADE;
TRUNCATE TABLE booking_sequences CASCADE;

-- ② Remove all parties EXCEPT company-linked ones
DELETE FROM parties WHERE party_code NOT LIKE 'COMPANY%';

-- ③ Clear audit logs for bookings and parties (optional but recommended)
DELETE FROM audit_logs WHERE module IN ('BOOKING', 'PARTY', 'MASTER');

-- ④ Verify: party counter will restart from PTY000001 on next party creation
-- Verify: booking counter will restart from 00001 on next booking day
SELECT COUNT(*) FROM bookings;        -- should be 0
SELECT COUNT(*) FROM parties WHERE party_code NOT LIKE 'COMPANY%';  -- should be 0
```

---

## 5. Build the Application (on your development machine)

### 5a. Build the Backend JAR

```bash
cd D:\Application\CourierApp\backend

# Clean build, skip tests (tests need a running DB)
mvn clean package -DskipTests

# Output: backend/target/courier-booking-backend-0.0.1-SNAPSHOT.jar
```

### 5b. Build the Frontend

```bash
cd D:\Application\CourierApp\frontend

npm install
npm run build

# Output: frontend/dist/  (a folder of static HTML/CSS/JS files)
```

---

## 6. Production Configuration File

Create `/opt/shipdesk/application-prod.yml` on the production server:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shipdesk_prod
    username: shipdesk_prod
    password: StrongPassword@2024
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
    username: YOUR_PROD_EMAIL@domain.com
    password: YOUR_PROD_EMAIL_PASSWORD
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

app:
  jwt:
    secret: GENERATE_A_64_CHAR_RANDOM_STRING_HERE_FOR_PRODUCTION
    access-token-expiry-minutes: 30
    refresh-token-expiry-days: 7
  cors:
    allowed-origins: http://YOUR_PROD_SERVER_IP_OR_DOMAIN
  frontend-url: http://YOUR_PROD_SERVER_IP_OR_DOMAIN
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
```

> **Generate a secure JWT secret:**
> ```bash
> openssl rand -hex 32
> ```

---

## 7. Deploy Files to Production Server

```bash
# On your development machine — copy files to server
# Replace 192.168.X.X with your production server IP

# Copy backend JAR
scp backend/target/courier-booking-backend-0.0.1-SNAPSHOT.jar \
    user@192.168.X.X:/opt/shipdesk/app.jar

# Copy frontend build
scp -r frontend/dist/* \
    user@192.168.X.X:/var/www/shipdesk/
```

---

## 8. Run the Backend as a System Service (systemd)

Create `/etc/systemd/system/shipdesk.service` on the production server:

```ini
[Unit]
Description=ShipDesk Courier App Backend
After=network.target postgresql.service

[Service]
User=www-data
WorkingDirectory=/opt/shipdesk
ExecStart=/usr/bin/java -jar /opt/shipdesk/app.jar \
  --spring.config.additional-location=file:/opt/shipdesk/application-prod.yml \
  --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=append:/var/log/shipdesk/app.log
StandardError=append:/var/log/shipdesk/app-error.log

[Install]
WantedBy=multi-user.target
```

```bash
# Create directories and set permissions
sudo mkdir -p /opt/shipdesk /var/www/shipdesk /var/log/shipdesk
sudo chown -R www-data:www-data /opt/shipdesk /var/log/shipdesk

# Enable and start the service
sudo systemctl daemon-reload
sudo systemctl enable shipdesk
sudo systemctl start shipdesk

# Check status and logs
sudo systemctl status shipdesk
sudo tail -f /var/log/shipdesk/app.log
```

---

## 9. Configure Nginx

Create `/etc/nginx/sites-available/shipdesk`:

```nginx
server {
    listen 80;
    server_name YOUR_SERVER_IP_OR_DOMAIN;

    # Frontend static files
    root /var/www/shipdesk;
    index index.html;

    # All frontend routes → index.html (React Router)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API reverse proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        client_max_body_size 20M;
    }
}
```

```bash
# Enable the site
sudo ln -s /etc/nginx/sites-available/shipdesk /etc/nginx/sites-enabled/
sudo nginx -t          # test config — should say "ok"
sudo systemctl reload nginx
```

---

## 10. Verify the Deployment

```bash
# 1. Backend health check
curl http://localhost:8080/api/actuator/health
# Should return: {"status":"UP"}

# 2. Open browser
http://YOUR_SERVER_IP
# Should show the ShipDesk login page
```

---

## 11. Deploying Future Updates (Zero-Downtime Process)

When you add new features or fix bugs, follow this process every time:

### Step 1 — Make changes on development machine
Develop and test everything locally as usual.

### Step 2 — Build new artifacts

```bash
# Backend
cd backend
mvn clean package -DskipTests

# Frontend
cd ../frontend
npm run build
```

### Step 3 — Transfer to production server

```bash
# Copy new JAR
scp backend/target/courier-booking-backend-0.0.1-SNAPSHOT.jar \
    user@PROD_SERVER:/opt/shipdesk/app.jar

# Copy new frontend
scp -r frontend/dist/* \
    user@PROD_SERVER:/var/www/shipdesk/
```

### Step 4 — Restart backend service

```bash
# SSH into production server
ssh user@PROD_SERVER

sudo systemctl restart shipdesk

# Watch logs to confirm startup
sudo tail -f /var/log/shipdesk/app.log
# Wait for: "Started CourierApplication in X.XXX seconds"
```

### Step 5 — Reload Nginx (only if frontend changed)

```bash
sudo systemctl reload nginx
```

> **Flyway runs automatically** on every restart.
> New database migrations (V23, V24, ...) are applied automatically.
> No manual SQL needed for schema changes.

---

## 12. Important Rules for Safe Production Deployments

| Rule | Why |
|---|---|
| **Never edit files directly on the server** | Your dev machine is the source of truth |
| **Always build before deploying** | Don't copy source files, copy build output only |
| **Never run `DROP` or `ALTER` SQL manually** | Use Flyway migrations only (`VXX__description.sql`) |
| **Keep `application-prod.yml` on the server only** | Never commit passwords to git |
| **Test locally before deploying** | Dev environment must mirror prod closely |
| **Keep backups before each deployment** | See backup command below |

---

## 13. Database Backup Before Each Deployment

Run this on the production server before every deployment:

```bash
# Create a timestamped backup
pg_dump -U shipdesk_prod -d shipdesk_prod \
  > /opt/shipdesk/backups/backup_$(date +%Y%m%d_%H%M%S).sql

# Keep only last 10 backups
ls -t /opt/shipdesk/backups/*.sql | tail -n +11 | xargs rm -f
```

To restore a backup if something goes wrong:

```bash
psql -U shipdesk_prod -d shipdesk_prod < /opt/shipdesk/backups/backup_YYYYMMDD_HHMMSS.sql
```

---

## 14. Quick Reference — Useful Commands on Production Server

```bash
# Check if backend is running
sudo systemctl status shipdesk

# View live logs
sudo tail -f /var/log/shipdesk/app.log

# Restart backend
sudo systemctl restart shipdesk

# Stop backend
sudo systemctl stop shipdesk

# Check Nginx status
sudo systemctl status nginx

# Test Nginx config
sudo nginx -t

# Check PostgreSQL
sudo systemctl status postgresql

# Connect to DB
psql -U shipdesk_prod -d shipdesk_prod

# Check disk space
df -h

# Check memory
free -h
```

---

## 15. Folder Structure on Production Server

```
/opt/shipdesk/
  app.jar                    ← Spring Boot JAR (replaced on each deploy)
  application-prod.yml       ← Production config (never replaced by deploy)
  backups/                   ← Database backups

/var/www/shipdesk/
  index.html                 ← React frontend (replaced on each deploy)
  assets/                    ← JS/CSS bundles

/var/log/shipdesk/
  app.log                    ← Backend stdout logs
  app-error.log              ← Backend stderr logs

/etc/nginx/sites-available/
  shipdesk                   ← Nginx config (set once, rarely changed)

/etc/systemd/system/
  shipdesk.service           ← Service definition (set once)
```

---

## 16. Summary — First-Time Setup Checklist

- [ ] Install Java 25, PostgreSQL, Nginx on production server
- [ ] Create database and user
- [ ] Create `/opt/shipdesk/` and `/var/www/shipdesk/` directories
- [ ] Create `application-prod.yml` with production credentials
- [ ] Build JAR on dev machine (`mvn clean package -DskipTests`)
- [ ] Build frontend on dev machine (`npm run build`)
- [ ] Copy JAR to `/opt/shipdesk/app.jar`
- [ ] Copy `dist/*` to `/var/www/shipdesk/`
- [ ] Create and enable `shipdesk.service`
- [ ] Start service — Flyway runs and creates all tables
- [ ] Run the data cleanup SQL (truncate bookings, delete parties)
- [ ] Configure Nginx and reload
- [ ] Open browser and verify login works
- [ ] Create first admin user via the UI

---

## 17. For Future Deployments — Checklist

- [ ] Backup production database
- [ ] Build JAR on dev machine
- [ ] Build frontend on dev machine
- [ ] Copy JAR to server
- [ ] Copy frontend dist to server
- [ ] `sudo systemctl restart shipdesk`
- [ ] Watch logs and confirm startup
- [ ] `sudo systemctl reload nginx`
- [ ] Smoke test in browser
