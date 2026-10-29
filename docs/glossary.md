# Glossary

**Document:** `SOC-DOC-GLOSSARY`. Domain, regulatory, and technical terms used across Societix docs.

## Domain — community & governance

| Term | Definition |
|---|---|
| **Society / Complex** | A residential gated community (the *tenant* in Societix). |
| **Unit / Flat** | An individual apartment; the billing subject. Has a carpet area, owner, optional tenant. |
| **MC — Managing Committee** | Elected volunteer body (Secretary, Treasurer, members) that runs the society. |
| **Secretary / Treasurer** | Key MC officers; Treasurer owns finances. Primary power users. |
| **Owner** | Legal owner of a unit; has voting rights. |
| **Tenant (resident)** | Occupant renting a unit; limited rights, typically no vote. NB: distinct from "tenant" in the multi-tenancy sense. |
| **AGM / EGM** | Annual / Extraordinary General Meeting — statutory member meetings. |
| **Quorum** | Minimum attendee voting weight required for an AGM resolution to be valid. |
| **Proxy** | A delegated vote cast on behalf of an absent owner. |
| **Resolution** | A motion put to vote at an AGM (e.g. approve accounts, fund hike). |
| **MoM — Minutes of Meeting** | Formal legal record of an AGM/EGM. |
| **Bylaws** | The society's governing rules, constrained by the State Cooperative Act. |

## Domain — treasury & accounting

| Term | Definition |
|---|---|
| **Maintenance** | Recurring charge levied on each unit to fund operations. |
| **Sinking Fund** | Long-term reserve for major capital works (statutorily mandated). |
| **Repair / Reserve Fund** | Funds earmarked for repairs and contingencies. |
| **Chart of Accounts** | Structured list of ledger accounts (assets, liabilities, income, expense, funds). |
| **Double-entry ledger** | Every transaction posts equal debits and credits. See [dd-01](deep-dives/dd-01-double-entry-ledger.md). |
| **Trial Balance / P&L / Balance Sheet** | Standard financial statements generated from the ledger. |
| **Defaulter** | A unit with overdue maintenance; subject to the escalation cadence. |
| **Cadence** | The automated multi-stage reminder/escalation sequence (Day +1 → +30). |
| **Maker-Checker** | Dual-control: one user initiates a payout, another approves. |
| **TDS / GST ITC** | Tax Deducted at Source / GST Input Tax Credit — tracked on vendor payouts. |

## Payments

| Term | Definition |
|---|---|
| **VAN — Virtual Account Number** | A dedicated virtual bank account per unit; inbound credits auto-reconcile to that unit. |
| **UPI VPA** | Virtual Payment Address (e.g. `maple.b1204@societix`) for UPI collections. |
| **Payment Aggregator (PA)** | RBI-licensed entity (Razorpay/Cashfree) that collects and settles funds to the society account. |
| **No float** | Societix never holds customer money; funds settle directly to the society bank account. |
| **Webhook matcher** | Service that consumes PA settlement webhooks and allocates them to invoices (zero-touch). |

## Regulatory

| Term | Definition |
|---|---|
| **DPDP Act 2023** | India's Digital Personal Data Protection Act. Society = *Data Fiduciary*, Societix = *Data Processor*. |
| **Data Fiduciary / Processor** | DPDP roles: who determines purpose (society) vs who processes on instruction (Societix). |
| **30-day purge** | Mandated auto-deletion of visitor photos/phone numbers after 30 days (M5). |
| **RBI PA norms** | Reserve Bank of India Payment Aggregator rules; basis for the no-float design. |
| **State Cooperative Societies Acts** | State statutes governing societies — e.g. **Maharashtra MCS Act** (Sec 154B interest caps, ≤10% non-occupancy), **Karnataka KAOA 1972** (voting weightages), **Haryana HRERA** (sinking-fund handover audits). |

## Technical

| Term | Definition |
|---|---|
| **Modular monolith** | Single deployable app with enforced internal module boundaries (Spring Modulith). |
| **RLS — Row-Level Security** | PostgreSQL feature filtering rows by a per-session tenant id; our tenancy isolation. |
| **CRDT** | Conflict-free Replicated Data Type — lets the offline gate merge edits without conflicts. |
| **WAL** | Write-Ahead Log — SQLite durability mechanism used on the gate edge. |
| **Outbox pattern** | Transactional outbox table written in the same DB transaction as the state change, then dispatched by a scheduled poller — our broker-free event backbone (see ADR-0004). |
| **Message broker (deferred)** | A dedicated event bus (e.g. Kafka or Redis Streams). Not used in v1; a documented scale-up option once the outbox poller's throughput is exceeded. |
| **RAG** | Retrieval-Augmented Generation — grounding LLM answers in retrieved bylaws/ledger context. |
| **pgvector** | PostgreSQL extension for vector similarity search (embeddings). |
| **ANPR** | Automatic Number Plate Recognition — vehicle entry at the gate. |
| **BFF** | Backend-for-Frontend — gateway layer tailoring APIs per client. |
| **Envelope encryption** | Encrypting data with a data key that is itself encrypted by a master key. |
