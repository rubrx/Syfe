# Personal Finance Manager API

A RESTful API for managing personal finances — transactions, categories, savings goals, and monthly/yearly reports. Built as a Syfe backend intern take-home assignment.

---

## Live URL

> **Base URL:** `https://syfe-finance-manager.onrender.com/api`  
> **Swagger UI:** `https://syfe-finance-manager.onrender.com/swagger-ui.html`

---

## Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 — session-based auth (JSESSIONID) |
| Persistence | Spring Data JPA + H2 in-memory |
| Validation | Jakarta Bean Validation |
| Docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5 + Mockito + Spring Security Test |
| Coverage | JaCoCo ≥ 80% line coverage enforced on `verify` |
| Build | Maven Wrapper (`./mvnw`) |

---

## Running Locally

**Prerequisites:** Java 17+

```bash
git clone https://github.com/rubrx/Syfe.git
cd Syfe
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **H2 Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:financedb`, user: `sa`, password: empty)

---

## Running Tests

```bash
# run all tests + coverage check (must stay ≥ 80%)
./mvnw verify

# run tests only
./mvnw test

# coverage report (after verify)
open target/site/jacoco/index.html
```

Current coverage: ~94% line coverage across 98 tests.

---

## API Reference

All endpoints except register and login require a valid session cookie (`JSESSIONID`).

### Authentication

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/register` | Register a new user | No |
| POST | `/api/auth/login` | Log in and receive session cookie | No |
| POST | `/api/auth/logout` | Invalidate session | Yes |

### Categories

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/categories` | List system defaults + user's custom categories | Yes |
| POST | `/api/categories` | Create a custom category | Yes |
| DELETE | `/api/categories/{name}` | Delete a custom category by name | Yes |

### Transactions

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/transactions` | List transactions (filterable) | Yes |
| POST | `/api/transactions` | Create a transaction | Yes |
| PUT | `/api/transactions/{id}` | Partial update (amount, category, description) | Yes |
| DELETE | `/api/transactions/{id}` | Delete a transaction | Yes |

GET filter params: `startDate`, `endDate`, `categoryId`, `type` (INCOME / EXPENSE)

### Savings Goals

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/goals` | List all goals with live progress | Yes |
| POST | `/api/goals` | Create a savings goal | Yes |
| GET | `/api/goals/{id}` | Get one goal with live progress | Yes |
| PUT | `/api/goals/{id}` | Update targetAmount or targetDate | Yes |
| DELETE | `/api/goals/{id}` | Delete a goal | Yes |

### Reports

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/reports/monthly/{year}/{month}` | Monthly income/expense by category | Yes |
| GET | `/api/reports/yearly/{year}` | Yearly income/expense by category | Yes |

---

## Error Format

Every error response uses this shape:

```json
{
  "timestamp": "2026-06-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Transaction date cannot be in the future",
  "path": "/api/transactions",
  "fieldErrors": [
    { "field": "amount", "message": "Amount must be greater than 0" }
  ]
}
```

`fieldErrors` is only present for validation failures (400). All other errors omit it.

| Status | Cause |
|--------|-------|
| 400 | Validation failure, business rule violation, bad date format |
| 401 | Not authenticated |
| 403 | Authenticated but not authorised (e.g. accessing another user's goal) |
| 404 | Resource not found |
| 405 | HTTP method not supported |
| 409 | Duplicate resource (e.g. duplicate category name) |
| 500 | Unexpected error (logged server-side, generic message to client) |

---

## Design Decisions

### Session-based auth (not JWT)
The spec explicitly requires session cookies. Spring Security 6 does not auto-persist the `SecurityContext` for programmatic logins — `HttpSessionSecurityContextRepository.saveContext()` must be called explicitly after authentication. Session ID is rotated on login to prevent session fixation.

### CSRF disabled
This is a JSON API with no browser form submissions. The session cookie is `HttpOnly` and `SameSite=Lax`, which provides equivalent CSRF protection for same-site requests. `Secure` is enabled in the `prod` profile.

### 403 for goals, 404 for transactions (cross-user access)
Goals expose `id`-based paths where the existence of the resource is ambiguous — returning 403 leaks existence but is the expected behaviour per the spec. Transactions use `findByIdAndUser` which treats "belongs to someone else" identically to "not found", returning 404 in both cases.

### `type` denormalised on `Transaction`
`CategoryType` is stored directly on each transaction row. This avoids a JOIN for the common `?type=INCOME` filter, and `setCategory()` keeps it in sync automatically.

### Category name uniqueness is case-insensitive
`UPPER(name)` comparison in JPQL. Original casing is preserved in storage. Clashing with a system default name is also a 409 — "Salary" and "salary" conflict.

### `isCustom` field name
Jackson strips the `is` prefix from boolean record accessors. `@JsonProperty("isCustom")` overrides this so the JSON field matches the spec exactly.

### Goal progress computed on every read
No caching. Two `SUM` queries (one INCOME, one EXPENSE) fire per goal read. The spec says "computed on every read" — caching would be premature optimisation for an assignment.

### Report aggregation in SQL
Monthly and yearly reports use `GROUP BY t.category.name` JPQL queries. No Java-side looping over raw transactions.

### Negative progress / >100% percentage are valid
If expenses exceed income since `startDate`, `currentProgress` is negative. No clamping — the formula is returned as-calculated.

### Clock injection
`LocalDate.now()` is never called directly. All "today" references go through an injected `java.time.Clock` bean so service tests are deterministic without mocking `LocalDate`.

---

## Project Structure

```
src/main/java/com/rubrangso/finance/
├── FinanceManagerApplication.java
├── config/
│   ├── ClockConfig.java           # Clock bean
│   ├── DefaultCategorySeeder.java # Seeds 7 default categories on startup
│   ├── OpenApiConfig.java         # Swagger UI config
│   └── SecurityConfig.java        # Spring Security 6 configuration
├── common/
│   ├── exception/                 # AppException hierarchy + GlobalExceptionHandler
│   └── security/                  # CurrentUserProvider
├── auth/                          # Register / login / logout
├── user/                          # User entity + AppUserDetailsService
├── category/                      # Category entity, CRUD, CategoryType enum
├── transaction/                   # Transaction entity + CRUD with filtering
├── goal/                          # SavingsGoal entity + CRUD + live progress
└── report/                        # Monthly and yearly report endpoints
```

---

## Assumptions

1. H2 in-memory is used for both dev and prod — data resets on restart. This is per the assignment spec; production would use PostgreSQL.
2. Password rules: minimum 8 characters, at least one letter, at least one digit.
3. Phone validation: optional `+` prefix, 7–15 digits.
4. Report year range: 1900–2100. Month range: 1–12.
5. `startDate` for a goal defaults to today (from the injected `Clock`) when not provided in the request.
6. Transaction `date` is immutable after creation — `PUT /api/transactions/{id}` silently ignores any `date` field.
7. Deleting a category that is referenced by transactions returns 400 (not 204/412).
