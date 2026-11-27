# Society onboarding API

`POST /api/v1/societies` creates one Society tenant. This endpoint requires a verified Firebase ID
token whose Societix `roles` custom claim contains `PLATFORM_ADMIN`. The caller-controlled
`X-Admin-Role` header is ignored and must never be used as authentication or authorization.

Society creation and metrics stay disabled until SOC-11 checks the administrator's current account
status and authorization version against Societix data on every request. Do not enable
`SOCIETIX_SOCIETY_CREATION_ENABLED` before that check is deployed.

Request:

```http
POST /api/v1/societies
Authorization: Bearer <Firebase ID token>
Content-Type: application/json

{"name":"Maple Residency","address":"12 MG Road, Pune","state":"MH"}
```

Societix validates the Firebase token signature, issuer, audience, and expiry. The Firebase `sub`
remains the Firebase UID; Societix custom claims use `userId`, `tenantId`, `roles`, and
`authorizationVersion`. Claims are set by Societix's trusted backend only. The response is
`201 Created`, includes a `Location: /api/v1/societies/{id}` header, and returns the society `id`,
`name`, `address`, and two-letter uppercase `state`. Missing or invalid fields return
`400 application/problem+json` with code `SOC-400-VALIDATION`. Missing/invalid credentials return
`401`; an authenticated caller without `PLATFORM_ADMIN` returns `403`.

The first platform administrator must be provisioned through a restricted operator process. There
is no public endpoint or request header that can grant the platform administrator role.
