# Society onboarding API

`POST /api/v1/societies` creates one Society tenant. This endpoint is for a platform administrator;
until the SOC-11/SOC-12 identity and RBAC work lands, the admin role is represented by the
`X-Admin-Role: PLATFORM_ADMIN` header.

Request:

```http
POST /api/v1/societies
X-Admin-Role: PLATFORM_ADMIN
Content-Type: application/json

{"name":"Maple Residency","address":"12 MG Road, Pune","state":"MH"}
```

The response is `201 Created`, includes a `Location: /api/v1/societies/{id}` header, and returns
the society `id`, `name`, `address`, and two-letter uppercase `state`. Missing or invalid fields
return `400 application/problem+json` with code `SOC-400-VALIDATION`. A missing admin header returns
`401 application/problem+json`; a non-platform-admin value returns `403 application/problem+json`.
