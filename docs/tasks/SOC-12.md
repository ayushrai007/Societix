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

## Implementation record

- Added `Role` as the role-name source for token claim conversion, Spring authorities, method
  authorization expressions, membership role assignment, and Firebase claim synchronization.
- Membership role request and response DTOs now use `Role` values. `PLATFORM_ADMIN` remains
  non-assignable to society memberships; security guard membership is restricted to `GUARD`.
- Added tests for every declared role mapping, ignoring unknown role names, a treasurer-only test
  fixture denying an owner even when the request header and body name `TREASURER`, and allowing a
  treasurer claim. The fixture is test-only because no production treasurer-only operation exists yet.
- Role assignment rejects `PLATFORM_ADMIN` and unknown role names over HTTP, and pending membership
  claim synchronization omits the tenant context and society roles.
- Society creation and current account/version enforcement are covered by the SOC-11 onboarding
  authorization tests.

### Acceptance criteria to verification

| Acceptance criterion | Verification |
|---|---|
| All backlog roles are represented; only declared roles from the validated claim become authorities | `FirebaseRoleAuthoritiesConverterTest` |
| Owner cannot use treasurer-only operation; role header cannot grant access | `RoleAuthorizationTest` |
| Pending membership receives no society role or tenant context; invalid role assignments return 400 | `MembershipServiceIT` |
| Only `PLATFORM_ADMIN` can create a society, with current account checks | `SocietyOnboardingIT` and SOC-11 security tests |

### Verification status

- Focused SOC-12 security tests: `./mvnw -B -Dtest=FirebaseRoleAuthoritiesConverterTest,RoleAuthorizationTest test` — passed.
- Full `./mvnw -B verify` is required in CI; see the current PR check for the result.
