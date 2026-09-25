# TAMVA Backend — Financial Identity & Trust Infrastructure

TAMVA Backend is the central API service for TAMVA, providing multi-currency wallet management, financial identity verification, transaction ledgering, consent-based credit risk assessment, and administrative case operations across African financial ecosystems.

---

## 🏗️ Architecture & Core Stack

- **Java 21** / **Spring Boot 3.4.x**
- **PostgreSQL 17** (Relational storage & Flyway migration engine)
- **Redis 7** (Caching & session management)
- **Spring Security & OAuth2 / JWT** (Role-Based Access Control)
- **SpringDoc OpenAPI / Swagger 3.0** (Contract-first API documentation)

---

## 🚀 Environment Profiles

The backend enforces strict profile separation between environments:

| Profile | Purpose | Data Seeder | Secret Validation |
|---|---|---|---|
| `dev` / `local` | Local development | Enabled | Convenient defaults |
| `test` | Automated test suite | Disabled | Test credentials |
| `staging` | Staging & QA environment | Disabled | **Strict (Fails if insecure)** |
| `prod` | Official Production | Disabled | **Strict (Fails if insecure)** |

---

## 🔒 Security & Hardening Rules

1. **Admin Access Security**: Public admin signup is disabled. Administrative accounts are created exclusively by authorized `SUPER_ADMIN` accounts or controlled bootstrap seeder.
2. **Real Wallet Zero Balance**: Customer wallets initialize at zero (`0.00`) balance across all currencies. Demo balances exist strictly within dev data seeding.
3. **Double-Entry Financial Ledger**: Financial movements record balanced Debit and Credit entries with immutable history and reversal handling.
4. **Idempotency & Concurrency**: Row-level pessimistic locks (`PESSIMISTIC_WRITE`) prevent race conditions, overspending, or duplicate transactions.
5. **Fail-Safe Startup Validation**: In `staging` and `prod`, the application refuses to start if default development secrets or passwords are detected.

---

## 🧪 Running Locally & Testing

### Prerequisites
- Docker & Docker Compose
- Java 21 JDK

### Step-by-Step Setup
```powershell
# 1. Start PostgreSQL and Redis infrastructure
docker compose up -d

# 2. Copy environment template to .env
copy .env.example .env

# 3. Run the full verification test suite
./gradlew clean test --console=plain --no-daemon

# 4. Start the Spring Boot backend
./gradlew bootRun
```

---

## 📖 API Documentation & Swagger

Once the application is running, interact with the API contracts at:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🛡️ License

Proprietary — Ghana First / Africa Ready. Owned by TAMVA.