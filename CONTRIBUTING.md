# Contributing Guidelines — TAMVA Backend

Thank you for contributing to TAMVA. To maintain high financial safety, security, and code quality, all contributions must adhere to these guidelines.

---

## 🌿 Branching Strategy

- `main` — Production-ready code.
- `hardening/*` — Security, architecture, and governance hardening.
- `feat/*` — Feature development.
- `fix/*` — Bug fixes.

---

## 📋 Pull Request Requirements

1. **All Tests Passing**: Every PR must pass `./gradlew clean test`.
2. **Container Build Check**: Docker build must complete cleanly.
3. **No Unsafe Fallbacks**: Hardcoded passwords or insecure defaults are strictly prohibited.
4. **API Contract Compatibility**: Swagger/OpenAPI annotations must be updated for any modified endpoints.