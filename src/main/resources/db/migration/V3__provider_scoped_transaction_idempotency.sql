-- V3: Provider-scoped transaction idempotency
--
-- The same source_event_id may legitimately be emitted by
-- different providers. Idempotency therefore needs to be scoped
-- by account + provider/source system + source event.

-- Existing legacy rows must have a provider identifier before
-- source_system can become mandatory.
UPDATE transaction
SET source_system = 'LEGACY'
WHERE source_system IS NULL
   OR TRIM(source_system) = '';

ALTER TABLE transaction
    ALTER COLUMN source_system SET NOT NULL;

ALTER TABLE transaction
    DROP CONSTRAINT IF EXISTS uq_account_source_event;

ALTER TABLE transaction
    ADD CONSTRAINT uq_account_source_system_event
    UNIQUE (account_id, source_system, source_event_id);