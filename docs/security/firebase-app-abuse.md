# Firebase authentication and app-abuse defenses

Firebase Authentication proves control of a phone number and signs the Firebase ID token. It does
not prove that a request came from the official Societix app, and it does not stop an authenticated
user from overusing Societix APIs. Use layered controls.

## Phone OTP abuse

The client Firebase SDK initiates phone verification directly with Firebase. A Spring Boot rate
limiter therefore cannot cap SMS sends by itself. Configure protections in Firebase:

- Allow SMS only to the regions Societix serves; initially allow India only.
- Keep Firebase's built-in app verification and per-number/project sending protections enabled.
- Link a Cloud Billing account before enabling Firebase SMS. Configure budget and quota alerts, and
  set an SMS quota where the Firebase/Google Cloud project exposes that control. A budget alert is a
  notification, not necessarily a hard spending cap.
- Monitor SMS sends, verification completions, costs, and quota errors. Investigate sudden increases
  and sustained high send-to-verification ratios.
- App Check is not part of the initial SOC-11/12 design. Reassess it only if abuse monitoring shows
  Firebase's built-in protections and the Societix API controls are insufficient.

These controls raise the cost of SMS pumping but cannot guarantee that every SMS is delivered or
that abuse is impossible.

## Societix API abuse

- Apply Redis-backed limits at the gateway/backend by IP before authentication and by verified
  Firebase UID after authentication. Use stricter limits for onboarding, membership requests, and
  other expensive actions. For pre-authentication keys derived from phone numbers, store an HMAC
  rather than the raw number.
- The current API has no public login/OTP endpoint because the Firebase client SDK talks directly to
  Firebase. Any future public Societix endpoint (for example, a membership-request bootstrap route)
  must receive an explicit IP-based limit before it is released. Firebase SMS controls remain the
  actual OTP-send limits; a Societix API limiter cannot see client-to-Firebase SMS requests.
- Keep authorization separate: every protected request still requires a valid Firebase ID token,
  approved Societix account, current account status, current `authorizationVersion`, and the
  appropriate role. App Check is not authentication or authorization.
- Add request-size/time limits at the ingress, generic error responses that do not reveal whether a
  phone is registered, and alerts for rejected tokens, rate-limit hits, SMS spend, and unusual
  per-account activity.

## Delivery and operations

- **Before enabling Firebase Phone Auth in a deployed project (SOC-11):** link Cloud Billing, restrict
  allowed SMS regions to India, confirm app verification is enabled, review Firebase quotas, and
  create billing and SMS-volume alerts. These are Firebase project settings, not Spring configuration.
- **When public unauthenticated API routes are introduced (SOC-11/12 or the route's owning task):**
  add Redis-backed IP limits and tests for the configured limit and `429` response before exposing
  the route. Configure trusted proxy handling at the ingress; do not trust arbitrary forwarded-IP
  headers.
- **Before production launch:** export Actuator/Micrometer metrics to the chosen monitoring service,
  create dashboards and alerts for SMS spend/volume, HTTP 401/403/429 rates, API error/latency rates,
  Firebase token verification failures, and Redis availability. Route alerts to an on-call owner and
  document a response/runbook. Local Actuator metrics alone do not provide durable monitoring.
- **During SOC-11/12 implementation:** add structured counters for rate-limit decisions and auth
  outcomes without recording OTPs, ID tokens, or raw phone numbers. Use a keyed HMAC for phone-based
  correlation where needed.

Keep per-user and per-IP limits as independent controls. Reassess App Check only if monitoring
shows Firebase's built-in protections and Societix API controls are insufficient.

## References

- [Firebase Phone Authentication: app verification and SMS region policy](https://firebase.google.com/docs/auth/android/phone-auth)
- [Firebase Authentication limits](https://firebase.google.com/docs/auth/limits)
- [Firebase Authentication troubleshooting, billing, and SMS region policy](https://firebase.google.com/docs/auth/faq-and-troubleshooting)
