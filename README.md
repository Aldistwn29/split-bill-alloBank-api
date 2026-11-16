# Split Bill API — Allo Bank Backend Challenge

A REST API for managing shared expenses within a group — a trip, a team
lunch, or a shared apartment. Participants take turns paying, and at the end
the API computes everyone's net position: who owes whom, and how much.

Built for the [Allo Bank Backend Developer Take-Home Test]
(https://github.com/allobankdev/allo-backend-test).

## Challenge Overview

> "Welcome, and thank you for your interest in joining Allo Bank Engineering!
> This challenge is intentionally open-ended. There is no skeleton, no guided
> steps, and no single correct answer. We want to see how you think, how you
> structure a solution, and what you consider important in production-grade
> code."

## APIs Built

The three core capabilities required by the challenge:

1. **Creating a bill group** — a name + a list of participants
   → `POST /api/groups`
2. **Adding expenses** — who paid (payer), how much, and who it was for
   → `POST /api/groups/{id}/expenses`
3. **Retrieving a settlement summary** — a clear breakdown of who owes whom
   and how much → `GET /api/groups/{id}/settlement`

Plus supporting endpoints (Show Your Skills):

| Method | Path                                | Status | Description                                   |
| ------ | ----------------------------------- | ------ | --------------------------------------------- |
| POST   | `/api/groups`                       | 201    | Create a bill group with participants         |
| GET    | `/api/groups/{id}`                  | 200    | Retrieve a bill group with its participants   |
| POST   | `/api/groups/{id}/expenses`         | 201    | Add an expense (EQUAL / EXACT / PERCENTAGE)   |
| GET    | `/api/groups/{id}/settlement`       | 200    | Settlement summary: balances + service charge |
| GET    | `/api/groups/{id}/expenses/summary` | 200    | Expense totals grouped by category            |

Key domain concept: **payer ≠ beneficiary**. An expense means "he already
paid" — the money has left the payer's pocket. The settlement then computes
each participant's net position: a negative balance means owing, a positive
balance means being owed. Invariant: **the sum of all net balances is always
zero** — money only changes hands, it never disappears.

## Tech Stack

- Java 17, Spring Boot, Maven (wrapper included)
- `BigDecimal` for all monetary values — no float/double; sub-cent amounts
  are rejected
- Multi-stage Dockerfile (following the challenge's `Dockerfile.template`)
- At least one unit test covering the settlement calculation logic — 34 tests
  in total
- Spring Data JPA (Hibernate), H2 in-memory database — no database setup
- JUnit 5, Mockito, AssertJ

## Build & Run

```bash
# Without Docker
./mvnw spring-boot:run        # starts on http://localhost:4110
./mvnw test                   # runs all tests

# With Docker (multi-stage build)
docker build -t splitbill .
docker run --name splitbill-api -p 4110:4110 splitbill
The database uses H2 in-memory — data resets every time the application
restarts.
Example curl Commands
# 1. Create a group
curl -X POST http://localhost:4110/api/groups \
  -H "Content-Type: application/json" \
  -d '{"name":"Trip Docker","participants":["aim","ami","abdul"]}'

# 2. Retrieve the group (note the participant ids for the next steps)
curl http://localhost:4110/api/groups/1

# 3. Add an expense — EQUAL split for participants 1, 2, 3
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"payerId":1,"amount":300000.00,"description":"Villa","category":"ACCOMMODATION","split":{"type":"EQUAL"},"beneficiaryIds":[1,2,3]}'

# 4. Add an expense — PERCENTAGE split (50/50)
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"payerId":1,"amount":100.00,"description":"Makan","category":"FOOD","split":{"type":"PERCENTAGE","allocations":[{"participantId":1,"value":50.00},{"participantId":2,"value":50.00}]}}'

# 5. Add an expense — EXACT split
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"payerId":1,"amount":100.00,"description":"Tiket","category":"TRANSPORT","split":{"type":"EXACT","allocations":[{"participantId":1,"value":70.00},{"participantId":2,"value":30.00}]}}'

# 6. Settlement summary
curl http://localhost:4110/api/groups/1/settlement

# 7. Category summary
curl http://localhost:4110/api/groups/1/expenses/summary
Sample Response — Settlement
{
  "totalExpenses": 100.00,
  "service_charge_pct": 7,
  "service_charge_amount": 7.00,
  "balances": [
    { "participantId": 1, "participantName": "aim",   "netBalance": 50.00 },
    { "participantId": 2, "participantName": "ami",   "netBalance": -50.00 },
    { "participantId": 3, "participantName": "abdul", "netBalance": 0.00 }
  ]
}
Personalization — Service Charge
Every settlement response includes two computed fields — never hardcoded:
- service_charge_pct — lowercase the GitHub username, sum the ASCII values
of all characters, then take % 10
- service_charge_amount — that percentage applied to the group's total
expenses
For GitHub username Aldistwn29 (lowercase aldistwn29):
97 + 108 + 100 + 105 + 115 + 116 + 119 + 110 + 50 + 57 = 977
service_charge_pct = 977 % 10 = 7 (7%)
The value can be overridden via the GITHUB_USERNAME environment variable.
Design & Strategy Notes (Show Your Skills)
Core invariant: Σ share == amount. All divisions are computed in integer
cents (long), never in floating point. When a division does not divide
evenly — e.g. 100.00 among 3 people — the leftover cents are handed out one
cent at a time to the first participants in input order: a deterministic rule
that keeps the invariant true by construction.
Three split strategies (EQUAL / EXACT / PERCENTAGE) share one pure
calculator with no I/O; the service validates relations (membership,
duplicates), while the calculator validates the math (percentages must sum to
exactly 100.00, positive values, sub-cent rejected). Adding a new strategy
requires no controller changes.
Expense categories — every expense is categorized (FOOD / TRANSPORT /
ACCOMMODATION / OTHER), and the summary endpoint totals expenses per category
to make group spending analysis easy.
Submission Question
"What was the hardest design decision you made while building this, and
what trade-off did you accept?"
The hardest decision was how to divide an amount that cannot be split
evenly down to the cent — for example, splitting 100.00 among three people.
Rounding each share independently can make the sum of shares drift away
from the original amount, which is unacceptable for financial data: the
invariant "sum of all shares equals the expense amount" had to hold by
construction, not by luck. I chose to compute in integer cents and hand the
leftover one-cent remainders to the first participants in input order — a
deterministic, invariant-safe rule. The trade-off I accepted is a small,
documented asymmetry: the first beneficiary can carry a few extra cents,
and the outcome depends on input order. I consider that acceptable because
money must always balance, while "who receives the extra cent" is a stable,
explainable rule rather than hidden rounding magic.
Limitations & Future Improvements
- Expenses are add-only (no edit/delete yet)
- The settlement is a net-balance view; transaction optimization (greedy
minimal payments) is not implemented
- With very small amounts (e.g. 0.01), split strategies can produce a 0.00
share for some participants — consistent, documented behavior
- H2 in-memory satisfies the "start without manual steps" requirement; for
production, swapping the dependency + URL to PostgreSQL requires no domain
code changes
```
