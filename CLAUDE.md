# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build and run the application (default port 8080)
mvn spring-boot:run

# Build only
mvn clean package -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=WebTestApplicationTests
```

## Database

- MySQL 8.0, database `testdata`, configured in `application.properties`
- Run [sql/init_database.sql](sql/init_database.sql) to initialize tables and seed data
- Default admin account: `admin` / `admin123`; test user: `test` / `admin123`

## Architecture

Spring Boot 3.4.12 web app serving a **二次压降检测系统** (secondary voltage drop detection system). Standard layered architecture:

```
controller/  →  service/ + service/impl/  →  mapper/
```

- **Frontend**: Two static HTML pages ([login.html](src/main/resources/static/login.html), [index.html](src/main/resources/static/index.html)) using Vue.js 3 + Bootstrap 5 + Axios + ECharts, all loaded via CDN (no frontend build tool).
- **Auth**: JWT stateless token authentication. `JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header (or `?token=` URL param for PDF downloads) on every request. BCrypt for password hashing.
- **Security**: Spring Security configured in [SecurityConfig.java](src/main/java/com/hspyx/config/SecurityConfig.java) — CSRF disabled, stateless sessions, CORS allowed for `localhost:8080`. `/api/auth/**` and Swagger paths are public; everything else requires authentication.
- **Database access**: MyBatis with annotation-based SQL in mapper interfaces. Camel-case auto-mapping is enabled.

### Key data flow

**Test record CRUD** uses a three-table hierarchy:
1. `test_records` — main record (product info, environment params, overall result)
2. `test_input_details` — per-project details (PT1, PT2, CT1, CT2), linked by `record_id`
3. `test_phase_results` — three-phase data (ao/bo/co) with f%, d, dU%, Upt:U, Uyb:U, linked by `record_id` + `item_name`

Saves use `@Transactional` to write all three tables atomically. Deletes are cascaded manually in `TestRecordServiceImpl.deleteById()` (delete children first, then parent).

**RecordDTO** is the central DTO that carries all three layers of data for both read (`getDetail`) and write (`save`) operations. It has nested inner classes: `DeviceInfo`, `ProjectItem` (with `PhaseData`), `TestItem`, and `TestResult`.

**OCR flow**: User uploads an image → `OcrController` → `OcrServiceImpl` calls Baidu OCR API with cached access token → raw text parsed via regex → `RecordDTO` returned for form auto-fill. Three recognition endpoints: file upload, Base64 input, and auto-fetch from a local HTTP service.

**PDF export**: `GET /api/records/{id}/pdf` generates a Chinese-language PDF report using iTextPDF 5 with `STSong-Light` font. Since browsers can't set headers on direct downloads, the JWT token is passed as a URL query parameter.

### API response format

Most controllers use the `Result` wrapper class (`code=1` for success, `code=0` for failure). The auth controller uses a separate `AuthResponse` with HTTP status codes. Swagger UI is available at `/swagger-ui.html` when the app is running.
