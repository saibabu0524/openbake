# OpenBake Server

A single Spring Boot application (Java 17, Maven) that serves both the REST API
(`/api/**`) and the web app (everything else) from one runnable jar. It replaces
the old split of a Python/FastAPI backend + separately-hosted Next.js frontend.

- API: `/api/**` (auth, products, orders, profile, payments, delivery, waitlist, admin)
- Web app: served at `/` — the Next.js app in `../web`, built as a static export
  and baked into this jar automatically (see [Building](#building) below)
- Docs: Swagger UI at `/docs`, OpenAPI spec at `/openapi.json`
- Health check: `/health`

## Prerequisites

- **JDK 17+**
- **Maven 3.9+**
- **MySQL 8** — either via Docker (recommended) or a local install
- **Docker** — only needed if you want Docker for MySQL and/or the full
  `docker-compose.yml` stack (server + MySQL + nginx)

Node.js is **not** required on your machine — the Maven build downloads its
own pinned Node/npm automatically to build the web app (see
[`pom.xml`](pom.xml)'s `build-web` profile).

## Quick start (Docker Compose — full stack)

From the repo root:

```bash
cp server/.env.example server/.env
# edit server/.env — at minimum set SECRET_KEY, ADMIN_PASSWORD, and the MySQL
# passwords (MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD)

docker compose up -d --build
```

This starts MySQL, the server, and an nginx reverse proxy (TLS termination —
you'll need certs in `nginx/certs/` for that to come up; for local testing you
can skip nginx and just hit the server directly, see below).

To run just MySQL + the server without nginx:

```bash
docker compose up -d --build db server
curl http://localhost:8080/health
```

## Manual setup (no Docker Compose)

### 1. Create the database

**Option A — Docker (fastest):**

```bash
docker run -d --name openbake-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=openbake \
  -p 3306:3306 \
  -v openbake-mysql-data:/var/lib/mysql \
  mysql:8.0 --default-authentication-plugin=mysql_native_password
```

Wait for it to become ready:

```bash
docker exec openbake-mysql mysqladmin ping -h 127.0.0.1 -uroot -proot --silent
```

**Option B — local MySQL install:**

```sql
CREATE DATABASE openbake CHARACTER SET utf8mb4;
CREATE USER 'bakery_user'@'%' IDENTIFIED BY 'STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON openbake.* TO 'bakery_user'@'%';
FLUSH PRIVILEGES;
```

You don't need to create tables yourself — Flyway runs the schema migration
(`src/main/resources/db/migration/V1__init_schema.sql`) automatically on
first startup, and a `CommandLineRunner` seeds categories/products/an admin
user if the database is empty.

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` (or export the same variables directly) — see the
[Configuration reference](#configuration-reference) below for what each one
does. At minimum, set `SECRET_KEY` and, if you didn't use the Docker defaults
above, `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD`.

`mvn`/`java` don't read `.env` files natively — either `export $(cat .env |
xargs)` before running, or pass the values as environment variables directly
(both shown below).

### 3. Build the jar

```bash
cd server
mvn -DskipTests package
```

This does two things:
1. Builds the web app in `../web` (installs a local Node/npm, runs `npm ci &&
   npm run build`) and copies the static export into the jar's resources.
2. Compiles the Java code and packages everything into
   `target/openbake-server.jar`.

The first build is slower (downloading Node + npm dependencies). For faster
iteration on backend-only changes once you've already built the web app once,
skip the web rebuild:

```bash
mvn -DskipTests package -P '!build-web'
```

### 4. Run the jar

```bash
DATABASE_URL="jdbc:mysql://localhost:3306/openbake?useSSL=false&allowPublicKeyRetrieval=true" \
DATABASE_USERNAME=root \
DATABASE_PASSWORD=root \
SECRET_KEY="$(openssl rand -hex 32)" \
java -jar target/openbake-server.jar
```

Or, for local development with auto-reload-friendly iteration, skip the jar
entirely and run via Maven directly (same env vars apply):

```bash
mvn spring-boot:run
```

### 5. Verify it's running

```bash
curl http://localhost:8080/health
# {"status":"healthy","database":"ok","version":"1.0.0","env":"development"}
```

Then open in a browser:
- `http://localhost:8080/` — the web app (homepage)
- `http://localhost:8080/docs` — Swagger UI for the API

## Default seeded credentials

On first startup (empty database), the seeder creates:

| Role     | Email                              | Password        |
|----------|-------------------------------------|-----------------|
| Admin    | `admin@srivinayakabakery.in` (or `ADMIN_EMAIL`) | `Admin@1234` (or `ADMIN_PASSWORD`) |
| Customer | `customer@openbake.com`            | `Customer@123`  |

**Change `ADMIN_PASSWORD` before deploying anywhere real** — the seeder only
runs once (it skips seeding if categories/products already exist), so set it
correctly on first boot.

## Configuration reference

All settings are environment variables (see [`.env.example`](.env.example)
for the full list with defaults). The most important ones:

| Variable | Purpose |
|---|---|
| `SECRET_KEY` | JWT signing secret — **must** be changed from the default in production |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | MySQL connection |
| `ALLOWED_ORIGINS` | CORS allow-list (comma-separated); `*` in dev |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin account created by the first-run seeder |
| `PAYU_MERCHANT_KEY` / `PAYU_MERCHANT_SALT` | PayU payment gateway credentials — leave blank to use PayU's dev-mock mode |
| `FIREBASE_CREDENTIALS_PATH` | Path to a Firebase service-account JSON, for Google sign-in — leave unset to disable Google auth gracefully |
| `MEDIA_DIR` | Local disk directory for uploaded profile avatars, served at `/media/**` |
| `WEB_BASE_URL` / `ANDROID_DEEP_LINK_BASE` | Redirect targets after a PayU payment completes |

## Android and web app

- **Android**: point `BASE_URL` in `../android/app/build.gradle.kts` at this
  server (defaults to `http://10.0.2.2:8080/api/` for the emulator; override
  with `-PapiBaseUrl=...` for a physical device or real deployment).
- **Web**: source lives in `../web`; this server builds and embeds it
  automatically (see [Building](#building) above) — you don't run it
  separately. `../web/.env.production` sets the API base path used in that
  build (`/api`, relative, since it's served from this same origin).

## Project layout

```
server/
├── pom.xml                  Maven build (Spring Boot, MySQL/Flyway, JWT, web-build profile)
├── Dockerfile                Multi-stage build — run from the repo root as build context
├── .env.example
└── src/main/
    ├── java/com/openbake/server/
    │   ├── config/           Security, CORS, static-resource/SPA-fallback config, seeder
    │   ├── controller/       REST controllers (one per resource area)
    │   ├── service/          Business logic (orders, payments, delivery, waitlist)
    │   ├── entity/            JPA entities
    │   ├── repository/       Spring Data repositories
    │   ├── dto/              Request/response DTOs, grouped by resource
    │   ├── security/         JWT issuing/verification, Firebase auth
    │   └── exception/        FastAPI-parity error responses
    └── resources/
        ├── application.yml
        └── db/migration/     Flyway SQL migrations
```

## Full local dev stack (server + MySQL + ngrok + Android)

From the repo root, `./run_all.sh` automates MySQL (via Docker), the server,
and an optional ngrok tunnel + Android `BASE_URL` update:

```bash
./run_all.sh              # MySQL + server + ngrok
./run_all.sh --android    # + build & install the Android debug APK
```

See `./run_all.sh --help` for details.
