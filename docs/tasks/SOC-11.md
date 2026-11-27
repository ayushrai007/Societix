# SOC-11 — User login with Firebase Phone Auth

## Decision

- Firebase Phone Authentication sends/verifies OTP and issues the Firebase ID token. Societix accepts
  that ID token directly as the API bearer token; there is no second Societix-issued token.
- Firebase's `sub` is the Firebase UID. Societix maps it to its internal user record and adds only
  the custom claims `userId`, `tenantId`, `roles`, and `authorizationVersion`.
- The backend requires a Firebase project ID and derives its expected issuer from that ID. It
  validates token signature, issuer, audience, expiry, non-empty `sub`, and numeric `auth_time`. It
  never accepts identity,
  society, or role claims from request headers or bodies.
- A phone-verified user may request membership in exactly one society. The request remains pending
  until that society's admin approves it and assigns roles. Pending accounts cannot call protected
  society APIs.
- The initial `PLATFORM_ADMIN` must be provisioned by a restricted operator process.

## Security and operations

- Persist account approval and a claims-sync event in one Postgres transaction. An idempotent outbox
  handler writes the complete Societix custom-claims object through Firebase Admin SDK.
- Increment `authorizationVersion` when approval, suspension, society assignment, or roles change.
  Compare token and database versions on protected requests; reject stale tokens until the client
  force-refreshes its Firebase ID token.
- Keep current account status available for checks before establishing tenant-scoped database work.
- Before enabling Phone Auth in a deployed Firebase project, restrict SMS regions to India, retain
  built-in app verification, link Cloud Billing, review SMS quotas, and configure spend/volume alerts.
  These are Firebase project operations; the backend cannot rate-limit client-to-Firebase SMS
  requests.
- Add Redis-backed IP rate limits to any public Societix endpoint before exposing it. Apply user
  limits after token validation. App Check is deferred unless abuse monitoring demonstrates a need.
- Firebase Admin credentials are deployment secrets or workload identity, never repository files.
- Never log OTPs, ID tokens, or raw phone numbers.
- Follow [Firebase authentication and app-abuse defenses](../security/firebase-app-abuse.md) for
  Firebase SMS controls, Redis-backed API limits, and monitoring/alerting rollout.

## Tests

- Valid Firebase identity is mapped to the Societix user and trusted claims.
- Expired, malformed, wrong-issuer, wrong-audience, missing-subject, and missing/invalid-auth-time
  ID tokens are rejected by the actual Firebase token decoder.
- Pending and suspended users are denied protected APIs.
- A stale authorization version is rejected; updated claims become usable after token refresh.
- Caller-supplied tenant/role fields and `X-Admin-Role` cannot grant access.
