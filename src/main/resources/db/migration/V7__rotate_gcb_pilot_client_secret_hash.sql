-- Rotate the seeded GCB pilot application's client secret.

UPDATE application
SET client_secret_hash = '$2a$10$qVArwVjcer47PjzxyqBEPupYHiPhd7lMFSTDO6Wt.f15549k2kL8O'
WHERE client_id = 'app_gcb_pilot_2026';