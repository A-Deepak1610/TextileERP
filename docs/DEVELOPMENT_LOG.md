# TexForge — Engineering Development Log

## Milestone 1: Local Docker Environment Setup
* **Date**: 2026-09-05
* **Branch**: `chore/docker-postgres-setup`
* **Changes**:
  * Configured `docker-compose.yml` running PostgreSQL 16 Alpine on `localhost:5432` with healthcheck (`pg_isready`), dedicated non-root user `texforge_user`, and named persistent volume `texforge-postgres-data`.
  * Created `.env.example` and workspace `.gitignore`.
  * Configured Spring Boot `application.properties` with dynamic datasource properties and Flyway enabled.
  * Added automated integration tests verifying database connection and persistence across container restarts.
  * Authored **ADR-001** in `docs/ENGINEERING_DECISIONS.md`.

---

## Milestone 2: User Management & Multi-Tenant Database Foundation
* **Date**: 2026-09-06
* **Branch**: `feature/user-management-foundation`
* **Changes**:
  * Consolidated User Management and Multi-Tenancy into single domain module `com.textile.erp.user`.
  * Created Flyway migration `V1__init_user_management_schema.sql` creating `tenants`, `roles`, `users`, and `user_roles`.
  * Implemented UUIDv7 generation for `tenants.id` and `users.id` using Hibernate 7 `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)`.
  * Configured seeded system roles: `SUPER_ADMIN`, `TENANT_ADMIN`, `EMPLOYEE`.
  * Enforced platform vs. tenant hierarchy:
    * `SUPER_ADMIN`: `tenant_id = NULL`.
    * `TENANT_ADMIN` & `EMPLOYEE`: `tenant_id = valid tenant UUID`.
  * Configured compound uniqueness `UNIQUE(tenant_id, email)` and PostgreSQL partial unique index `uq_platform_user_email` for `tenant_id IS NULL`.
  * Designed `users.password_hash` as nullable to support future federated OAuth (Google / SSO) logins without placeholder passwords.
  * Implemented domain services (`TenantServiceImpl`, `UserServiceImpl`) with strict DTO boundaries (`TenantRequestDto`, `TenantResponseDto`, `UserCreateRequestDto`, `UserResponseDto`, `RoleResponseDto`).
  * Created comprehensive test suite `UserManagementIntegrationTest` verifying Flyway migrations, role seeding, hierarchy validation, cross-tenant email reuse, duplicate tenant email rejection, and database partial index enforcement.
  * Authored `docs/DATABASE_DESIGN.md`, `docs/LLD.md`, and updated `docs/ENGINEERING_DECISIONS.md` with ADR-002, ADR-003, and ADR-004.

---

## Milestone 3: Auth V1 & Multi-Tenant Security Architecture
* **Date**: 2026-09-09
* **Branch**: `feature/auth-v1`
* **Changes**:
  * Established separate `com.textile.erp.auth` module adhering to SOLID principles.
  * Created Flyway migration `V2__init_refresh_tokens.sql` with table `refresh_tokens`, indexing `user_id`, `token_hash`, and `expires_at`.
  * Implemented BCrypt password hashing via Spring Security `PasswordEncoder`, updating `UserServiceImpl` to enforce zero plaintext password persistence.
  * Implemented `JwtService` using JJWT 0.12 with HMAC-SHA256, signing tokens with claims: `sub` (userId), `tenant_id` (null for SUPER_ADMIN), `roles`, `email`.
  * Implemented immutable `CurrentUser` security principal abstraction and `SecurityUtils` helper.
  * Implemented `JwtAuthenticationFilter` (`OncePerRequestFilter`) populating Spring `SecurityContext` with `CurrentUser` and granted authorities.
  * Implemented `RefreshTokenService` featuring cryptographically secure random token generation, deterministic SHA-256 hashing (`TokenHashUtil`), single-use token rotation, and revocation on logout.
  * Built `AuthController` exposing `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout`.
  * Built `AuthExceptionHandler` mapping `BadCredentialsException` (401), `DisabledException` (403), and `IllegalArgumentException` (400) to standard JSON error responses.
  * Configured `SecurityConfig` with stateless session management, permitting public auth routes and health endpoints while securing all other endpoints.
  * Built `AuthIntegrationTest` verifying successful SuperAdmin/TenantAdmin login, invalid credentials rejection, inactive user rejection, refresh token rotation, revocation, logout, and protected endpoint access.
  * Authored ADR-005 in `docs/ENGINEERING_DECISIONS.md` and updated `docs/LLD.md`.

