# Slotly

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

### Known gap

The exclusion constraint is not yet in place. The application-level lock
protects against concurrent requests to this application; it does not
protect against a second instance, a migration script, or direct database
access. The intended end state is both layers: the lock for a clean `409`
response, the constraint as the guarantee that cannot be bypassed.