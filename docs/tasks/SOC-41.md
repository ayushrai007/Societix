# SOC-41 — Health checks and basic metrics

## Delivery and why

SOC-41 makes the running backend observable without adding a dashboard stack. Spring Boot
Actuator exposes health and request metrics, while console logs use ECS structured JSON so
operational events can be searched by fields instead of parsed from plain text. The error path is
represented in request metrics, and its test checks that a submitted phone-like value is not logged.

## Touched files and modules

- `backend/src/main/resources/application.yml`: configure ECS structured console logging; expose
  the Actuator `health` and `metrics` web endpoints while keeping health details hidden.
- `backend/src/test/java/com/societix/SocietixApplicationTest.java`: verify health, HTTP request
  metrics, ECS JSON logging, and a client error counted in metrics without logging the test phone
  number.
- `docs/standards/getting-started.md`: document how to inspect available metrics and the HTTP 400
  request metric locally, including the intended network restriction for deployed environments.

Spring Boot Actuator was already a project dependency; SOC-41 uses its existing health and
Micrometer instrumentation rather than adding a separate metrics library.

## Verification

Acceptance mapping:

1. `SocietixApplicationTest.actuatorHealthEndpointReturnsUp` proves `/actuator/health` returns
   `UP`; `actuatorMetricsEndpointExposesHttpRequestMetrics` proves the metrics endpoint lists
   `http.server.requests`.
2. `SocietixApplicationTest.applicationLogsAreStructuredJson` parses a captured log line as JSON
   and checks its message. `httpClientErrorIsVisibleInMetricsWithoutLoggingPersonalData` also
   asserts that the test phone number is absent from captured stdout and stderr.
3. `SocietixApplicationTest.httpClientErrorIsVisibleInMetricsWithoutLoggingPersonalData` sends an
   invalid society request, then verifies that the HTTP 400 request count is positive.

Manual local checks:

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/metrics
curl -s 'http://localhost:8080/actuator/metrics/http.server.requests?tag=status:400'
```

Verification status: the application was started locally and `/actuator/health` returned
successfully. The console output observed locally is ECS JSON and includes `@timestamp`, log level,
logger, service name, message, and ECS version. That sample was a warning for a GET request to an
endpoint that does not support GET (HTTP 405), rather than the intentional HTTP 400 case above.
The full automated test suite and the HTTP 400 metrics check have not been reported as run for this
change.

## Configuration and environment

- `/actuator/health` and `/actuator/metrics` are exposed on the same application port, as selected
  for local diagnostics.
- Health details remain hidden (`show-details: never`).
- Console logs use Spring Boot's ECS structured logging format.
- MVC request instrumentation provides the `http.server.requests` metric; a 400 response is
  visible when queried with the `status:400` tag.
- The metrics endpoint has no dedicated management port or security rule in this task. Before
  deployment, restrict it to trusted network access and ensure future authentication and security
  configuration explicitly covers the Actuator endpoints.

## Decisions and reasons

- Expose metrics on the application port so local diagnostics use one port and the upcoming
  authentication work can apply through the application security configuration. The Actuator paths
  still need to be explicitly included in that security configuration; same-port exposure alone
  does not guarantee authentication.
- Keep health details hidden to avoid disclosing internal component details.
- Use ECS JSON logs, consistent with the project's structured logging conventions, without adding
  personal data to log fields.

## Limits and follow-ups

- Metrics currently provide basic in-process diagnostics, not dashboards, alerting, or long-term
  storage.
s- SOC-11/12 now require `PLATFORM_ADMIN` for `/actuator/metrics/**`; health remains public and
  detailed health data stays hidden. Deployed access should still use trusted network controls.
- Run the automated suite on Java 25 with Docker available to verify the testcontainers-backed
  project test setup as well as these SOC-41 tests.
