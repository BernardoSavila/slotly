CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_bookings
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    );