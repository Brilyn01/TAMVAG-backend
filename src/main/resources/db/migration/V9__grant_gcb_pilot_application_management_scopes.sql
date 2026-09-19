-- Grant the GCB pilot application the permissions required
-- for MVP integration testing and resource discovery.

UPDATE application
SET scopes = '[
  "risk:evaluate",
  "profile:read",
  "consent:create",
  "connector:sync",
  "connector:read",
  "application:read",
  "application:manage"
]'
WHERE client_id = 'app_gcb_pilot_2026';