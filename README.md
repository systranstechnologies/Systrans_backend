# SysTrans Spring Boot API

This service provides the jobs API used by the Angular careers page:

- `GET /api/jobs` lists published vacancies.
- `GET /api/admin/session` checks the signed admin cookie.
- `POST /api/admin/login` signs in with `ADMIN_PASSWORD`.
- `POST /api/admin/logout` clears the admin cookie.
- `POST /api/jobs` creates a vacancy for an authenticated admin.

## Configuration

Spring Boot reads these variables from the process environment or a local `.env` file in this backend project:

- `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `ADMIN_PASSWORD`
- `SESSION_SECRET` with at least 32 characters
- `SERVER_PORT` (optional; defaults to `8080`)
- `COOKIE_SECURE=true` when served over HTTPS; this sets the cross-site admin cookie to `SameSite=None; Secure`. It defaults to `true` for production deployments. For local HTTP-only development, set `COOKIE_SECURE=false`.
- `CORS_ALLOWED_ORIGINS` with comma-separated Angular site origins or supported origin patterns

The database itself must already exist. Startup applies `src/main/resources/schema.sql`, so the database user needs permission to create the jobs table as well as read and insert rows.

## Run and test

```powershell
Copy-Item .env.example .env
.\mvnw.cmd spring-boot:run
```

Replace the example MySQL credentials, admin password, and session secret in `.env` first. `SESSION_SECRET` must be at least 32 characters. Do not commit `.env`.

This starts just the API at `http://localhost:8080`; **you do not need `npm start` to run Spring Boot**. Start Angular separately only when you want the website UI.

Run backend tests independently:

```powershell
.\mvnw.cmd test
```

For production, deploy this Spring service to a Java-capable host, configure its environment variables, and set the frontend API URL in `src/environments/environment.ts`. The frontend and backend are independent deployments.
