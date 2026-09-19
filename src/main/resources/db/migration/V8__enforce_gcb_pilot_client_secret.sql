-- Ensure the seeded GCB pilot application's client secret
-- matches the credential defined for the pilot environment.

UPDATE application
SET client_secret_hash = '$2a$10$qVArwVjcer47PjzxyqBEPupYHiPhd7lMFSTDO6Wt.f15549k2kL8O'
WHERE client_id = 'app_gcb_pilot_2026';
