-- V9 granted application:read / application:manage to the GCB pilot
-- client. ApplicationService had no per-institution scoping at the time,
-- so those two scopes let this partner client list/read/modify/rotate-
-- secret on every OTHER institution's application, not just its own.
-- Pull them back out. connector:read is unaffected and stays.

UPDATE application
SET scopes = '[
  "risk:evaluate",
  "profile:read",
  "consent:create",
  "connector:sync",
  "connector:read"
]'
WHERE client_id = 'app_gcb_pilot_2026';