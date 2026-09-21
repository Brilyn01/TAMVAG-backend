
-- Extend case records to support both automated risk-event cases
-- and manually created investigations.

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS institution_id UUID;

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS case_type VARCHAR(50);

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS customer_id UUID;

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS title VARCHAR(255);

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS priority VARCHAR(50);

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS resolution TEXT;

ALTER TABLE case_record
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;

-- Backfill institution ownership and defaults for existing
-- risk-event-linked cases.
UPDATE case_record c
SET institution_id = i.institution_id,
    case_type = COALESCE(c.case_type, 'RISK_EVENT'),
    priority = COALESCE(c.priority, 'NORMAL')
FROM risk_event r
JOIN account a
    ON a.account_id = r.account_id
JOIN institution i
    ON i.institution_id = a.institution_id
WHERE c.risk_event_id = r.risk_event_id
  AND c.institution_id IS NULL;

-- Apply defaults to any remaining existing records.
UPDATE case_record
SET case_type = COALESCE(case_type, 'RISK_EVENT'),
    priority = COALESCE(priority, 'NORMAL')
WHERE case_type IS NULL
   OR priority IS NULL;

-- Manual cases may have no associated risk event.
ALTER TABLE case_record
    ALTER COLUMN risk_event_id DROP NOT NULL;

ALTER TABLE case_record
    ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE case_record
    ALTER COLUMN case_type SET NOT NULL;

ALTER TABLE case_record
    ALTER COLUMN priority SET NOT NULL;

ALTER TABLE case_record
    ADD CONSTRAINT fk_case_record_institution
    FOREIGN KEY (institution_id)
    REFERENCES institution(institution_id);

ALTER TABLE case_record
    ADD CONSTRAINT chk_case_record_type
    CHECK (case_type IN ('RISK_EVENT', 'MANUAL'));

ALTER TABLE case_record
    ADD CONSTRAINT chk_case_record_risk_event_link
    CHECK (
        (case_type = 'RISK_EVENT' AND risk_event_id IS NOT NULL)
        OR
        (case_type = 'MANUAL')
    );

CREATE INDEX IF NOT EXISTS idx_case_record_institution_created
    ON case_record(institution_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_case_record_institution_status
    ON case_record(institution_id, status);

CREATE INDEX IF NOT EXISTS idx_case_record_institution_severity
    ON case_record(institution_id, severity);