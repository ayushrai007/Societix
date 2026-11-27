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

- The membership onboarding API uses Redis fixed-window limits: 60 requests per source IP per
  minute across onboarding routes, plus 5 membership submissions per Firebase UID per hour. The
  limits are configurable through `SOCIETIX_ONBOARDING_REQUESTS_PER_IP_PER_MINUTE` and
  `SOCIETIX_MEMBERSHIP_REQUESTS_PER_USER_PER_HOUR`.
- Redis failures fail closed with `503`; a reached limit returns `429` and `Retry-After`. Redis keys
  contain SHA-256 digests of the IP or Firebase UID, not phone numbers, tokens, or raw identifiers.
- These API limits do not cap OTP SMS sends: the Firebase client SDK sends those requests directly to
  Firebase. Firebase project SMS policy and sending quotas remain the OTP-send controls.
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
- **Before production:** configure Redis and the ingress so client IPs cannot be spoofed. The
  application currently uses the socket peer address; a deployed ingress must prevent direct public
  access to the backend and forward traffic through the trusted edge.
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
