# API Guidelines — REST Design Rules

**Document:** `SOC-DOC-API` · **Status:** Standard (prescriptive)

The design rules every Societix HTTP API must follow. The concrete endpoint catalogue and webhook
contracts live in [`../04-api-specification.md`](../04-api-specification.md), which references this
document for conventions. **OpenAPI 3.1 is the source of truth** — these rules describe how we write
that spec and the controllers behind it. Error model and tenancy align with
[`../01-architecture.md`](../01-architecture.md) and [`coding-standards.md`](coding-standards.md).

---

## 1. Base path & versioning

- All endpoints live under **`/api/v1`**. The major version is in the **path**, never a header or
  query param.
- **v1 is stable**: additive changes only (new optional fields, new endpoints). A breaking change
  means `/api/v2`, run in parallel with a documented deprecation window (min 6 months). Deprecated
  fields/endpoints carry a `Deprecation` and `Sunset` response header.
- The OpenAPI 3.1 document is published at `/v3/api-docs` and rendered at `/swagger-ui.html`. A PR
  that changes an endpoint **must** change the spec in the same PR; a contract test fails otherwise.

## 2. Resource naming

- Resources are **plural nouns**: `/invoices`, `/payouts`, `/units`, `/resolutions`, `/gate-passes`.
- Nest only to express ownership, max one level: `/agms/{agmId}/resolutions`. Beyond that, use a
  top-level resource with a filter.
- **No verbs in paths.** Express actions as sub-resources or state transitions:
  `POST /payouts/{id}/approval` (not `/approvePayout`). `kebab-case` for multi-word paths.
- Identifiers in paths are opaque UUIDs. Never put `tenant_id` in the path — tenancy comes from the
  JWT (§8).

## 3. HTTP verbs & status codes

| Verb | Use | Idempotent | Typical success |
|---|---|---|---|
| `GET` | read a resource/collection | yes | `200` |
| `POST` | create; non-idempotent action | no | `201` (create, with `Location`) / `200`/`202` |
| `PUT` | full replace of a known resource | yes | `200` / `204` |
| `PATCH` | partial update (JSON Merge Patch) | no | `200` |
| `DELETE` | remove / soft-delete | yes | `204` |

Status code rules:

| Code | When |
|---|---|
| `200 OK` | successful read/update with a body |
| `201 Created` | resource created; include `Location` header |
| `202 Accepted` | async accepted (e.g. queued cadence/notification) |
| `204 No Content` | success, no body (DELETE, empty PUT) |
| `400 Bad Request` | malformed syntax / validation failure |
| `401 Unauthorized` | missing/invalid token |
| `403 Forbidden` | authenticated but not permitted (RBAC) |
| `404 Not Found` | resource absent **or** not visible to this tenant (fail-closed; never reveal cross-tenant existence) |
| `409 Conflict` | state conflict / idempotency-key reuse with different payload |
| `422 Unprocessable Entity` | syntactically valid but business-rule rejected |
| `429 Too Many Requests` | rate limit exceeded (with `Retry-After`) |
| `500` | unexpected server fault (never for client errors) |

## 4. Request / response envelopes

- **Single resources are returned bare** (the object itself), not wrapped.
- **Collections are wrapped** in an envelope with `data` + `page` metadata (see §5). This keeps
  pagination metadata out of the item shape.
- Field names are `lowerCamelCase`. Timestamps are ISO-8601 UTC with `Z`. Enums are `UPPER_SNAKE`
  strings.
- Unknown request fields are rejected (`400`) — no silent drop.

## 5. Pagination, filtering, sorting

**Cursor pagination is the default** for large/forward-only collections (ledger entries, audit log,
gate visits). **Offset/page pagination** is allowed for small, bounded admin lists.

Query params:

| Param | Meaning | Example |
|---|---|---|
| `cursor` | opaque cursor from a prior response | `cursor=eyJpZCI6...` |
| `limit` | page size, default 20, max 100 | `limit=50` |
| `page` / `size` | offset style (small lists only) | `page=2&size=25` |
| `sort` | `field` or `field:asc`/`field:desc`, comma-separated | `sort=dueDate:desc,amount:asc` |
| filters | explicit named params (never free-form) | `status=OVERDUE&from=2026-01-01` |

Collection response envelope:

```json
{
  "data": [ { "id": "…", "status": "OVERDUE" } ],
  "page": {
    "limit": 20,
    "nextCursor": "eyJpZCI6IjAxSFguLi4ifQ",
    "hasMore": true
  }
}
```

- `nextCursor` is `null` and `hasMore` is `false` on the last page.
- Cursors are opaque (base64 of the sort key + id); clients must not parse them.
- Filtering is **allow-listed per endpoint** in the OpenAPI spec — no arbitrary query languages.

## 6. Error model — RFC 9457 problem+json

Every error (4xx/5xx) returns `Content-Type: application/problem+json` per **RFC 9457**, plus an
application **error code** from the catalogue.

```json
{
  "type": "https://errors.societix.dev/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "amountMinor must be positive",
  "instance": "/api/v1/invoices",
  "code": "SOC-400-VALIDATION",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "errors": [
    { "field": "amountMinor", "message": "must be greater than 0" }
  ]
}
```

Required members: `type`, `title`, `status`, `detail`, `code`, `traceId`. `errors[]` is included for
field-level validation. **Never** leak stack traces, SQL, or PII in any member.

### App error-code catalogue (`SOC-<HTTP>-<SLUG>`)

| Code | HTTP | Meaning |
|---|---|---|
| `SOC-400-VALIDATION` | 400 | Request failed Jakarta Validation |
| `SOC-401-UNAUTHENTICATED` | 401 | Missing/invalid/expired JWT |
| `SOC-403-FORBIDDEN` | 403 | RBAC denies the action for this role |
| `SOC-404-RESOURCE` | 404 | Resource not found / not in tenant scope |
| `SOC-409-IDEMPOTENCY-CONFLICT` | 409 | `Idempotency-Key` reused with a different payload |
| `SOC-409-STATE` | 409 | Illegal state transition (e.g. approve a sent payout) |
| `SOC-422-MAKER-CHECKER` | 422 | Maker equals checker on a payout |
| `SOC-429-RATE-LIMIT` | 429 | Rate limit exceeded |
| `SOC-500-INTERNAL` | 500 | Unexpected fault (opaque to client) |

New codes are added to this table in the same PR that introduces them.

## 7. Idempotency for unsafe money operations

Any **non-idempotent money-moving or externally-visible** `POST` (raise payout, initiate
collection, trigger a notification batch) **must** accept an **`Idempotency-Key`** header (a
client-generated UUID).

- The server stores the key → first response in **Redis** (TTL 24h; see canonical stack).
- Same key + **same** request body → the original response is replayed (no duplicate side effect).
- Same key + **different** body → `409 SOC-409-IDEMPOTENCY-CONFLICT`.
- Webhooks from the Payment Aggregator are deduplicated the same way (keyed on the PA event id), so
  at-least-once delivery never double-posts to the ledger.

```http
POST /api/v1/payouts HTTP/1.1
Authorization: Bearer <jwt>
Idempotency-Key: 0b5f3d6a-1e2c-4a9b-9f1a-7c2e5b8d0a11
Content-Type: application/json
```

## 8. Authentication & tenant resolution

- **Bearer JWT** (OIDC) in `Authorization: Bearer <token>`. No cookies, no session state (stateless
  pods).
- The token carries `sub`, `tenant_id`, and `roles`. **The `tenant_id` is taken from the verified
  token, never from the client body or path.** The gateway/tenancy filter sets the request's
  `app.tenant_id` GUC inside the transaction (RLS — see
  [`../01-architecture.md`](../01-architecture.md) §6).
- Authorization is **role-based** (`SECRETARY`, `TREASURER`, `RESIDENT`, `GUARD`, `AUDITOR`);
  enforced with method security. A denied action is `403 SOC-403-FORBIDDEN`.

## 9. Rate limiting

- Per-tenant and per-principal token-bucket limits enforced at the gateway using **Redis**.
- On breach: `429` + `Retry-After` (seconds) + `SOC-429-RATE-LIMIT`. Successful responses to
  limited endpoints may include `RateLimit-Remaining`/`RateLimit-Reset` headers.

## 10. HATEOAS stance

- **Pragmatic, not strict HAL.** We do **not** require hypermedia link graphs. Clients navigate by
  the documented OpenAPI spec and stable resource paths.
- Where a next action is state-dependent (e.g. a payout awaiting a checker), responses **may** carry
  a lightweight `_actions` object listing permitted transitions. This is optional and advisory.

## 11. Date & money formats

- **Dates/times:** ISO-8601. Instants are UTC with `Z` (`2026-10-27T09:30:00Z`). Calendar dates are
  `YYYY-MM-DD`. Never a locale string, never epoch millis on the wire.
- **Money:** always **integer minor units** (paise) in JSON, with an explicit currency, e.g.
  `{"amountMinor": 250000, "currency": "INR"}` = ₹2,500.00. **Never** floats for money.
  Server-side, money is `BigDecimal`/`numeric(14,2)` in the ledger (see
  [`../03-data-model.md`](../03-data-model.md) §2). The `amountMinor`↔`BigDecimal` conversion is
  centralized in `platform`.

## 12. OpenAPI 3.1 as source of truth

- The spec is generated from annotated controllers/DTOs and committed to the repo.
- Contract tests (CI) assert the running app matches the spec; the web/mobile clients are generated
  from it. A behavior change without a spec change fails CI.
- Examples (success **and** problem+json error) are required on every operation in the spec.

### Canonical examples

**Success — `GET /api/v1/invoices/{id}` → `200`:**

```json
{
  "id": "01HX6Z0X4Q2E8K3N7W9B5A2C1D",
  "unitCode": "A-101",
  "amountMinor": 250000,
  "currency": "INR",
  "dueDate": "2026-11-05",
  "status": "OVERDUE",
  "raisedAt": "2026-10-01T04:00:00Z"
}
```

**Error — same request, unknown id → `404 application/problem+json`:**

```json
{
  "type": "https://errors.societix.dev/not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "No invoice with id 01HX6Z0X4Q2E8K3N7W9B5A2C1D",
  "instance": "/api/v1/invoices/01HX6Z0X4Q2E8K3N7W9B5A2C1D",
  "code": "SOC-404-RESOURCE",
  "traceId": "0af7651916cd43dd8448eb211c80319c"
}
```

---

**Related:** [`../04-api-specification.md`](../04-api-specification.md) ·
[`coding-standards.md`](coding-standards.md) · [`getting-started.md`](getting-started.md) ·
[`../01-architecture.md`](../01-architecture.md) · [`../03-data-model.md`](../03-data-model.md)
