# Taste Theory

[![build](https://github.com/pshakira/taste-theory/actions/workflows/build.yml/badge.svg)](https://github.com/pshakira/taste-theory/actions/workflows/build.yml)

Back-office software for a single independent restaurant: distributor invoices,
supplier price tracking, inventory, and recipe costing with nutrition and
allergen data. It replaces a set of spreadsheets.

Java 21 · Spring Boot 4 · PostgreSQL · a modular monolith with a domain model
kept deliberately free of the framework.

---

## Why this domain is interesting

Most of the work here is not CRUD. Four problems shape the design:

**Recipes contain recipes.** A dish can use a sub-recipe, which can use another,
to any depth. Cost and nutrition roll up recursively, and a cycle — A contains B
contains A — has to be rejected when it is written, not discovered later during a
calculation. That invariant spans several aggregates, so it cannot live on the
`Recipe` entity itself.

**One traversal, two rules.** Yield affects cost but not nutrition. At 80% yield
you buy 1.25 kg for every 1 kg that reaches the pan, so cost per recipe unit
rises — but the nutrition figures already describe the edible portion, so they
must not be scaled. The same tree walk accumulates two totals under two different
rules.

**Money is not a `double`, and it is not one type either.** An invoice line total
is an exact recorded amount that has to reconcile to the cent. A price per unit is
a derived rate: €8.90 across 12 units is €0.741666…, and rounding it to two places
and multiplying back does not return €8.90. Those need different types with
different rounding contracts.

**Units are dimensional.** An item is bought in one unit and cooked in another —
a 5 kg sack, measured out in grams. Weight and volume never convert into one
another, because that would need a density the app does not model. The type system
enforces it rather than a comment asking nicely.

## Domain rules

These are decisions, not accidents. Each has a consequence worth knowing before
entering data.

| Rule | Consequence |
|---|---|
| An item has a purchase unit, a recipe unit, and one conversion factor between them. | Buy a 5 kg sack, cook in grams. No weight↔volume conversion — an item is one or the other. |
| Pricing is entered as pack size plus pack price; price per purchase unit is derived. | Leave the pack fields empty to enter a price per unit directly. |
| IVA is recorded but not applied to costing. | Recipe costs are net of tax. |
| Cost is the latest purchase price. Saving an invoice updates each item's cost from its most recent line. | Recipe margins describe today's prices, never historical ones. |
| Yield applies to cost, not to nutrition. | Cost per recipe unit rises with trim loss; nutrition figures already describe the edible portion. |
| Sub-recipe quantities are expressed in servings, not weight. | "Two servings of tomato sauce", not "200 g". |
| Allergens propagate upward automatically through ingredients and sub-recipes. | Editing one ingredient updates every dish that uses it, including through sub-recipes. |

> **Allergen and nutrition output is not compliance-ready.** The app propagates
> what you enter. It has not been verified against food labelling regulation, and
> nutrition is a sum of ingredient figures rather than an analysis of the finished
> dish. It must not be presented as label-ready.

## Architecture

A modular monolith — one deployable application with firm internal walls. Each
bounded context is a package, and each is split into three layers:

```
com.tastetheory
├── shared          Money, Quantity, UnitOfMeasure — cross-context value objects
├── inventory       items, units, pack pricing, suppliers
├── recipes         recipes, sub-recipe composition, costing, nutrition, allergens
└── invoicing       invoices, line items, price-change detection
```

```
domain           entities, value objects, domain services. Plain Java: no Spring,
                 no JPA, no imports from the layers around it.
application      use cases, plus the port interfaces the domain needs.
infrastructure   adapters: JPA entities, repository implementations, REST
                 controllers, external clients.
```

Dependencies point inward only. JPA entities are kept separate from domain
entities and mapped between, so the domain model can express business rules
without bending to what an ORM finds convenient.

Contexts do not reach into each other. Where `recipes` needs an ingredient's cost,
it declares its own port describing what it needs, and an adapter translates —
so `inventory` can be restructured without breaking anything but that adapter.

**These rules are enforced, not merely intended.** `ArchitectureTest` fails the
build if the domain acquires a framework import, if a layer depends outward, or if
`shared` comes to depend on a module.

## Running it locally

Requires Java 21 and Docker.

```bash
docker compose up -d      # Postgres on 5432
./gradlew bootRun         # http://localhost:8080
```

Health check, including database connectivity:

```bash
curl localhost:8080/actuator/health
```

Flyway applies migrations on startup. `docker compose down` stops the database
and keeps its data; add `-v` to discard it.

## Tests

```bash
./gradlew test
```

Three kinds, deliberately:

- **Domain unit tests** — plain JUnit, no Spring context, no database. Fast enough
  to run constantly. That they are possible at all is what the layering buys.
- **Integration tests** — Testcontainers, against a real PostgreSQL rather than an
  in-memory substitute that behaves subtly differently.
- **Architecture tests** — ArchUnit, described above.

## Status

Built in vertical slices, each ending with something that works end to end.

- [x] **1** Project skeleton — Gradle, packages, Docker Compose, Flyway, health endpoint, CI
- [ ] **2** Shared value objects — Money, Quantity, UnitOfMeasure, conversion
- [ ] **3** Inventory domain and persistence
- [ ] **4** Inventory API and first frontend slice
- [ ] **5** Recipes — sub-recipes, cost and nutrition rollup, allergen propagation
- [ ] **6** Invoicing — line items, latest-cost propagation, price-change detection
- [ ] **7** Deployment
- [ ] **8** Invoice photo extraction — vision model behind a port, reviewed drafts
- [ ] **9** Authentication
- [ ] **10** Stock movement ledger

## Not built, deliberately

- **Statutory label output** — blocked on a compliance question, not on
  engineering. Internal nutrition data is treated as non-labelling until that is
  closed.
- **Weighted-average or FIFO costing** — latest purchase price only. Once the
  movement ledger records deliveries, upgrading is a change to a calculation
  rather than a data migration.
- **POS integration, payroll, reservations, double-entry accounting** — out of
  scope. Invoices are captured for costing and handed to an accountant.
- **Multi-tenancy** — every table is single-organisation.

## Background

This is a rewrite. An earlier implementation in Node.js and Express
([`restaurant-os-2`](https://github.com/pshakira/restaurant-os-2)) proved the
domain rules against a real restaurant's invoices and cost sheets — several
thousand rows of them. The requirements here are ported from a working system
rather than invented, which is why the rules above are as specific as they are.
