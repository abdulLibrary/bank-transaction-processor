# Banking Transaction Processor

Production-grade implementation of the NatWest APIM coding kata.

## Features implemented

| Requirement | Implementation |
|-------------|----------------|
| Persistent storage | PostgreSQL + Flyway (H2 for local/demo) |
| Optimistic locking | `@Version` on `Account` + retry on conflict |
| Domain events + Outbox | `TransferCompleted`, `MoneyDeposited`, `MoneyWithdrawn` → transactional outbox → scheduled publisher |
| Idempotency keys | `Idempotency-Key` header for deposit / withdraw / transfer |
| OpenAPI 3 | springdoc-openapi → `/swagger-ui.html` |
| Resilience4j | Circuit breaker + rate limiter on money-moving operations |
| Structured logging + tracing | Logstash JSON encoder + Micrometer Tracing (Brave) |
| Clean domain + tests | Aggregate root, value objects, comprehensive tests |
| JWT Resource Server | OAuth2 Resource Server – validates JWT only; roles USER / ADMIN |

## Tech stack

- Java 17
- Spring Boot 3.3
- Spring Data JPA + Flyway
- Spring Security OAuth2 Resource Server
- PostgreSQL (production) / H2 (local & tests)
- Resilience4j
- springdoc-openapi
- Micrometer Tracing + Logstash encoder

## Quick start

```bash
# Run against in-memory H2 (no external DB required)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run tests
mvn clean test
```

Swagger UI: http://localhost:8080/swagger-ui.html  
Health:     http://localhost:8080/actuator/health

### PostgreSQL (production profile)

```bash
docker run -d --name bank-pg -e POSTGRES_DB=bankdb -e POSTGRES_USER=bank -e POSTGRES_PASSWORD=bank -p 5432:5432 postgres:16
mvn spring-boot:run
```

## Security (JWT Resource Server)

This service is a **pure OAuth2 Resource Server**. A separate Auth service issues JWTs; this service only validates them.

### Role model

| Role  | Permissions |
|-------|-------------|
| `ADMIN` | Create account, deposit, withdraw, transfer, read balance |
| `USER`  | Deposit, withdraw, transfer, read balance |

Unauthenticated requests to `/api/**` return **401**. Wrong role returns **403**.

### Calling the API (Authorization header required)

```bash
# Example: deposit as USER
curl -X POST http://localhost:8080/api/v1/accounts/ACC-001/deposits \
  -H "Authorization: Bearer <your-jwt>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -d '{"amount":"50.00"}'
```

### Local demo JWT (HS256)

For local testing the service uses a shared secret (see `app.security.jwt.secret` in `application.yml`).

Generate a token at https://jwt.io with:

- Algorithm: **HS256**
- Secret: `ThisIsADevOnlySecretKeyThatIsAtLeast32BytesLong!!`
- Payload example (USER):

```json
{
  "sub": "user-123",
  "roles": ["USER"],
  "iat": 1700000000,
  "exp": 1900000000
}
```

- Payload example (ADMIN):

```json
{
  "sub": "admin-1",
  "roles": ["ADMIN"],
  "iat": 1700000000,
  "exp": 1900000000
}
```

Copy the encoded token and pass it as `Authorization: Bearer <token>`.

### Production configuration

Point the Resource Server at your Auth server’s JWKS / issuer:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
          # or jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

Then remove (or profile-gate) the secret-based `JwtDecoder` bean in `SecurityConfig` so Spring Boot auto-configures from the issuer.

## API overview

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/v1/accounts` | ADMIN | Open account |
| GET  | `/api/v1/accounts/{id}/balance` | USER, ADMIN | Balance |
| POST | `/api/v1/accounts/{id}/deposits` | USER, ADMIN | Deposit (`Idempotency-Key` supported) |
| POST | `/api/v1/accounts/{id}/withdrawals` | USER, ADMIN | Withdraw (`Idempotency-Key` supported) |
| POST | `/api/v1/accounts/transfers` | USER, ADMIN | Transfer (`Idempotency-Key` supported) |

### Idempotency example (with JWT)

```bash
curl -X POST http://localhost:8080/api/v1/accounts/ACC-001/deposits \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -d '{"amount":"50.00"}'
```

Replaying the same key + same payload returns the original response without side-effects.

## Design decisions & trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| `Money` value object + JPA converter | Precise monetary arithmetic | Slightly more ceremony |
| Account aggregate owns ledger | Strong consistency of the aggregate | Larger write set on every mutation |
| Optimistic locking + limited retry | Handles concurrent transfers cleanly | Rare conflicts surface as 409 |
| Transactional Outbox | Guarantees at-least-once event delivery without dual-write | Requires a background publisher |
| Idempotency keyed by (key, operation) | Safe retries from clients | Client must supply a unique key |
| Resilience4j on service methods | Protects downstream / limits abuse | Configuration is environment-specific |
| Flyway + `ddl-auto=validate` | Schema is the source of truth | Schema changes require migrations |

## Outbox pattern

1. Business transaction writes the domain change **and** an `outbox_events` row in the same DB transaction.
2. A scheduled publisher polls `PENDING` events and “publishes” them (currently logs them; replace with Kafka/SNS in production).
3. Event is marked `PUBLISHED`.

This gives reliable transfer notifications without distributed transactions.

## Git history philosophy

Commits are intentionally small and narrative:

1. Domain model + Money value object
2. JPA entities + Flyway schema
3. Account aggregate with optimistic locking
4. BankService + transactional boundaries
5. Outbox + domain events
6. Idempotency support
7. REST API + OpenAPI
8. Resilience4j + observability
9. Tests & documentation

This history demonstrates the iterative refinement that NatWest values.

## What remains for a full production system

- Replace log-based outbox publisher with Kafka / EventBridge
- Add distributed tracing exporter (Zipkin / OTLP)
- Horizontal scaling + connection pooling tuning
- Multi-currency support
- Full audit / compliance retention policies
