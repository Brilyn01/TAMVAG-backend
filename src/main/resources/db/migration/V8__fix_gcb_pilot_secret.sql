-- V7 set client_secret_hash to a bcrypt hash that does not correspond
-- to the documented pilot secret ("gcb-pilot-secret-2026"), locking out
-- the GCB pilot client. Correct it here rather than editing the already
-- applied V7/V8 (which would break Flyway checksum validation).

UPDATE application
SET client_secret_hash = '$2b$10$buwPoyEtjsyNvc/ieFrUaOjI3jOoQymintFLVfHok2Y.BLfYChJOS'
WHERE client_id = 'app_gcb_pilot_2026';