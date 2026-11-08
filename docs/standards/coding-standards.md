# Coding Standards — Java / Spring

**Document:** `SOC-DOC-CS` · **Status:** Standard (prescriptive)

Conventions for the Societix backend (Java 25, Spring Boot 4.1.2, Spring Modulith 2.1.2). A reviewer may
block a PR that violates these. Architecture: [`../01-architecture.md`](../01-architecture.md).
Multi-tenancy/RLS: [`../03-data-model.md`](../03-data-model.md) §3. API rules:
[`api-guidelines.md`](api-guidelines.md).

---

## 1. Package-by-module layout (Spring Modulith)

Root package: **`com.societix`**. One top-level package per Modulith module. **Package by module,
then by layer** — never a global `controllers/`, `services/`, `repositories/` split.

```
com.societix
├── treasury      # M1  Treasury / Billing / Double-entry ERP
├── governance    # M2  Statutory governance & e-AGMs
├── copilot       # M3  AI Committee Copilot (RAG)
├── gate          # M4  Offline gate infrastructure / sync
├── privacy       # M5  DPDP Privacy Shield
├── migration     # M6  Smart-Switch migration / ETL
└── platform      # shared kernel: tenancy, security, errors, audit, outbox, config
```

Each module follows the same internal shape:

```
com.societix.treasury
├── TreasuryModule.java        # optional @ApplicationModule metadata
├── api/                       # PUBLIC: events, DTOs, Spring beans other modules may use
│   ├── TreasuryEvents.java    # published event records (invoice/payment)
│   └── TreasuryFacade.java    # the only cross-module entry point
├── web/                       # @RestController, request/response records
├── domain/                    # entities, value objects, domain services (business rules)
├── internal/                  # package-private wiring, mappers, helpers
└── persistence/               # @Repository, JPA entities, query objects
```

### Module API vs internal rules

- **`api/` is the contract.** Other modules may depend only on types in another module's `api/`
  package (and on published events). Everything else is internal.
- All other packages are **module-internal** and must not be imported across module boundaries.
  Spring Modulith's `ApplicationModules.verify()` runs in a test (`ModularityTests`) and **fails the
  build** on a boundary violation. Do not add `@SuppressWarnings`-style escapes.
- **Cross-module calls are either a published event (preferred, async) or a `*Facade` bean call
  (sync).** No reaching into another module's repository or domain object.
- `platform` is the shared kernel: tenancy filter, security, the problem+json model, audit, the
  transactional outbox. Any module may depend on `platform`; `platform` depends on no feature module.

```java
// GOOD — governance reacts to a treasury event, no code coupling
@ApplicationModuleListener                       // async, transactional, after commit
void on(TreasuryEvents.InvoiceSettled event) { ... }

// BAD — importing another module's internals
import com.societix.treasury.persistence.InvoiceRepository;   // ❌ boundary violation
```

## 2. Naming

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, singular module noun | `com.societix.governance` |
| Class / record | `UpperCamelCase`, no `I`/`Impl` noise | `BallotTally`, not `IBallotTallyImpl` |
| Interface + single impl | name the interface for the role; impl `<Role>Impl` only if unavoidable | `PayoutApprover` |
| Method | `lowerCamelCase`, verb-first | `allocatePayment(...)` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_REMINDER_STAGES` |
| REST controller | `<Resource>Controller` | `InvoiceController` |
| Event record | `<Noun><PastTenseVerb>` | `InvoiceSettled`, `VoteCast` |
| Test class | `<Subject>Test` (unit) / `<Subject>IT` (integration) | `BallotTallyTest`, `TenancyRlsIT` |

## 3. Dependency injection — constructor only

**Constructor injection, always.** No field or setter injection. Declare collaborators `final`.
Single-constructor classes need no `@Autowired`.

```java
// GOOD
@Service
public class PaymentMatcher {
    private final InvoiceRepository invoices;
    private final LedgerPoster ledger;

    public PaymentMatcher(InvoiceRepository invoices, LedgerPoster ledger) {
        this.invoices = invoices;
        this.ledger = ledger;
    }
}

// BAD
@Autowired private InvoiceRepository invoices;   // ❌ field injection — untestable, hides deps
```

Prefer Lombok `@RequiredArgsConstructor` **only if** the project already uses Lombok; otherwise write
the constructor. Do not mix styles within a module.

## 4. DTO vs Entity vs Domain

Three distinct layers; never leak a JPA entity to the web layer.

| Type | Lives in | Mutability | Purpose |
|---|---|---|---|
| **Request/Response DTO** | `web/` | immutable `record` | HTTP wire shape; Jakarta-validated |
| **JPA entity** | `persistence/` | mutable class | DB row mapping only |
| **Domain object / VO** | `domain/` | immutable where possible | business rules, invariants |

- DTOs are **Java `record`s**. Map DTO↔domain↔entity in an explicit mapper (`internal/`); no
  `ModelMapper`-style reflection magic.
- Entities carry no business logic beyond invariants; put rules in domain services.

```java
// web/ — request DTO: immutable record + validation
public record RaiseInvoiceRequest(
        @NotNull UUID unitId,
        @NotNull @Positive Long amountMinor,       // money in minor units (paise)
        @NotNull @FutureOrPresent LocalDate dueDate) {}

// web/ — response DTO
public record InvoiceResponse(UUID id, String unitCode, long amountMinor,
                              LocalDate dueDate, InvoiceStatus status) {}
```

## 5. Immutability & records

- Use `record` for DTOs, events, and value objects.
- Prefer `final` fields and unmodifiable collections (`List.copyOf(...)`). Return immutable views.
- No setters on domain objects; express change as a method that returns a new instance or mutates a
  single aggregate root under a transaction.

## 6. Validation (Jakarta Validation)

- Validate **at the boundary**: annotate request records and add `@Valid` on the controller param.
- Business invariants that validation annotations can't express go in the domain layer and throw a
  domain exception (→ §8).
- Reusable constraints (e.g. `@UpiVpa`, `@Gstin`) are custom annotations in `platform`.

```java
@PostMapping("/invoices")
public InvoiceResponse raise(@Valid @RequestBody RaiseInvoiceRequest req) { ... }
```

## 7. Null-safety & Optional

- **No `null` across public APIs.** Return `Optional<T>` from repository/finder methods that may miss;
  never return `Optional` from a field, a collection getter (return empty collection), or a DTO.
- Don't call `.get()` without a prior `isPresent()`/`orElseThrow`. Prefer `orElseThrow(() -> ...)`.
- Annotate packages with `@org.springframework.lang.NonNullApi` (in `package-info.java`) so tooling
  flags nullable violations; use `@Nullable` only where genuinely optional.

```java
Invoice inv = invoices.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("invoice", id));   // → 404 problem+json
```

## 8. Exception handling → uniform problem+json

All errors surface as **RFC 9457 problem+json** (see [`api-guidelines.md`](api-guidelines.md) §error).
Throw typed domain exceptions; a single `@RestControllerAdvice` in `platform` maps them.

```java
// platform/
@RestControllerAdvice
public class ProblemExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail onNotFound(ResourceNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://errors.societix.dev/not-found"));
        pd.setProperty("code", "SOC-404-RESOURCE");         // app error-code catalogue
        pd.setProperty("traceId", Tracing.currentTraceId());
        return pd;
    }
}
```

- Never catch-and-swallow. Never return `500` for a client error — map to the right 4xx.
- Do **not** leak stack traces or internal messages to clients; log them, return a sanitized detail.

## 9. Logging — structured, zero PII (DPDP)

- Use **SLF4J** with **structured** key/value logging (JSON appender in all non-dev profiles).
- **Never log PII.** No names, phone numbers, email, vehicle plates, visitor photos, bank
  identifiers, UPI VPAs, or raw webhook bodies. This is a **DPDP** obligation, not a style rule.
  Log stable identifiers (`invoiceId`, `tenantId`, `unitId`) instead.
- Always include `tenantId` and `traceId` (MDC, set by the tenancy/trace filter).
- Use parameterized logging (`log.info("allocated {}", invoiceId)`), never string concatenation.

```java
// GOOD
log.info("payment allocated invoiceId={} amountMinor={}", invoiceId, amountMinor);
// BAD — PII leak, violates DPDP
log.info("paid by {} ({})", resident.name(), resident.phone());   // ❌ never
```

## 10. Transactions & the RLS tenant GUC

- Put `@Transactional` on the **application service method** (the use-case boundary), not on
  controllers or repositories. Keep transactions short.
- The tenancy filter opens the request transaction and runs `SET LOCAL app.tenant_id = :tid`
  (see [`../01-architecture.md`](../01-architecture.md) §6). `SET LOCAL` is scoped to that
  transaction — so **every DB-touching use case must run inside a transaction**, or RLS has no GUC
  and fails closed.
- The app DB role is **not** `BYPASSRLS`. Never query a tenant table outside a tenant-scoped tx.
- Publish domain events with `@ApplicationModuleListener` (runs **after commit**) or via the
  transactional **outbox** for anything that must be dispatched reliably and atomically with the DB
  write (no broker in v1 — see [ADR-0004](../adr/0004-kafka-event-driven-cadence.md)).

```java
@Service
public class PayoutService {
    @Transactional                                   // RLS GUC is live for the whole use case
    public Payout approve(UUID payoutId, UUID checkerId) {
        var payout = payouts.findById(payoutId).orElseThrow(...);
        payout.approve(checkerId);                   // maker-checker invariant in domain
        events.publish(new TreasuryEvents.PayoutApproved(payout.id()));  // after-commit
        return payout;
    }
}
```

## 11. Formatting & static analysis

| Gate | Tool | Rule |
|---|---|---|
| Formatting | **Spotless + google-java-format** | `cd backend && ./mvnw spotless:apply` before committing; CI runs `spotless:check` |
| Bug patterns | **Error Prone** | compiler plugin; warnings on `error` level fail the build |
| Static analysis | **SpotBugs** | runs in `cd backend && ./mvnw verify`; new high-priority bugs block merge |
| Imports | no wildcard imports | enforced by google-java-format |
| Line length | 100 cols | google-java-format default |

`cd backend && ./mvnw verify` runs tests + Spotless + Error Prone + SpotBugs + the Modulith `verify()` test.
CI runs the same — green locally, green in CI.

## 12. Test naming & structure

- Unit/slice tests: `<Subject>Test`; integration (Testcontainers): `<Subject>IT`.
- Method names describe behavior: `shouldRejectPayoutWhenMakerEqualsChecker()`.
- Arrange–Act–Assert, one logical assertion per test. Use JUnit 5 + AssertJ.
- Every module ships a Modulith `ApplicationModules.verify()` test and at least one tenancy/RLS
  isolation IT (a cross-tenant read must return zero rows).

```java
@Test
void shouldRejectPayoutWhenMakerEqualsChecker() {
    var payout = Payout.initiated(TENANT, VENDOR, 250_000L, makerId);
    assertThatThrownBy(() -> payout.approve(makerId))
        .isInstanceOf(MakerCheckerViolation.class);
}
```

---

**Related:** [`getting-started.md`](getting-started.md) · [`api-guidelines.md`](api-guidelines.md) ·
[`git-workflow.md`](git-workflow.md) · [`../03-data-model.md`](../03-data-model.md) ·
[ADR-0001](../adr/0001-modular-monolith-spring-modulith.md) ·
[ADR-0002](../adr/0002-postgres-rls-multitenancy.md)
