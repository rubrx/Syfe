# API Contract — Personal Finance Manager

> **Warning:** `.assignment/financial_manager_tests.sh` was **not present** in the repository at planning time.
> All decisions in the "Ambiguities" section are based on the PDF spec and CLAUDE.md guidance.
> They must be re-verified by reading the test script when it becomes available, and confirmed
> by running Phase 8 grading. Entries marked **HIGH risk** are the most likely to need revision.

---

## Error Response Format

Every non-2xx response uses this JSON envelope:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object 'createTransactionRequest'",
  "path": "/api/transactions",
  "fieldErrors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

`fieldErrors` is present **only** on Bean Validation failures (400). All other errors omit it.

---

## 1. Authentication

### POST /api/auth/register

| | |
|---|---|
| Auth required | No |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `username` | String | Required; valid email format |
| `password` | String | Required; ≥8 chars; ≥1 letter; ≥1 digit (`^(?=.*[A-Za-z])(?=.*\d).{8,}$`) |
| `fullName` | String | Required; not blank |
| `phoneNumber` | String | Required; `^\+?[0-9]{7,15}$` |

**Success — 201 Created:**
```json
{ "message": "User registered successfully", "userId": 1 }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Any field fails validation — `fieldErrors` populated |
| 409 | `username` already registered |

---

### POST /api/auth/login

| | |
|---|---|
| Auth required | No |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `username` | String | Required |
| `password` | String | Required |

**Success — 200 OK:**
```json
{ "message": "Login successful" }
```
Sets `JSESSIONID` cookie (`HttpOnly`, `SameSite=Lax`; `Secure` flag added in `prod` profile only).

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Wrong credentials or unknown username |

---

### POST /api/auth/logout

| | |
|---|---|
| Auth required | Yes (session cookie) |

**Request:** No body.

**Success — 200 OK:**
```json
{ "message": "Logout successful" }
```
Session is invalidated; `JSESSIONID` cookie is cleared.

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | No valid session cookie — Spring Security intercepts before the controller |

---

## 2. Categories

### GET /api/categories

| | |
|---|---|
| Auth required | Yes |

**Request:** No body. No query params.

**Success — 200 OK:**
```json
{
  "categories": [
    { "id": 1, "name": "Salary",   "type": "INCOME",  "isCustom": false },
    { "id": 2, "name": "Food",     "type": "EXPENSE", "isCustom": false },
    { "id": 9, "name": "Freelance","type": "INCOME",  "isCustom": true  }
  ]
}
```
Returns all system defaults **plus** the authenticated user's custom categories.
`id` is included to support `categoryId` filtering on `GET /api/transactions` (see Ambiguity #1).

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Not authenticated |

---

### POST /api/categories

| | |
|---|---|
| Auth required | Yes |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `name` | String | Required; not blank |
| `type` | String | Required; must be `INCOME` or `EXPENSE` |

**Success — 201 Created:**
```json
{ "id": 9, "name": "Freelance", "type": "INCOME", "isCustom": true }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Field missing, blank, or `type` is not `INCOME`/`EXPENSE` |
| 401 | Not authenticated |
| 409 | Name already exists as a system default **or** as this user's custom category (case-insensitive — "Food" and "food" are the same; original casing is preserved in storage) |

---

### DELETE /api/categories/{name}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{name}` — URL-encoded category name |

**Success — 200 OK:**
```json
{ "message": "Category deleted successfully" }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Category is currently referenced by one or more transactions (`BusinessValidationException`) |
| 401 | Not authenticated |
| 403 | Category is a system default (`isCustom = false`) — `ForbiddenOperationException` |
| 404 | Category name not found among the user's accessible categories |

---

## 3. Transactions

### POST /api/transactions

| | |
|---|---|
| Auth required | Yes |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `amount` | BigDecimal | Required; > 0 |
| `date` | String (YYYY-MM-DD) | Required; not in the future (checked against injected `Clock`) |
| `category` | String | Required; case-insensitive match against system defaults and the user's custom categories (stored casing is preserved in the response) |
| `description` | String | Optional |

**Success — 201 Created:**
```json
{ "id": 1, "amount": 50000.00, "date": "2024-01-15", "category": "Salary", "description": "January Salary", "type": "INCOME" }
```
`type` is derived from the resolved category — never sent by the client.

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | `amount` ≤ 0; `date` in the future; invalid date format; unknown category name; malformed JSON |
| 401 | Not authenticated |

---

### GET /api/transactions

| | |
|---|---|
| Auth required | Yes |

**Query parameters (all optional):**

| Param | Type | Description |
|-------|------|-------------|
| `startDate` | YYYY-MM-DD | Include only transactions on or after this date |
| `endDate` | YYYY-MM-DD | Include only transactions on or before this date |
| `categoryId` | Long | Filter by category ID — **see Ambiguity #1** |
| `type` | String | Filter by `INCOME` or `EXPENSE` |

Default sort: `date DESC`, then `id DESC` (newest first; stable tie-break).

**Success — 200 OK:**
```json
{
  "transactions": [
    { "id": 1, "amount": 50000.00, "date": "2024-01-15", "category": "Salary", "description": "January Salary", "type": "INCOME" }
  ]
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Not authenticated |

---

### PUT /api/transactions/{id}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{id}` — transaction ID |

**Request body (partial update — only provided fields are changed):**

| Field | Type | Validation |
|-------|------|-----------|
| `amount` | BigDecimal | If provided: > 0 |
| `category` | String | If provided: must match an accessible category |
| `description` | String | Optional |

`date` is immutable. If present in the body it is **silently ignored** — the original date is preserved (see Ambiguity #3).

**Success — 200 OK:**
```json
{ "id": 1, "amount": 60000.00, "date": "2024-01-15", "category": "Salary", "description": "Updated January Salary", "type": "INCOME" }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Provided field value fails validation |
| 401 | Not authenticated |
| 404 | Transaction not found or belongs to another user |

---

### DELETE /api/transactions/{id}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{id}` — transaction ID |

**Success — 200 OK:**
```json
{ "message": "Transaction deleted successfully" }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Not authenticated |
| 404 | Transaction not found or belongs to another user |

---

## 4. Savings Goals

Progress fields are computed on every read via a `SUM ... GROUP BY` repository query, never cached.

Formulas (all `BigDecimal`, scale 2, `HALF_UP`):
- `currentProgress = SUM(income) − SUM(expense)` where `transaction.date >= goal.startDate`
- `progressPercentage = currentProgress / targetAmount × 100`
- `remainingAmount = targetAmount − currentProgress`

Values are returned as-calculated — no clamping. Negative progress and percentages above 100 are valid outputs (see Ambiguity #7).

---

### POST /api/goals

| | |
|---|---|
| Auth required | Yes |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `goalName` | String | Required; not blank |
| `targetAmount` | BigDecimal | Required; > 0 |
| `targetDate` | String (YYYY-MM-DD) | Required; strictly in the future |
| `startDate` | String (YYYY-MM-DD) | Optional; defaults to today (from `Clock`); if provided must be < `targetDate` |

**Success — 201 Created:**
```json
{ "id": 1, "goalName": "Emergency Fund", "targetAmount": 5000.00, "targetDate": "2026-01-01", "startDate": "2025-01-01", "currentProgress": 1000.00, "progressPercentage": 20.00, "remainingAmount": 4000.00 }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | `targetAmount` ≤ 0; `targetDate` not in future; `startDate` ≥ `targetDate`; invalid date format |
| 401 | Not authenticated |

---

### GET /api/goals

| | |
|---|---|
| Auth required | Yes |

**Success — 200 OK:**
```json
{ "goals": [ { "id": 1, "goalName": "Emergency Fund", ... } ] }
```
All of the authenticated user's goals with live progress.

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Not authenticated |

---

### GET /api/goals/{id}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{id}` — goal ID |

**Success — 200 OK:** goal object with live progress.

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | `{id}` is not a valid number (`MethodArgumentTypeMismatchException`) |
| 401 | Not authenticated |
| 403 | Goal exists but belongs to another user (see Ambiguity #4) |
| 404 | Goal does not exist |

---

### PUT /api/goals/{id}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{id}` — goal ID |

**Request body:**

| Field | Type | Validation |
|-------|------|-----------|
| `targetAmount` | BigDecimal | If provided: > 0 |
| `targetDate` | String (YYYY-MM-DD) | If provided: strictly in the future and after `startDate` |

**Success — 200 OK:** updated goal with live progress.

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Provided value fails validation |
| 401 | Not authenticated |
| 403 | Goal belongs to another user |
| 404 | Goal not found |

---

### DELETE /api/goals/{id}

| | |
|---|---|
| Auth required | Yes |
| Path variable | `{id}` — goal ID |

**Success — 200 OK:**
```json
{ "message": "Goal deleted successfully" }
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Not authenticated |
| 403 | Goal belongs to another user |
| 404 | Goal not found |

---

## 5. Reports

`totalIncome` and `totalExpenses` are `Map<String, BigDecimal>` keyed by category name. Only categories with at least one transaction in the period appear. An empty period returns empty maps and `netSavings: 0.00`.

### GET /api/reports/monthly/{year}/{month}

| | |
|---|---|
| Auth required | Yes |

**Path variables:**

| Variable | Validation |
|----------|-----------|
| `year` | Integer; 1900 ≤ year ≤ 2100 |
| `month` | Integer; 1–12 |

**Success — 200 OK:**
```json
{
  "month": 1,
  "year": 2024,
  "totalIncome":   { "Salary": 3000.00, "Freelance": 500.00 },
  "totalExpenses": { "Food": 400.00, "Rent": 1200.00, "Transportation": 200.00 },
  "netSavings": 1700.00
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | `month` outside 1–12, or `year` outside 1900–2100, or non-integer path variable |
| 401 | Not authenticated |

---

### GET /api/reports/yearly/{year}

| | |
|---|---|
| Auth required | Yes |

**Path variables:**

| Variable | Validation |
|----------|-----------|
| `year` | Integer; 1900 ≤ year ≤ 2100 |

**Success — 200 OK:**
```json
{
  "year": 2024,
  "totalIncome":   { "Salary": 36000.00, "Freelance": 6000.00 },
  "totalExpenses": { "Food": 4800.00, "Rent": 14400.00, "Transportation": 2400.00 },
  "netSavings": 20400.00
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | `year` outside 1900–2100 or non-integer path variable |
| 401 | Not authenticated |

---

## Ambiguities Resolved by the Test Script

> The test script was not available. Each entry states the spec text, the PDF evidence, our decision, and the revision risk.

---

### #1 — Category filter parameter name for GET /api/transactions

**Spec text:** "Filter capabilities by date range, category, and transaction type."

**PDF query example:** `?startDate=2024-01-01&endDate=2024-01-31&categoryId=1`

**Problem:** The `GET /api/categories` response example does not include an `id` field. Filtering by `categoryId=1` requires the client to know numeric IDs. The transaction response uses `"category": "Salary"` (a name string), which would make name-based filtering (`?category=Salary`) more natural and consistent.

**Decision:** Follow the PDF literally — use `categoryId` (Long). Add `id` to all category response objects so clients can discover IDs via `GET /api/categories` before filtering.

**Risk: HIGH** — if the test script uses `?category=Salary` (name-based), we rename the param and change its type in Phase 8.

---

### #2 — In-use category delete status code

**Spec text:** "Categories currently referenced by transactions cannot be deleted."

**PDF status list:** `DELETE /api/categories/{name}` → 200, 400, 401, 403, 404.

**Problem:** The spec names the rule but does not say which status code the in-use case maps to.

**Decision:** Return `400 Bad Request` (thrown as `BusinessValidationException`). It is the only plausible status among the listed codes — 403 is reserved for deleting a default, and 404 means not found.

**Risk: LOW.**

---

### #3 — `date` field in Transaction PUT body

**Spec text:** "Users can modify any transaction field except the date field."

**Problem:** If a client sends `"date"` in the PUT body, should the server return 400 or silently ignore it?

**Decision (RESOLVED):** Silently ignore `date` — consistent with the spec's own example PUT body, which only shows `amount` and `description`. The update DTO does not include a `date` field; Jackson discards the key. The response returns the original date unchanged. Exact behaviour when `date` is present in the body will be verified against the test script.

**Risk: MEDIUM** — if the script sends `date` and expects 400, we add a `@JsonIgnoreProperties` violation check.

---

### #4 — Another user's resource: 404 vs 403

**Spec error table:** "403 Forbidden — accessing other user's data."

**PDF status codes:**
- `GET/PUT/DELETE /api/goals/{id}` → includes both 403 and 404.
- `GET/PUT/DELETE /api/transactions/{id}` → includes 404 but **not** 403.

**Decision:**
- **Goals:** Two-step check. If the ID does not exist → 404. If it exists but belongs to someone else → 403.
- **Transactions:** Owner-scoped repository lookup (`findByIdAndUserId`). If not found (for any reason) → 404. No 403 because the spec does not list it for transaction endpoints.

**Risk: LOW** — the spec is internally consistent with this interpretation.

---

### #5 — Password validation rules

**Spec text:** "Secure password meeting system requirements."

**PDF example:** `"password123"` — 11 chars, mixed letters and digits.

**Decision:** Minimum 8 characters, at least one letter, at least one digit.
Pattern: `^(?=.*[A-Za-z])(?=.*\d).{8,}$`

**Risk: MEDIUM** — the script may test edge cases (e.g., 7-char passwords, all-digit passwords) that reveal a different threshold. We adjust in Phase 8.

---

### #6 — Phone number validation rules

**Spec text:** "Valid contact number."

**PDF example:** `"+1234567890"` — optional `+` prefix, 10 digits.

**Decision:** Accept an optional `+` followed by 7–15 digits.
Pattern: `^\+?[0-9]{7,15}$`

**Risk: MEDIUM** — same reasoning as password rules.

---

### #7 — Negative or >100% goal progress

**Spec text:** `currentProgress = (Total Income - Total Expenses) since goal start date`

**Problem:** If expenses exceed income, `currentProgress` is negative, making `progressPercentage` negative and `remainingAmount` greater than `targetAmount`. If income exceeds the target, `progressPercentage` exceeds 100 and `remainingAmount` is negative.

**Decision:** Return all values as mathematically calculated — no clamping, no special-casing.

**Risk: LOW** — the spec is silent on clamping, and the formula is unambiguous.

---

### #8 — Logout without a valid session

**Spec:** `POST /api/auth/logout` → 200 OK, 401 Unauthorized.

**Decision:** Configure `/api/auth/logout` as a protected endpoint. An unauthenticated request is intercepted by Spring Security before reaching the controller; our custom `AuthenticationEntryPoint` returns 401 JSON. This makes the behavior consistent with every other protected endpoint.

**Risk: LOW.**

---

### #9 — Custom category name clashing with a default name

**CLAUDE.md:** "Custom category names are unique per user. Clashing with a default name is also a conflict (409)."

**Decision (RESOLVED):** Case-insensitive uniqueness; original casing is preserved in storage and responses. When creating a custom category, check:
1. No system default has the same name (case-insensitive `UPPER(name)` comparison).
2. No existing custom category for this user has the same name (case-insensitive).

Either failure → 409 Conflict. Category lookup when creating transactions is also case-insensitive (for consistency — a user typing "salary" still resolves to "Salary").

**Risk: LOW** — explicitly confirmed by the user.

---

## Open Questions Needing Your Decision

1. **Test script:** `.assignment/financial_manager_tests.sh` is missing. The category filter param name (`categoryId` vs `category`) and exact date-in-PUT-body behaviour cannot be locked until the script is read. Phase 3 will not start until the script is present.

2. **Category filter param (`categoryId` vs `category`):** Deferred until the test script is available. Category responses expose `id` as a harmless extra field. Implementation will match whichever param the script uses.

3. **Year range for reports:** Chosen as 1900–2100. Adjust if the test script uses a different range.

> Resolved: category name uniqueness is case-insensitive (original casing preserved). Transaction PUT uses partial-update semantics.
