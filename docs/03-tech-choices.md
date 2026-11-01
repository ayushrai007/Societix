# 03 — Tech Choices

**What this is:** every tool/technology we use, what it actually is, and why we picked it — in plain
words. No prior knowledge assumed.
**Who should read it:** everyone, so we all understand *why* the stack looks the way it does.

A theme runs through all of it: **start cheap and simple, upgrade later only if we actually need to.**
We're 3 people with a small early user base — we don't buy enterprise-scale tools to serve 20
societies.

---

## Backend (the server program)

### Java 21
**What:** a mature, fast, strongly-typed programming language. "21" is the current long-term-support
version (supported for years).
**Why:** the team already knows Java; it's reliable, has huge library support, and is a safe choice
for financial software where correctness matters.

### Spring Boot
**What:** the most widely used framework for building web servers in Java. It handles the boring
plumbing (HTTP, database connections, security) so we write mostly business logic.
**Why:** industry standard, enormous community, tons of built-in features we'd otherwise hand-build.

### Spring Modulith
**What:** a small add-on to Spring Boot that lets us split our one program into clear internal
**modules** (Treasury, Gate, etc.) and *enforces* the boundaries between them.
**Why:** gives us the tidiness of separate services without the pain of actually running separate
services. ("Modulith" = modular + monolith.)

### Gradle
**What:** our **build tool** — the thing that compiles the code, runs tests, and packages the app
(`./gradlew ...` commands).
**Why:** standard for modern Java; the "wrapper" (`gradlew`) means everyone uses the exact same
version with no manual install.

---

## Data (where information is stored)

### PostgreSQL ("Postgres")
**What:** a free, open-source **relational database** (stores data in tables with rows and columns,
like a very powerful spreadsheet you query with SQL).
**Why:** rock-solid, free, does everything we need (and several things that let us *avoid* buying
extra tools — see outbox and pgvector below). One database to learn and run.

### Row-Level Security (RLS)
**What:** a built-in Postgres feature where the database only returns rows that belong to the current
user/society.
**Why:** this is how we keep each society's data private. Even if our application code has a bug, the
database itself won't leak society A's data to society B. Safer than relying only on our own code,
and critical for the DPDP privacy law.

### Flyway
**What:** a tool that applies **database migrations** — version-controlled scripts that change the
database structure (e.g. "add a `late_fee` column"). Each script runs once, in order.
**Why:** so the database schema is reproducible and every developer/environment is identical. No
manual "run this SQL on prod" chaos.

### Redis
**What:** an extremely fast in-memory key-value store — think a shared scratchpad that lives in RAM.
**Why:** for things that need to be fast and temporary: caching, rate-limiting, and preventing
duplicate actions. Not our source of truth (Postgres is); just a speed helper.

### pgvector
**What:** a Postgres extension (plug-in) that lets the database store and search "embeddings" —
numeric representations of text used by AI features.
**Why:** it lets the AI Copilot (M3) do its searching **inside Postgres** instead of us running a
separate specialized "vector database." One less thing to operate. It's basically free (just enable
the extension).

### The outbox (instead of Kafka)
**What:** a plain database table where we record "things that happened" (events), drained by a small
background job. Explained in [02 — How It Works](02-how-it-works.md#how-automatic-things-happen).
**Why:** it gives us reliable automatic/background behaviour **with zero extra infrastructure**.
The heavyweight alternative, **Kafka** (a dedicated message-passing system), is powerful but costly
and complex to run — pure overkill at our scale. We can switch to it later without rewriting the
business logic.

---

## Payments

### Razorpay / Cashfree (payment aggregators)
**What:** licensed Indian companies that handle the actual money movement (UPI, cards, bank
transfers). A "payment aggregator" collects payments on behalf of a business and settles them to a
bank account.
**Why:** money paid by residents goes **directly to the society's bank account** through them — we
never hold it. That keeps us out of heavy banking regulation (holding other people's money needs a
special RBI licence we don't want). They notify our backend when a payment lands.

### VAN / UPI VPA
**What:** a **VAN** is a "virtual account number" — a unique bank account number we assign to each
flat. A **UPI VPA** is the UPI equivalent (an address like `maple-b1204@bank`). When a resident pays
to their unique number/address, we instantly know which flat it was.
**Why:** it makes payments reconcile themselves — no resident needs to type a reference, and the
treasurer never has to manually match payments to flats.

---

## Front-end (what users see)

### React (web dashboard)
**What:** the most popular JavaScript library for building web interfaces.
**Why:** standard, huge ecosystem, easy to hire for. This is the committee's dashboard.

### React Native (phone apps)
**What:** a framework to build iPhone **and** Android apps from a single codebase, using the same
React skills as the web.
**Why:** we're 3 people — writing one app codebase for both platforms (resident app + gate app)
instead of two separate native apps saves enormous time.

### TypeScript
**What:** JavaScript with type-checking added (catches a class of bugs before the code runs).
**Why:** safer than plain JavaScript, now the default for serious React projects.

---

## Hosting & running it

### Managed cloud services
**What:** we rent Postgres, Redis, and app-hosting as **managed services** — the cloud provider runs
and maintains them for us (backups, patches, uptime).
**Why:** 3 developers should write features, not babysit servers. Slightly more money per month, far
less of our time. We host in **India** (Mumbai region) because the DPDP law expects Indian residents'
data to stay in India.

### Docker
**What:** a tool that packages software into **containers** — self-contained boxes that run the same
everywhere. Locally, `docker compose` starts Postgres and Redis for us in seconds.
**Why:** everyone's local setup is identical; no "works on my machine" problems.

### Single region (for now)
**What:** we run in one data-centre region to start, with automatic backups.
**Why:** running in multiple regions for extra resilience is expensive and complex; not worth it
until we have many paying societies. Documented as a later upgrade.

---

## Things we deliberately did **not** use (and why)

| We skipped | Because |
|---|---|
| **Kafka** (message broker) | Overkill and costly at our scale; the Postgres outbox does the job. |
| **Microservices** (many small servers) | Huge operational burden for 3 people; one modular program is simpler. |
| **A separate vector database** | pgvector keeps AI search inside Postgres — one less system. |
| **Multi-region / fancy deploys** (canary, blue-green) | Premature; a simple rolling deploy in one region is fine early on. |
| **Holding/escrowing money ourselves** | Would make us a regulated financial entity; the payment aggregator avoids that. |

Every one of these is a documented decision in [`archive/adr/`](archive/adr/README.md) if you want
the full reasoning.
