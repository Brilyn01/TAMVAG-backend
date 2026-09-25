# TAMVA Security Policy & Vulnerability Reporting

## 🛡️ Supported Versions

| Version | Supported |
|---|---|
| 1.0.x (Main) | :white_check_mark: Yes |

---

## 🔒 Secret Management Policy

1. **No Hardcoded Secrets**: Secrets, tokens, and database credentials must never be committed to the repository.
2. **Environment Variable Injection**: All production secrets must be supplied via secure environment variables or secret store.
3. **Fail-Safe Startup**: Staging and production instances validate secret strength on application startup and fail-fast if default development credentials are present.

---

## 📩 Reporting a Vulnerability

If you discover a security vulnerability in TAMVA Backend:
- Email the TAMVA Security Team at **security@tamva.africa**.
- Do **not** open a public issue on GitHub.
- Include detailed reproduction steps and affected components.