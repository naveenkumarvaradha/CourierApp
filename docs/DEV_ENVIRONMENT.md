# Dev Environment Reference

Everything installed on this machine to build and run ShipDesk (CourierApp) locally, and the exact configuration used to start it. This is a snapshot for this machine — versions will drift; re-run the version commands below to check current state.

---

## IDE

| Tool | Version | Notes |
|---|---|---|
| VS Code | 1.133.0 | Opened on the repo root |

### VS Code extensions installed

| Extension | ID | Version |
|---|---|---|
| Extension Pack for Java | `vscjava.vscode-java-pack` | 0.31.1 |
| Language Support for Java | `redhat.java` | 1.55.0 |
| Debugger for Java | `vscjava.vscode-java-debug` | 0.59.0 |
| Test Runner for Java | `vscjava.vscode-java-test` | 0.46.0 |
| Maven for Java | `vscjava.vscode-maven` | 0.45.3 |
| Project Manager for Java | `vscjava.vscode-java-dependency` | 0.27.6 |
| Gradle for Java | `vscjava.vscode-gradle` | 3.18.0 |
| Spring Boot Extension Pack | `vmware.vscode-boot-dev-pack` | 0.2.2 |
| Spring Boot Tools | `vmware.vscode-spring-boot` | 2.3.0 |
| Spring Boot Dashboard | `vscjava.vscode-spring-boot-dashboard` | 0.14.0 |
| Spring Initializr | `vscjava.vscode-spring-initializr` | 0.12.0 |
| ESLint | `dbaeumer.vscode-eslint` | 3.0.34 |
| Prettier | `esbenp.prettier-vscode` | 12.4.0 |
| ES7+ React/Redux/React-Native snippets | `dsznajder.es7-react-js-snippets` | 4.4.3 |
| YAML | `redhat.vscode-yaml` | 1.24.0 |
| SQLTools | `mtxr.sqltools` | 0.28.6 |

Reinstall all of them:
```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension dsznajder.es7-react-js-snippets
code --install-extension redhat.vscode-yaml
code --install-extension mtxr.sqltools
```

---

## Toolchain

| Tool | Version | Check with |
|---|---|---|
| Java (JDK) | 25.0.3 (Oracle) | `java -version` |
| Maven | 3.9.12 | `mvn -v` |
| Node.js | 22.22.1 (LTS) | `node -v` |
| npm | 9.2.0 | `npm -v` |
| PostgreSQL | 18.4 | `psql --version` |
| Redis | 8.0.5 | `redis-cli --version` |
| GitHub CLI | 2.97.0 | `gh --version` |
| pgAdmin4 (desktop) | 9.17 | `dpkg -l pgadmin4-desktop` |

Install sources: Java came preinstalled; Maven, PostgreSQL, and Redis via `apt`; Node.js via the NodeSource `setup_lts.x` repo; `gh` via the official GitHub CLI apt repo (`cli.github.com/packages`); pgAdmin4 via the official pgAdmin apt repo (`ftp.postgresql.org/pub/pgadmin/pgadmin4/apt`). See [INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md) for the step-by-step commands.

---

## Local services

| Service | Address | Managed by |
|---|---|---|
| PostgreSQL | `localhost:5432` | `systemctl` (`postgresql.service`) |
| Redis | `localhost:6379` | `systemctl` (`redis-server.service`) |
| Backend (Spring Boot) | `localhost:8080` | run manually (`mvn spring-boot:run`) |
| Frontend (Vite) | `localhost:5173` | run manually (`npm run dev`) |
| pgAdmin4 | desktop app | run manually (`/usr/pgadmin4/bin/pgadmin4`) |

### Database

| Field | Value |
|---|---|
| Database | `courierdb` |
| App user | `courier_app` / `Courier@123` |
| Superuser (for pgAdmin/admin tasks) | `postgres` / `postgres` |

Add both as separate server connections in pgAdmin if you need superuser-level access (e.g. to manage roles) — don't grant `courier_app` superuser, it doesn't need it and shouldn't have it.

### Default application login

| Field | Value |
|---|---|
| Company | `My Company` |
| Username | `admin` |
| Password | `Admin@123` |

Seeded by `backend/src/main/resources/db/migration/V2__seed.sql` on first startup.

---

## Environment variables used to run the backend

```bash
export DB_USERNAME=courier_app
export DB_PASSWORD=Courier@123
export JWT_SECRET=<a random string, at least 64 characters — HS512 requires >= 512 bits>
export REDIS_HOST=localhost
export REDIS_PORT=6379
export CORS_ORIGINS=http://localhost:5173
```

Full variable reference: see the "Environment Variables" table in [../README.md](../README.md). A ready-to-copy template is at [../.env.example](../.env.example).

---

## OS-level changes made on this machine

These aren't part of the app, but were made while setting up this environment — listed here so they're not a mystery later:

| Change | Why |
|---|---|
| Installed `libgtk2.0-0t64` | Fixed FortiClient's tray icon, which was crash-looping (missing GTK2 library) |
| Installed `earlyoom` | Kills the worst memory offender before the system fully locks up |
| Installed & configured `zram` (`systemd-zram-generator`, 3.5 GB, zstd) | Fast compressed swap, reduces disk-thrashing hangs under memory pressure |
| Raised `vm.min_free_kbytes` to 131072 (`/etc/sysctl.d/99-memory-headroom.conf`) | More kernel headroom under memory pressure |

This machine has 7.1 GB RAM — avoid running `mvn`/`npm install` at the same time as a heavy browser session or VS Code's Java indexer if things feel slow.
