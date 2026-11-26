# Membership onboarding API

All endpoints require a valid Firebase ID token. The membership application endpoints also accept
users who have not yet been approved by a society. They accept no phone number, user ID, tenant ID,
or roles from request bodies; identity and verified contact data come from the signed token.

## Request membership

```http
POST /api/v1/onboarding/membership-requests
Authorization: Bearer <Firebase ID token>
Content-Type: application/json

{"societyId":"<society UUID>","membershipType":"RESIDENT"}
```

`membershipType` is `RESIDENT` or `SECURITY_GUARD`. The Firebase token must contain the verified
`phone_number` claim, which is encrypted before being stored as Societix contact data. A successful
request returns `202 Accepted` and a `PENDING` membership. Only one pending or approved society
membership may exist for a Firebase identity. Rejected applications remain as history and may
reapply to another society.

## Check onboarding and token refresh status

```http
GET /api/v1/onboarding/status?societyId=<society UUID>
Authorization: Bearer <Firebase ID token>
```

The response reports the membership status, whether the Firebase custom-claims update is still
pending, and whether this token's `authorizationVersion` differs from the current membership
version. When claims sync is complete and `tokenRefreshRequired` is true, the client must force
refresh its Firebase ID token before calling protected society APIs.

## First secretary bootstrap

Only an active `PLATFORM_ADMIN` can list resident applicants for a society's first secretary and
appoint that first secretary:

- `GET /api/v1/societies/{societyId}/first-secretary-candidates`
- `POST /api/v1/societies/{societyId}/membership-requests/{membershipId}/appoint-first-secretary`

This one-time action approves the selected resident and assigns only `SECRETARY`. The database
records that the initial appointment has happened; later platform-admin attempts are rejected.

## Secretary operations

The current society `SECRETARY` can:

- `GET /api/v1/membership-requests/pending`
- `GET /api/v1/memberships` to list approved and suspended users with their membership IDs and
  contact numbers
- `POST /api/v1/membership-requests/{membershipId}/approve` with `{"roles":["OWNER"]}` for a
  resident or `{"roles":["GUARD"]}` for a security guard
- `POST /api/v1/membership-requests/{membershipId}/reject`
- `PUT /api/v1/memberships/{membershipId}/roles` with the complete replacement role list
- `PUT /api/v1/memberships/{membershipId}/suspension` with `{"suspended":true}` or `false`

Every approval, role change, and suspension change increments `authorizationVersion` and appends a
claims-sync event in the same Postgres transaction. Protected requests compare the signed token to
the current RLS-scoped membership row, so an old token is rejected immediately. Role names are
centralized temporarily for SOC-11; SOC-12 will replace them with its `Role` enum.

## Contact number update

After a user verifies a replacement phone number with Firebase, the client calls
`PUT /api/v1/me/contact-phone` with the refreshed Firebase ID token and no body. Societix reads the
new verified `phone_number` claim and updates the encrypted contact field. A client cannot set an
arbitrary contact number.

## Rate limits

Onboarding requests use Redis fixed-window limits: 60 requests per source IP per minute and 5
membership submissions per Firebase UID per hour. A limit returns `429 Too Many Requests`; an
unavailable Redis returns `503` so onboarding fails closed. These limits protect Societix APIs; SMS
sending limits must be configured in Firebase because the client sends OTP requests directly there.
