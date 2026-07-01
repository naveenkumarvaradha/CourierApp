# Installation Guide (Windows)

Step-by-step setup for everything needed to run the Courier Booking System on Windows 11. Skip a section if the tool is already installed — each section ends with a verification command.

---

## 1. Java 17 (JDK)

> Note: this machine already has a newer JDK installed (25) which is **not** compatible with the Lombok version pinned in `pom.xml`. Install JDK 17 specifically and point this project at it.

1. Download **Eclipse Temurin 17 (LTS)**: https://adoptium.net/temurin/releases/?version=17 → pick `.msi` for Windows x64.
2. Run the installer. On the "Custom Setup" screen, enable:
   - "Add to PATH"
   - "Set JAVA_HOME variable"
3. Finish the install, then open a **new** PowerShell window and verify:
   ```powershell
   java -version
   ```
   Expect `openjdk version "17.0.x"`.
4. If you have multiple JDKs installed (like the JDK 25 here) and `java -version` shows the wrong one, set `JAVA_HOME` explicitly for this project:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot"
   $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
   ```
   Add this to your PowerShell profile (`notepad $PROFILE`) to make it permanent, or set it as a Windows environment variable via System Properties → Environment Variables.

---

## 2. Apache Maven

1. Download the binary zip from https://maven.apache.org/download.cgi (e.g. `apache-maven-3.9.x-bin.zip`).
2. Extract to `C:\Program Files\Maven\apache-maven-3.9.x`.
3. Add `C:\Program Files\Maven\apache-maven-3.9.x\bin` to your **PATH** environment variable (System Properties → Environment Variables → Path → New).
4. Verify in a new terminal:
   ```powershell
   mvn -v
   ```
   Confirm it reports `Java version: 17.0.x` (from step 1). If not, fix `JAVA_HOME` first.

---

## 3. Node.js 18+ and npm

1. Download the **LTS** installer from https://nodejs.org (currently Node 20 LTS, which satisfies the 18+ requirement).
2. Run the `.msi`, accept defaults (npm is bundled).
3. Verify in a new terminal:
   ```powershell
   node -v
   npm -v
   ```

---

## 4. PostgreSQL 16

1. Download the installer from https://www.postgresql.org/download/windows/ (EDB installer).
2. Run it and follow the wizard:
   - Installation directory: default is fine.
   - Components: keep PostgreSQL Server, pgAdmin 4, Command Line Tools checked.
   - Data directory: default is fine.
   - **Superuser (postgres) password**: set one and remember it.
   - Port: keep default `5432`.
   - Locale: default.
3. Finish the install (skip Stack Builder unless you need extra tools).
4. Verify the service is running — open **Services** (`services.msc`) and confirm `postgresql-x64-16` is "Running". Or from PowerShell:
   ```powershell
   Get-Service postgresql-x64-16
   ```
5. Add PostgreSQL's `bin` folder to PATH if `psql` isn't recognized (usually `C:\Program Files\PostgreSQL\16\bin`).
6. Create the app database and user — open a terminal and connect as the superuser:
   ```powershell
   psql -U postgres
   ```
   Then at the `postgres=#` prompt:
   ```sql
   CREATE USER courier_app WITH PASSWORD 'change_me';
   CREATE DATABASE courierdb OWNER courier_app;
   \q
   ```
   (You can also do this visually in **pgAdmin 4**, which was installed alongside PostgreSQL: right-click Login/Group Roles → Create → Login/Group Role for the user, then right-click Databases → Create → Database for `courierdb`, setting Owner to `courier_app`.)

---

## 5. IDE — recommended setup

You'll want **two** editors set up well, or one IDE that handles both reasonably — here's the recommendation:

### Backend (Java/Spring Boot): **IntelliJ IDEA**
Best-in-class Spring Boot support (run configurations, bean diagrams, endpoint navigation, database tool window, Lombok support out of the box).

- **Community Edition** (free) works fine for this project — Spring Boot run support is included; only the dedicated "Spring" framework assistant and some enterprise tooling are Ultimate-only.
- If you want full Spring-aware autocompletion, application.yml property hints, and the built-in HTTP client, **Ultimate** (paid, free for students via JetBrains education program) is worth it.

Install:
1. Download from https://www.jetbrains.com/idea/download/?section=windows (Community is free; this machine already has IntelliJ 2025.3.2 installed per the JDK scan — you likely already have this).
2. On first launch, install the **Lombok plugin** if not bundled (Settings → Plugins → search "Lombok" → Install) and enable annotation processing: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → check "Enable annotation processing".
3. Open the project: File → Open → select `D:\Application\CourierApp\backend` (open the `backend` folder specifically, as its own Maven project; or open the parent `CourierApp` folder and let IntelliJ detect both Maven and Node submodules).
4. Configure Project SDK: File → Project Structure → Project → SDK → Add SDK → JDK 17 install path.
5. Right-click `pom.xml` → Maven → Reload Project to fetch dependencies.

### Frontend (React/TypeScript): **Visual Studio Code**
Lighter weight, best TypeScript/React/Vite ecosystem support.

1. Download from https://code.visualstudio.com/download.
2. Install these extensions (Extensions panel, `Ctrl+Shift+X`):
   - **ESLint** (`dbaeumer.vscode-eslint`)
   - **Prettier - Code formatter** (`esbenp.prettier-vscode`)
   - **ES7+ React/Redux/React-Native snippets** (optional, speeds up component scaffolding)
3. Open the frontend folder: File → Open Folder → `D:\Application\CourierApp\frontend`.
4. VS Code will pick up `tsconfig.json` automatically for IntelliSense; no extra TypeScript install needed (bundled).

You can also just open the whole `CourierApp` folder in VS Code if you'd rather use one editor for everything — its Java extension pack (Extension Pack for Java + Spring Boot Extension Pack) is decent, just not as deep as IntelliJ for Spring-specific tooling.

---

## 6. (Optional but recommended) DB GUI client

**pgAdmin 4** ships with the PostgreSQL installer (step 4) and is sufficient. Alternative: **DBeaver Community** (https://dbeaver.io/download/) if you prefer a universal DB client — works the same for PostgreSQL plus anything else you touch later.

---

## 7. Putting it together — first run checklist

```powershell
# 1. Verify tool versions
java -version      # 17.0.x
mvn -v             # reports Java 17
node -v             # v18+ or v20 LTS
psql --version      # 16.x

# 2. Confirm DB + user exist (from step 4)

# 3. Backend
cd D:\Application\CourierApp\backend
$env:DB_USERNAME = "courier_app"
$env:DB_PASSWORD = "change_me"
$env:JWT_SECRET = "replace-with-a-long-random-string"
$env:CORS_ORIGINS = "http://localhost:5173"
mvn spring-boot:run
# Wait for "Started CourierApplication" — API live at http://localhost:8080/api
# Swagger UI: http://localhost:8080/api/swagger-ui.html

# 4. Frontend (separate terminal)
cd D:\Application\CourierApp\frontend
npm install
npm run dev
# App live at http://localhost:5173 — log in with admin / Admin@123
```

> PowerShell note: `export VAR=value` (bash syntax in the original README) doesn't work in PowerShell — use `$env:VAR = "value"` as shown above for each new terminal session, or set them permanently via System Properties → Environment Variables so you don't have to re-set them every time.

---

## Troubleshooting quick reference

| Symptom | Fix |
|---|---|
| `mvn` reports wrong Java version | Re-check `JAVA_HOME` points at JDK 17, open a fresh terminal |
| Lombok compile errors (`ExceptionInInitializerError`, `TypeTag :: UNKNOWN`) | You're compiling with a JDK newer than Lombok supports (this happened with JDK 25 on this machine) — switch to JDK 17 via `JAVA_HOME` |
| Backend fails to start: `Connection refused` to PostgreSQL | Confirm the `postgresql-x64-16` Windows service is running and port 5432 isn't blocked |
| Backend fails to start: `password authentication failed` | Re-check `DB_USERNAME`/`DB_PASSWORD` match what you created in step 4 |
| Frontend shows network errors calling the API | Confirm backend is running on 8080 and `CORS_ORIGINS` includes `http://localhost:5173` |
| `npm install` errors about Node version | Confirm `node -v` is 18+; reinstall Node LTS if older |
