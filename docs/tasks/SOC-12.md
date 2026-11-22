# SOC-12 — Roles and permissions (RBAC)

## Roles

Define a Java `Role` enum for the backlog roles: `PLATFORM_ADMIN`, `SECRETARY`, `TREASURER`,
`COMMITTEE_MEMBER`, `OWNER`, `TENANT`, `GUARD`, and `AUDITOR`. Society users belong to one society;
role claims are an array to support users who hold more than one approved role. `PLATFORM_ADMIN` is
a global role and does not receive a society tenant context unless separately assigned as a society
user.

## Authorization rules

- Spring authorities are created only from the validated Firebase `roles` custom claim.
- The backend denies by default and applies method-level authorization to protected operations.
- A role in a request header or body is never authoritative. In particular,
  `X-Admin-Role: PLATFORM_ADMIN` must not authorize society creation.
- The society-creation endpoint requires a verified `PLATFORM_ADMIN` role.
- Use the `Role` enum as the single source of role names in claims conversion and authorization
  expressions; do not repeat role-name string literals across production code.
- Authorization uses `tenantId` from the validated token and checks current Societix account status
  and `authorizationVersion`; client-supplied tenant IDs cannot select another society.

## Tests

- All backlog roles are represented in the role model.
- A `TREASURER`-only endpoint returns 403 for an `OWNER` token.
- A caller-supplied role header/body cannot grant privileges.
- A valid `PLATFORM_ADMIN` token can create a society; missing, invalid, or non-admin credentials
  cannot.
