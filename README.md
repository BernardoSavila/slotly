# Slotly

[![CI](https://github.com/BernardoSavila/slotly/actions/workflows/ci.yml/badge.svg)](https://github.com/BernardoSavila/slotly/actions/workflows/ci.yml)

A resource booking REST API built with Java 25 and Spring Boot 4.
Users reserve shared resources (meeting rooms, equipment) for a time window,
and the system guarantees that no two bookings for the same resource overlap.

## Preventing overlapping bookings

This is the core problem the project solves, and the part worth reading.

### Detecting an overlap

Two intervals overlap when:

```
existing.startsAt < new.endsAt  AND  new.startsAt < existing.endsAt
```

Enumerating the cases where intervals *do* overlap gives four scenarios and
invites off-by-one mistakes. Negating the two cases where they *don't* —
one ends before the other begins, or vice versa — gives the condition above,
which covers all four.

Note the strict `<`. Intervals are treated as half-open: `[09:00, 10:00)`
and `[10:00, 11:00)` do **not** overlap. Using `<=` would reject
back-to-back bookings, which is exactly what a booking system must allow.

### The naive implementation is not enough

The obvious implementation checks, then inserts:

```java
if (bookingRepository.existsOverlapping(resourceId, startsAt, endsAt)) {
    throw new OverlappingBookingException(...);
}
bookingRepository.save(booking);
```

This is correct for sequential requests and wrong under load. Between the
check and the insert there is a window in which another transaction can
insert a conflicting row. Both requests read an empty result, both conclude
the slot is free, both write.

This is a time-of-check to time-of-use race. It is invisible in manual
testing — `curl` cannot issue two requests close enough together — and it
surfaces precisely in the scenario that matters: two people booking the
same room at the same moment.

`@Transactional` alone does not fix it. A transaction guarantees atomicity,
not that a read stays valid until commit. Under the default `READ COMMITTED`
isolation level, a row that did not exist at check time can appear before
the transaction ends: a phantom read.

### Options considered

**Raise the isolation level to `SERIALIZABLE`.** Correct, but applied to the
whole transaction, costly under load, and in PostgreSQL it surfaces
serialization failures that the application must retry. Too blunt for a
single invariant.

**Pessimistic row lock on the resource.** Acquire a write lock on the
resource row before checking, so concurrent requests for the same resource
queue behind each other. Bookings for *different* resources are unaffected,
since the lock is per row.

**Database-level exclusion constraint.** In PostgreSQL:

```sql
ALTER TABLE bookings ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist (
    resource_id WITH =,
    tstzrange(starts_at, ends_at) WITH &&
);
```

This makes overlapping rows physically impossible, regardless of which
client writes them. It is the strongest guarantee but produces a constraint
violation rather than a clean domain error.

### What is implemented

The pessimistic lock, acquired before the overlap check:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Resource r WHERE r.id = :id")
Optional<Resource> findByIdForUpdate(@Param("id") Long id);
```

The lock is taken on the **resource** row rather than on bookings, because
you cannot lock rows that do not exist yet, and it is the insertion of new
rows that must be serialized. The resource acts as a serialization point
that every request for that resource must pass through. It is held until
the transaction commits, which is why `@Transactional` is required for it
to mean anything.

### How it is verified

`BookingConcurrencyTest` releases ten threads simultaneously via a
`CountDownLatch` and has them all attempt the same slot. Exactly one
succeeds.

The test was verified by removal. With the lock, the generated SQL shows the
requests serializing — ten lock acquisitions, then checks spaced apart, each
seeing the previous insert:

```
23:14:02.054 [thread-10] select ... from resources where id=? for no key update
23:14:02.055 [thread-7]  select ... from resources where id=? for no key update
...
23:14:02.076 [thread-2]  insert into bookings ...     <- the only insert
23:14:02.084 [thread-7]  select count(b.id)>0 ...     <- sees it, rejects
```

Replacing `findByIdForUpdate` with a plain `findById` makes the test fail
with ten bookings created — all ten checks run in the same millisecond,
all return false, all insert:

```
23:18:24.667 [thread-5] select count(b.id)>0 ...
23:18:24.668 [thread-9] select count(b.id)>0 ...
23:18:24.668 [thread-2] select count(b.id)>0 ...
...
23:18:24.673 [thread-9] insert into bookings ...
23:18:24.673 [thread-8] insert into bookings ...
23:18:24.674 [thread-5] insert into bookings ...
```

A test that passes with and without the protection proves nothing. This one
fails without it.

Tests run against a real PostgreSQL instance via Testcontainers rather than
an in-memory database, since locking semantics differ between engines and a
test against H2 would not establish anything about production behaviour.

### Two layers

Both protections are in place, and they do different jobs.

The pessimistic lock serializes concurrent requests to this application and
produces a clean `409 Conflict` with a message naming the resource and the
period. It is what the API relies on in normal operation.

The exclusion constraint (`V4__prevent_overlapping_booking.sql`) makes
overlapping rows impossible for any writer — a second application instance,
a migration script, a direct `psql` session, or a future bug in this code:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_bookings
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    );
```

`tstzrange` is half-open by default — it includes the start and excludes the
end — which matches the strict `<` used in the application-level check. The
two layers agree on the boundary semantics without having to be kept in sync
by hand.

Adding the constraint opens a path the application did not previously have:
if it fires, the result is a `DataIntegrityViolationException`, not the
domain exception. `GlobalExceptionHandler` inspects the exception chain for
the constraint name and maps that specific case to `409`; every other
integrity violation is rethrown and surfaces as a `500`, because a foreign
key or unique violation there would be a bug, not a booking conflict.

Matching on the constraint name is not elegant — renaming the constraint
without updating the handler would silently fall back to `500`. Checking the
PostgreSQL SQLSTATE (`23P01`) would be more robust but less readable with a
single constraint in play. Both options trade something.

## Work in progress

Step 8 of 10: JWT authentication.

Done: Spring Security and JJWT dependencies, `V5__add_user_password.sql`
adding a `password_hash` column, `User` entity updated, `SecurityConfig`
(stateless, CSRF off, `/api/auth/**` public), `JwtService` for signing and
verifying tokens, and the auth request/response records.

Remaining: `AuthService` (register and login), `AuthController`, the filter
that reads the token on each request, and updating the fifteen existing
tests, which currently fail with 401 now that Security is on the classpath.

Note: the CI badge above may be red while this