# Syfe Backend Intern Assignment: Personal Finance Manager

Project rules. Read this at the start of every session and follow it strictly.

## Sources of truth (priority order)
1. `.assignment/financial_manager_tests.sh` is the grader. Its expected status codes, JSON field names, query params, cookie handling and error cases override everything else.
2. `.assignment/assignment.pdf` is the spec.
3. This file.

When the script and the spec disagree, follow the script. Record the decision in the README under "Design decisions".

## Stack (fixed, do not change)
- Java 17, Spring Boot **3.x** (never upgrade to 4.x), Maven wrapper (`./mvnw`)
- starter-web, starter-security, starter-data-jpa, starter-validation, H2 (in-memory)
- JUnit 5, Mockito, spring-security-test, JaCoCo
- springdoc-openapi (Swagger UI) is the only extra dependency allowed
- No Lombok: DTOs are Java records, entities are plain classes
- Out of scope: JWT, Redis, Postgres, frontend, Kafka, caching, rate limiting, and anything else the spec doesn't ask for

## Architecture
Package by feature, with layers inside each feature:

```
com.rubrangso.finance
├── FinanceManagerApplication
├── config/         SecurityConfig, OpenApiConfig, ClockConfig, DefaultCategorySeeder
├── common/
│   ├── exception/  domain exceptions, GlobalExceptionHandler (@RestControllerAdvice), ErrorResponse
│   └── security/   CurrentUserProvider (resolves the logged-in User from the SecurityContext)
├── auth/           AuthController, AuthService, dto/
├── user/           User, UserRepository, AppUserDetailsService
├── category/       Category, CategoryType, CategoryController, CategoryService, CategoryRepository, dto/
├── transaction/    Transaction, TransactionController, TransactionService, TransactionRepository, dto/
├── goal/           SavingsGoal, GoalController, GoalService, GoalRepository, dto/
└── report/         ReportController, ReportService, dto/
```

Rules:
- Calls go Controller → Service → Repository only. Controllers stay thin: validate, delegate, set the status. Controllers never touch repositories.
- Entities never leave the service layer. Each response DTO has a `static from(Entity)` factory or a small mapper.
- Use constructor injection with `final` fields. No field `@Autowired`.
- Money is `BigDecimal` only, stored as `NUMERIC(19,2)`, scale 2, `RoundingMode.HALF_UP`. Never `double`.
- Dates are `LocalDate`. "Today" comes from an injected `Clock` bean so tests are deterministic. Never call `LocalDate.now()` without the clock.
- Every data access is scoped to the owner at the repository level (`findByIdAndUserId`, etc.). Another user's resource behaves exactly as the script expects (404 vs 403, check it).
- Aggregations (goal progress, reports) are done with `SUM ... GROUP BY` queries in the repository, not Java loops over all rows.
- Externalize config in `application.yml` with `dev` (default) and `prod` profiles. `server.port=${PORT:8080}`.
- JavaDoc goes on every public class and every public method in controllers and services. Explain intent and constraints, not the obvious.
- No commented-out code, leftover TODOs, `System.out` or unused imports. Use SLF4J logging, sparingly.

## Error handling (no 5xx for any known scenario)
Every error uses one JSON shape:
`{ "timestamp", "status", "error", "message", "path", "fieldErrors": [{ "field", "message" }] }` (fieldErrors only on validation failures).

`GlobalExceptionHandler` must handle:
- `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `ConstraintViolationException` → 400
- `HttpMessageNotReadableException` (malformed JSON, bad date format, bad enum) → 400
- `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` → 400
- `NoResourceFoundException` → 404
- `HttpRequestMethodNotSupportedException` → 405
- Domain exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `ForbiddenOperationException`, `BusinessValidationException`) → 404 / 409 / 403 / 400
- `DataIntegrityViolationException` → 409 as a safety net
- `Exception` → 500, logged. This should never trigger in the test script.

## Security (known gotchas)
- Use session-based auth (HttpSession / `JSESSIONID`). No JWT.
- Login is a JSON controller: authenticate via `AuthenticationManager`, then **explicitly** save the `SecurityContext` with `HttpSessionSecurityContextRepository`. Spring Security 6 does not do this automatically. Change the session ID on login to prevent session fixation.
- Add a custom `AuthenticationEntryPoint` that returns 401 JSON. Without it Spring can answer 403 for unauthenticated calls. Add a custom `AccessDeniedHandler` that returns 403 JSON.
- Logout invalidates the session, clears the cookie and returns 200 JSON. Logout with no valid session returns 401.
- CSRF is disabled because this is a stateless-client JSON API. Justify it in the README and mention the SameSite cookie.
- Cookies are `HttpOnly` and `SameSite=Lax`, plus `Secure` in the `prod` profile only. Set `server.forward-headers-strategy=framework` because Render terminates TLS at its proxy.
- Passwords are hashed with BCrypt. Password and phone rules must match what the script treats as valid or invalid.
- `permitAll` applies only to register, login, Swagger UI and the OpenAPI docs.

## Domain rules (verify each against the script)
- **Categories:** defaults are seeded once at startup as global rows (`user_id = null`, `custom = false`): Salary (INCOME); Food, Rent, Transportation, Entertainment, Healthcare, Utilities (EXPENSE).
  - Custom category names are unique per user. Clashing with a default name is also a conflict (409).
  - Deleting a default category → 403. Deleting a category referenced by transactions → status per the script (spec suggests 400). Unknown name → 404.
  - The JSON field must be exactly `isCustom`. Jackson strips `is` from booleans, so use `@JsonProperty("isCustom")`.
- **Transactions:**
  - `amount > 0`. `date` is required and not in the future (use the Clock).
  - `category` is a name resolved among the defaults plus the user's custom categories.
  - `type` is derived from the category, never sent by the client.
  - GET filters: `startDate`, `endDate`, category (check whether the script uses `categoryId` or `category`), `type`. Sort by date desc, then id desc.
  - PUT may change any field except `date`. Handle a `date` in the PUT body exactly as the script expects.
  - Delete is a hard delete, so it drops out of goals and reports automatically.
- **Goals:**
  - `targetAmount > 0`. `targetDate` is strictly in the future. `startDate` defaults to today and must be before `targetDate`.
  - `currentProgress` = income − expenses with `date >= startDate`.
  - `progressPercentage` = progress / target × 100, at 2 decimals.
  - `remainingAmount` = target − progress. Check how the script expects negative or over-100% values.
  - PUT updates only `targetAmount` and `targetDate`.
- **Reports:**
  - `month` must be 1–12 and `year` must be sane, otherwise 400.
  - `totalIncome` and `totalExpenses` are maps keyed by category name, containing only categories with data.
  - `netSavings` = income − expenses. Empty periods return empty maps and `0.00`.

## Testing
- Write service unit tests with Mockito for every service. This is the bulk of the coverage.
- Write `@WebMvcTest` slices per controller with security enabled, covering status codes and validation.
- Write one `@SpringBootTest` + MockMvc integration flow: register → login → use cookie → CRUD → logout → 401.
- JaCoCo reports from Phase 1. The **80% line coverage check** is added to `verify` in the hardening phase. Exclude only the main application class.
- Test names follow `method_condition_expectedResult` or use a `@DisplayName`. Use Arrange / Act / Assert.

## Git workflow
- Use Conventional Commits: `feat(transaction): add date-range filtering`, `test(goal): ...`, `fix(auth): ...`, `docs: ...`, `chore: ...`, `refactor: ...`.
- One commit per logical unit, several per phase. Never one big dump.
- Every commit compiles and passes `./mvnw verify`.
- Subjects are imperative and ≤ 72 chars. Add a body only when the "why" isn't obvious.
- Never push, force-push, amend pushed commits, or alter commit dates. I push myself.
- Tag `v1.0.0` only when I say we're submitting.

## Working style
- Work strictly phase by phase, following the plan in `docs/plan.md`.
- **Before** writing a phase, state each non-obvious design decision in 2–3 lines so I can defend it in the interview.
- **After** a phase:
  1. Run `./mvnw verify`.
  2. Show the result, the coverage %, and `git log --oneline` for the new commits.
  3. Stop and wait for me to say `next`.
- If something in the spec or script is ambiguous and the script doesn't settle it, ask me. Don't guess silently.
- No features beyond the spec. Polish means correctness, consistency and clarity, not extras.