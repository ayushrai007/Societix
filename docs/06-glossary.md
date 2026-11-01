# 06 — Glossary

**What this is:** plain-English meaning of every term, acronym, and bit of jargon used in Societix
docs and code. When you hit a word you don't know, it's here.
**Who should read it:** keep it open as a reference.

---

## Society / domain words

| Term | In simple words |
|---|---|
| **Society** (a.k.a. complex) | One apartment complex. In our system, each society is a separate "tenant" whose data is kept private from all others. |
| **Unit / Flat** | One apartment. Maintenance bills are charged per flat. |
| **Managing Committee (MC)** | The small group of unpaid resident volunteers who run the society. |
| **Secretary / Treasurer** | The two key committee roles. Treasurer handles money; Secretary runs the rest. Our main users. |
| **Owner** | Someone who owns a flat. Can vote in meetings. |
| **Tenant (resident)** | Someone renting a flat. Usually can't vote. ⚠️ Different from "tenant" in the software sense below. |
| **AGM** | **A**nnual **G**eneral **M**eeting — the yearly meeting where owners vote on society matters (budgets, rule changes). **EGM** is an *extraordinary* (unscheduled) one. |
| **Quorum** | The minimum number/weight of attendees needed for a meeting's votes to count as valid. |
| **Proxy** | A vote cast on behalf of an owner who can't attend. |
| **Resolution** | A proposal put to a vote at a meeting (e.g. "raise the sinking fund by ₹2/sq.ft"). |
| **Minutes (MoM)** | The official written record of what happened and was decided at a meeting. |
| **Bylaws** | The society's own rulebook, within the limits of state law. |

## Money / accounting words

| Term | In simple words |
|---|---|
| **Maintenance** | The recurring fee every flat pays to run the society. |
| **Sinking fund / Repair fund / Reserve fund** | Savings pots for big future expenses (major repairs, etc.). Some are legally required. |
| **Defaulter** | A flat that hasn't paid its dues on time. |
| **Cadence** | Our automatic reminder sequence for defaulters: gentle nudge on day 1, firmer on day 10, late fee at day 15, formal notice at day 30. |
| **Double-entry ledger** | The standard, correct way to keep accounts: every transaction is recorded twice (a debit and a credit) so the books always balance. |
| **Maker-checker** | A safety rule: one person requests a payment, a *different* person approves it. Stops any single person moving money alone. |
| **TDS / GST** | Indian taxes. TDS = tax withheld from vendor payments; GST = sales tax (and "ITC" = credit for GST already paid). We track these on payouts. |

## Payment words

| Term | In simple words |
|---|---|
| **Payment aggregator** | A licensed company (Razorpay/Cashfree) that collects payments and deposits them into the society's bank account. We use one so we never hold money ourselves. |
| **VAN** | **V**irtual **A**ccount **N**umber — a unique bank account number we give each flat, so a payment to it is instantly matched to that flat. |
| **UPI VPA** | The UPI version of a VAN — a unique payment address per flat (like `maple-b1204@bank`). |
| **Webhook** | A message an outside service (the payment provider) sends *to us* when something happens ("payment received"). The opposite of us calling them. |
| **No float** | "Float" = holding other people's money. We hold none — it goes straight to the society. This keeps us out of heavy banking regulation. |

## Technical words

| Term | In simple words |
|---|---|
| **Backend** | The server program we write (the Java/Spring Boot app). Does the thinking; stores the data. |
| **Frontend** | What users see and tap — the web dashboard and the phone apps. |
| **Monolith / Modular monolith** | *Monolith* = one single program (not many mini-servers). *Modular* = cleanly divided inside into the 6 modules. We build a modular monolith: simple to run, still tidy. |
| **Microservices** | The opposite approach — many small separate servers. Powerful but a lot of work to run; we chose **not** to do this early. |
| **Module** | One of the 6 self-contained parts of the backend (Treasury, Governance, etc.). |
| **Tenant / Multi-tenant** | *Tenant* (software sense) = one society sharing our system. *Multi-tenant* = many societies on one system, kept safely separate. |
| **RLS (Row-Level Security)** | A Postgres feature that makes the database itself hide one society's rows from another. Our main privacy safeguard. |
| **Migration (database)** | A small script that changes the database structure (add a table/column). Run in order, once each, via Flyway. ⚠️ Different from the *Migration module* (M6), which imports a society's old data. |
| **Outbox (pattern)** | A database table where we jot down "things that happened," which a background job then acts on. Our simple, cheap replacement for a message broker. |
| **Kafka** | A heavyweight dedicated system for passing events between programs. Powerful but expensive/complex — we skip it for now and use the outbox instead. |
| **Event** | A record that something happened ("invoice raised", "payment settled"). Other parts of the system react to events. |
| **Idempotency** | A fancy word for "doing it twice has the same effect as doing it once." Important so a repeated payment webhook doesn't charge/record twice. |
| **CRDT** | **C**onflict-free **R**eplicated **D**ata **T**ype — a technique that lets the offline gate tablet and the server merge their changes automatically without clashing. |
| **WAL** | **W**rite-**A**head **L**og — a database durability trick (used on the gate tablet) so data isn't lost on a crash. |
| **RAG** | **R**etrieval-**A**ugmented **G**eneration — how the AI Copilot answers: first *retrieve* relevant society data, then let the AI *generate* an answer grounded in it (so it doesn't make things up). |
| **pgvector** | A Postgres plug-in that stores the data the AI needs to search, so we don't run a separate AI database. |
| **Embedding** | Text turned into a list of numbers so a computer can find "similar" text. Used by the AI Copilot. |
| **API** | The set of web addresses (endpoints) our backend exposes for the apps to call (e.g. `POST /api/v1/payments`). |
| **JWT** | **J**SON **W**eb **T**oken — a signed digital pass a user gets after logging in; every request carries it to prove who they are and which society they belong to. |
| **OIDC / OAuth** | Standard, secure ways to handle "log in." We use them so we don't invent our own (risky) login system. |
| **Actuator** | A Spring Boot feature that exposes health/metrics endpoints so we can see if the app is healthy. |
| **Docker / container** | A way to package software so it runs identically everywhere. Locally it starts our database/cache with one command. |

## Legal / compliance words

| Term | In simple words |
|---|---|
| **DPDP Act** | India's **D**igital **P**ersonal **D**ata **P**rotection law (2023) — like Europe's GDPR. Big fines for misusing personal data. Shapes our privacy module. |
| **Data Fiduciary / Data Processor** | DPDP roles. The society *decides* how data is used (fiduciary); we *process* it on their instruction (processor). |
| **RBI** | **R**eserve **B**ank of **I**ndia — the banking regulator. Why we use a payment aggregator and never hold money. |
| **State Cooperative Act** | State-level laws for housing societies (Maharashtra, Karnataka, etc.) that set rules like late-fee caps and voting weights. Our software has to respect them. |

---

Still stuck on a word? It's probably defined in more depth in the archived docs
([`archive/glossary.md`](archive/glossary.md)).
