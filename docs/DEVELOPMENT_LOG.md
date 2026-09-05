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
