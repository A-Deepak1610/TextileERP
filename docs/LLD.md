# TexForge — Low-Level Design (LLD): User Management Module

## 1. Module Overview & Package Architecture

The **User Management** module (`com.textile.erp.user`) encapsulates multi-tenancy, platform administration, organizational user identities, and role assignments following the **package-by-feature** architectural pattern.

### Package Layout
```text
com.textile.erp.user
│
├── entity
│   ├── Tenant.java          # Multi-tenant organization aggregate
│   ├── TenantStatus.java    # Enum: ACTIVE, SUSPENDED, INACTIVE
│   ├── User.java            # User identity entity (Platform & Tenant-scoped)
│   ├── UserStatus.java      # Enum: ACTIVE, INACTIVE, SUSPENDED
│   ├── Role.java            # System authorization role
│   ├── RoleName.java        # Enum: SUPER_ADMIN, TENANT_ADMIN, EMPLOYEE
│   ├── UserRole.java        # Composite junction entity
│   └── UserRoleId.java      # Embeddable composite key (userId, roleId)
│
├── repository
│   ├── TenantRepository.java    # Tenant CRUD and slug operations
│   ├── UserRepository.java      # Tenant/Platform scoped email lookups
│   ├── RoleRepository.java      # Role lookup by RoleName
│   └── UserRoleRepository.java  # Junction queries with JOIN FETCH
│
├── dto
│   ├── TenantRequestDto.java    # Tenant creation payload
│   ├── TenantResponseDto.java   # Safe tenant output
│   ├── UserCreateRequestDto.java # User creation payload
│   ├── UserResponseDto.java     # Safe user output with role enum names
│   └── RoleResponseDto.java     # Role metadata output
│
└── service
    ├── TenantService.java       # Tenant service contract
    ├── TenantServiceImpl.java   # Tenant business logic & validation
    ├── UserService.java         # User service contract
    └── UserServiceImpl.java     # Platform/Tenant hierarchy enforcement
```

---

## 2. Class Diagram

```mermaid
classDiagram
    class Tenant {
        +UUID id
        +String name
        +String slug
        +TenantStatus status
        +Instant createdAt
        +Instant updatedAt
    }

    class User {
        +UUID id
        +UUID tenantId
        +String email
        +String passwordHash
        +String firstName
        +String lastName
        +UserStatus status
        +Instant createdAt
        +Instant updatedAt
        +isPlatformUser() boolean
    }

    class Role {
        +Long id
        +RoleName name
        +String description
    }

    class UserRole {
        +UserRoleId id
        +User user
        +Role role
        +Instant assignedAt
    }

    class UserRoleId {
        +UUID userId
        +Long roleId
        +equals(Object) boolean
        +hashCode() int
    }

    class TenantStatus {
        <<enumeration>>
        ACTIVE
        SUSPENDED
        INACTIVE
    }

    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
    }

    class RoleName {
        <<enumeration>>
        SUPER_ADMIN
        TENANT_ADMIN
        EMPLOYEE
    }

    Tenant "1" <-- "0..*" User : tenant_id
    User "1" <-- "1..*" UserRole : user_id
    Role "1" <-- "0..*" UserRole : role_id
    UserRole *-- UserRoleId : composite key
    Tenant *-- TenantStatus
    User *-- UserStatus
    Role *-- RoleName
```

---

## 3. Authorization Hierarchy & Invariants

```text
TexForge Platform
│
└── SUPER_ADMIN (tenant_id = NULL)
      │
      ├── Tenant A (UUID-A)
      │    ├── TENANT_ADMIN (tenant_id = UUID-A)
      │    └── EMPLOYEE (tenant_id = UUID-A)
      │
      └── Tenant B (UUID-B)
           ├── TENANT_ADMIN (tenant_id = UUID-B)
           └── EMPLOYEE (tenant_id = UUID-B)
```

### Invariants Enforced in `UserServiceImpl`
1. **Platform Isolation**:
   * If `role == RoleName.SUPER_ADMIN`, `tenantId` MUST be `NULL`. If non-null, rejected with `IllegalArgumentException`.
   * Platform users are checked for uniqueness via `userRepository.existsByEmailAndTenantIdIsNull(email)`.
2. **Tenant Scoping**:
   * If `role != RoleName.SUPER_ADMIN`, `tenantId` MUST NOT be `NULL`.
   * The referenced `tenantId` must exist in `tenants` table (`tenantRepository.existsById(tenantId)`).
   * Email must be unique within that tenant (`userRepository.existsByTenantIdAndEmail(tenantId, email)`).
3. **Role Assignment**:
   * A `SUPER_ADMIN` role cannot be assigned to an existing tenant-scoped user.
   * A tenant role (`TENANT_ADMIN`, `EMPLOYEE`) cannot be assigned to a platform-level user.

---

## 4. DTO & API Boundary Isolation

JPA entities are strictly internal to the persistence and domain layers:
* **No Direct Entity Exposure**: Controllers and API contracts exchange exclusively `*RequestDto` and `*ResponseDto`.
* **Zero Lazy-Loading Leaks**: `UserResponseDto` embeds only detached values (`List<RoleName>`), preventing `LazyInitializationException` outside transactional contexts.
* **Sensitive Field Protection**: `password_hash` is never exposed in response DTOs.

---

## 5. Auth Module (`com.textile.erp.auth`)

### Package Layout
```text
com.textile.erp.auth
├── controller
│   ├── AuthController.java         # POST /api/auth/login, /refresh, /logout
│   └── AuthExceptionHandler.java   # Centralized HTTP 401/403/400 exception mapping
├── dto
│   ├── LoginRequestDto.java        # Email + password credentials
│   ├── LoginResponseDto.java       # Access token + refresh token + UserAuthDto
│   ├── RefreshTokenRequestDto.java # Raw refresh token payload
│   ├── RefreshTokenResponseDto.java# Rotated tokens response
│   ├── LogoutRequestDto.java       # Revocation target token
│   ├── LogoutResponseDto.java      # Simple acknowledgment message
│   └── UserAuthDto.java            # Safe authenticated user details
├── entity
│   └── RefreshToken.java           # DB-persisted hashed refresh token
├── repository
│   └── RefreshTokenRepository.java # Token hash lookups & user revocations
├── security
│   ├── CurrentUser.java            # Immutable authenticated security principal
│   ├── JwtAuthenticationFilter.java# Bearer token verification filter
│   └── SecurityUtils.java          # ThreadLocal SecurityContext accessor
├── service
│   ├── AuthService.java            # Authentication service contract
│   ├── AuthServiceImpl.java        # Login, refresh rotation, and logout logic
│   ├── JwtService.java             # HMAC-SHA256 JWT generation and parsing
│   └── RefreshTokenService.java    # Cryptographic generation & SHA-256 hashing
└── util
    └── TokenHashUtil.java          # Deterministic SHA-256 token hashing
```

### JWT Claims Specification
```text
{
  "sub": "018e3d64-8ab1-71b3-a18c-c60395bcf94a",       // User UUID
  "tenant_id": "018e3d64-77f2-70b1-91a0-d123456789ab", // Tenant UUID (null for SUPER_ADMIN)
  "roles": ["TENANT_ADMIN"],                            // List of assigned RoleName strings
  "email": "admin@tenant.com",
  "iat": 1788975000,
  "exp": 1788975900                                     // 15-minute default expiration
}
```

### Refresh Token Security & Rotation Strategy
1. **Zero Plaintext Storage**: Raw tokens are generated using cryptographically strong `SecureRandom` (32 bytes, URL-safe Base64), but only their SHA-256 hash (`token_hash`) is stored in `refresh_tokens`.
2. **Deterministic Lookup**: `TokenHashUtil.hashToken(raw)` enables efficient $O(1)$ indexed lookup while protecting tokens against database dump leakage.
3. **Single-Use Token Rotation**: Every call to `POST /api/auth/refresh` immediately revokes the submitted refresh token and issues a brand-new token pair.
4. **Logout Revocation**: Calling `POST /api/auth/logout` sets `revoked = true` and `revoked_at = CURRENT_TIMESTAMP`, preventing any future session reuse.

