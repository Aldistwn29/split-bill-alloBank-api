# Split Bill Application

A REST API that helps a group of people manage shared expenses and calculate
who owes whom at the end — a group trip, a team lunch, a shared apartment.
People take turns paying for things, and at the end the API figures out the
fairest way to settle up.

Built for the [Allo Bank Backend Developer Take-Home Test](https://github.com/allobankdev/allo-backend-test).

## Domain Model

- **BillGroup** — a group containing participants who share expenses.
- **Participant** — a member of a group.
- **Expense** — a recorded cost: who paid it (payer), how much, and for whom.
- **ExpenseSplit** — the allocated share of an expense per beneficiary.

Key concept: **payer ≠ beneficiary**. An expense means _"he already paid"_ —
the money has left the payer's pocket. The settlement then computes each
participant's net position: a negative balance means owing, a positive balance
means being owed. Invariant: the sum of all net balances is always zero —
money only changes hands, it never disappears.

## Requirements

The test requires, at minimum:

1. **Create a bill group** — a name and a list of participants. _(implemented)_
2. **Add expenses to a group** — who paid, how much, and who it was for. _(implemented)_
3. **Retrieve a settlement summary** — a clear breakdown of who owes whom and how much. _(implemented)_

## Strategy (Personalization)

Every settlement response includes two computed fields — never hardcoded:

- `service_charge_pct` — derived from the owner's GitHub username:
  lowercase the username, sum the Unicode values of all characters, take `sum % 10`.
- `service_charge_amount` — the percentage applied to the total group expenses.

For GitHub username `Aldistwn29` (lowercase `aldistwn29`):

```text
97 + 108 + 100 + 105 + 115 + 116 + 119 + 110 + 50 + 57 = 977
service_charge_pct = 977 % 10 = 7 (7%)
```

The value is computed at runtime from the configured username
(`app.github-username`, overridable via the `GITHUB_USERNAME` environment variable).

## Tech Stack

- Java 17, Spring Boot 4.1.2, Maven (wrapper included)
- Spring Data JPA (Hibernate 7), H2 in-memory database (`jdbc:h2:mem`) — no database setup needed
- Bean Validation, Jackson 3
- JUnit 5, Mockito, AssertJ
- `BigDecimal` for all monetary values; sub-cent precision is rejected

## API Design

| Method | Path                          | Status | Description                                   |
| ------ | ----------------------------- | ------ | --------------------------------------------- |
| POST   | `/api/groups`                 | 201    | Create a bill group with participants         |
| GET    | `/api/groups/{id}`            | 200    | Retrieve a bill group with its participants   |
| POST   | `/api/groups/{id}/expenses`   | 201    | Add an expense to a group (EQUAL split)       |
| GET    | `/api/groups/{id}/settlement` | 200    | Settlement summary: balances + service charge |

All errors follow RFC 9457 (`ProblemDetail`): business violations and bean
validation failures return 400, unknown resources return 404, with
Bahasa Indonesia detail messages.

## Build & Run

```bash
./mvnw spring-boot:run        # starts on http://localhost:4110
./mvnw test                   # runs all tests
```

## Example curl Commands

```bash
# 1. Create a group
curl -X POST http://localhost:4110/api/groups \
  -H "Content-Type: application/json" \
  -d '{"name":"Trip Sukabumi","participants":["Aldi","Setiawan","Damn"]}'

# 2. Retrieve the group (note the participant ids for the next step)
curl http://localhost:4110/api/groups/1

# 3. Add an expense (payer = participant 1, EQUAL split for participants 1, 2, 3)
curl -X POST http://localhost:4110/api/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"payerId":1,"amount":300000.00,"description":"Villa","split":{"type":"EQUAL"},"beneficiaryIds":[1,2,3]}'

# 4. Settlement summary
curl http://localhost:4110/api/groups/1/settlement
```

### Sample Request — Create Group

```json
{ "name": "Trip Sukabumi", "participants": ["Aldi", "Setiawan", "Damn"] }
```

### Sample Request — Create Expense

```json
{
  "payerId": 1,
  "amount": 300000.0,
  "description": "Villa",
  "split": { "type": "EQUAL" },
  "beneficiaryIds": [1, 2, 3]
}
```

### Sample Response — Settlement

```json
{
  "totalExpenses": 300000.0,
  "service_charge_pct": 7,
  "service_charge_amount": 21000.0,
  "balances": [
    { "participantId": 1, "participantName": "Aldi", "netBalance": 200000.0 },
    {
      "participantId": 2,
      "participantName": "Setiawan",
      "netBalance": -100000.0
    },
    { "participantId": 3, "participantName": "Damn", "netBalance": -100000.0 }
  ]
}
```

## Tests

```bash
./mvnw test
```

Unit tests cover: `SplitCalculator` (equal split invariants, cent remainder,
sub-cent detection), `GroupService` (happy path, duplicate-name rejection,
trim normalization, 404), `ExpenseService` (validation guards, split wiring),
and `SettlementService` (404, zero totals, happy path balances).

## Limitations & Roadmap

- Split strategy: `EQUAL` only — `PERCENTAGE` / `EXACT` in development
- Settlement summary is a simple net-balance view; transaction
  optimization (minimal payments) is unexplored territory
- Expenses are add-only (no edit/delete)
- Multi-stage `Dockerfile` included (build verification in progress)

## Submission Question

**"What was the hardest design decision you made while building this, and what trade-off did you accept?"**

> The hardest decision was how to divide an amount that cannot be split evenly
> down to the cent — for example, splitting 100.00 among three people. Rounding
> each share independently can make the sum of shares drift away from the
> original amount, which is unacceptable for financial data: the invariant
> "sum of all shares equals the expense amount" had to hold by construction,
> not by luck. I chose to compute in integer cents and hand the leftover
> one-cent remainders to the first participants in input order — a
> deterministic, invariant-safe rule. The trade-off I accepted is a small,
> documented asymmetry: the first beneficiary can carry a few extra cents, and
> the outcome depends on input order. I consider that acceptable because money
> must always balance, while "who receives the extra cent" is a stable,
> explainable rule rather than hidden rounding magic.
