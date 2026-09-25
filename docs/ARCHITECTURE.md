# Design notes and interview preparation

## Request flow

A form submission sends JSON to the REST controller. Bean Validation rejects missing or oversized inputs. The service enforces cross-field date rules. The repository executes parameterized SQL, then returns the stored record. Responses include a version number, which the next update must send back.

## Why optimistic concurrency?

Two browser tabs can hold the same version. Updates use `WHERE id=? AND version=?` and increment the version atomically. Only one competing update succeeds; the other returns 409. Deletion also checks the version. This avoids silently discarding newer work without requiring database locks for the lifetime of a browser session.

## Why JDBC instead of JPA?

The data model has one table and no complex object graph. JDBC makes the queries and concurrency behavior explicit. JPA would become more attractive if relationships and aggregate persistence grew more complex.

## Why H2?

A reviewer can run the application without creating a database account. The file-backed database retains data after restart; integration tests use a separate in-memory database. H2 is a convenience for this local app, not a claim of production scalability. A PostgreSQL version would need migration scripts and database-specific integration tests.

## Frontend boundaries

The API is authoritative. The browser filters fetched records but never stores the source of truth in localStorage. User-controlled values are rendered with `textContent`, not interpolated HTML. Failed writes retain the open form and display the server's error. Successful writes refresh the server state.

CSV export quotes values and doubles embedded quotes; formula-like values receive an apostrophe prefix. Date-only values are formatted at local noon to avoid midnight UTC timezone shifts.

## Security boundaries

Loopback binding limits normal network access to the machine running the app. A custom header on writes, no CORS opt-in, cross-site fetch rejection, and a restrictive Content Security Policy reduce browser-origin risks. These controls do not authenticate users or defend against other software running on the same machine. Public hosting requires a different security design.

## Discuss it honestly

Before putting this project on your resume, run it, read the code, and make a change you can explain. Good practice exercises:

1. Add a follow-up date through schema, record, request, SQL, UI, and tests.
2. Explain why 409 differs from 400 and why create returns 201.
3. Demonstrate a stale edit using two tabs.
4. Replace H2 with PostgreSQL and document configuration changes.
5. Add a test for a boundary case before changing the implementation.

AI-assisted implementation is a useful starting point; interview confidence comes from understanding and extending it.
