# TexForge — Engineering Decision & Interview Playbook

> This document records important engineering decisions made during the development of TexForge.
>
> **Rule:** Whenever a significant technical decision is made, document:
>
> 1. What problem were we solving?
> 2. What did we choose?
> 3. Why did we choose it?
> 4. What alternatives did we consider?
> 5. What are the trade-offs?
> 6. What interview questions can be asked?
> 7. How will I explain the decision?

---

# 1. Project Overview

## Project

**TexForge — Multi-Tenant Textile Manufacturing SaaS**

## Goal

Build a production-oriented SaaS platform for textile manufacturers covering:

* Tenant management
* Authentication & authorization
* User management
* Products
* Raw materials
* Inventory
* Procurement
* Sales
* Production
* Machines
* Production planning
* Invoices
* Notifications
* Reporting
* Audit logs

The primary engineering focus is:

* Multi-tenancy
* Security
* Scalability
* Concurrency
* Performance
* Asynchronous processing
* Clean architecture
* Observability
* Cloud deployment
* CI/CD

---

# 2. Final Technology Stack

## Frontend

* React
* TypeScript
* Tailwind CSS
* TanStack Query
* Zustand
* React Hook Form
* Zod

## Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* Bean Validation
* Flyway
* PostgreSQL

## Infrastructure

* Redis
* RabbitMQ
* Docker
* AWS
* S3
* RDS PostgreSQL
* ECS/Fargate
* Application Load Balancer
* CloudWatch

## Testing

* JUnit
* Mockito
* Testcontainers

## CI/CD

* GitHub Actions

---

# 3. Architectural Principles

TexForge follows these principles:

* Modular Monolith first
* Domain-oriented module boundaries
* REST APIs
* Stateless backend
* API versioning
* Tenant isolation
* RBAC
* Database transactions
* Asynchronous processing for expensive operations
* Cache only where justified
* Failures must be observable
* Infrastructure should support horizontal scaling
* Avoid premature microservices

---

# 4. Backend Architecture

Initial architecture:

```text
Spring Boot
│
├── auth
├── tenant
├── user
├── product
├── inventory
├── procurement
├── sales
├── production
├── reporting
├── notification
└── audit
```

We intentionally start with a **modular monolith**.

The goal is to maintain clear domain boundaries so that high-load modules can be extracted into independent services later if necessary.

---

# 5. Package Structure

Use package-by-feature rather than package-by-layer.

Preferred:

```text
com.texforge
│
├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── inventory
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── production
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
└── shared
```

### Why?

Feature-based packaging keeps domain code together and makes future service extraction easier.

---

# 6. Multi-Tenancy Decision

## Decision

Use a **shared database with tenant_id** initially.

Example:

```text
orders
----------------
id
tenant_id
customer_id
amount
created_at
```

Every tenant-owned entity must contain tenant context where appropriate.

## Why?

* Lower infrastructure cost
* Easier initial development
* Efficient resource utilization
* Easier operational management
* Can support a large number of tenants

## Important requirement

Tenant isolation must be enforced server-side.

Never trust:

```text
tenantId
```

provided directly by the client.

Tenant context should come from authenticated identity/security context.

---

# 7. Database Indexing

Tenant-aware queries must be considered during index design.

Example:

```sql
CREATE INDEX idx_orders_tenant_created
ON orders(tenant_id, created_at);
```

For frequently filtered columns:

```sql
WHERE tenant_id = ?
AND status = ?
```

consider:

```sql
CREATE INDEX idx_orders_tenant_status
ON orders(tenant_id, status);
```

## Interview Questions

* Why include tenant_id in indexes?
* What happens if tenant_id is not indexed?
* How do composite indexes work?
* What is the leftmost-prefix rule?
* How would you optimize a slow tenant query?

---

# 8. API Versioning

All public APIs should be versioned.

Preferred:

```text
/api/v1/products
/api/v1/orders
/api/v1/inventory
```

Future breaking changes:

```text
/api/v2/products
```

## Why?

API consumers should not break when the backend evolves.

## Interview Questions

* Why API versioning?
* URI vs header versioning?
* When should v2 be introduced?
* How do you deprecate v1?
* How would you migrate existing clients?

---

# 9. API Design Guidelines

Use consistent REST conventions.

```text
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products
PATCH  /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

Use appropriate HTTP status codes.

Examples:

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

Use DTOs rather than exposing JPA entities directly.

---

# 10. Error Handling

Use centralized exception handling.

Preferred:

```text
Controller
    ↓
Service
    ↓
Exception
    ↓
Global Exception Handler
    ↓
Consistent Error Response
```

Example response structure:

```json
{
  "timestamp": "...",
  "status": 404,
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product was not found",
  "path": "/api/v1/products/123",
  "requestId": "..."
}
```

## Interview Questions

* Why global exception handling?
* Why shouldn't entities be returned directly?
* How do you design production-ready API errors?

---

# 11. Authentication

Use:

```text
Spring Security
+
JWT
```

Authentication flow:

```text
Login
 ↓
Validate credentials
 ↓
Generate access token
 ↓
Client sends Bearer token
 ↓
Spring Security validates token
 ↓
SecurityContext
 ↓
Controller
```

JWT should contain appropriate identity information, such as:

```text
userId
tenantId
roles
```

Do not put sensitive information into the JWT.

---

# 12. Authorization

Implement:

```text
Authentication
+
Tenant Authorization
+
Role-Based Access Control
+
Resource Authorization
```

Example:

```text
User
 ↓
Belongs to Tenant A
 ↓
Has INVENTORY_MANAGER role
 ↓
Can access Tenant A inventory
```

But cannot access:

```text
Tenant B inventory
```

even if the resource ID is known.

---

# 13. Redis

Redis should not be added simply because it is popular.

Use it where caching provides measurable benefit.

Potential uses:

* Frequently accessed product data
* Reference data
* Rate limiting
* Distributed locks where appropriate
* Temporary state

Cache key convention:

```text
tenant:{tenantId}:product:{productId}
```

Never use ambiguous global keys for tenant-specific data.

## Interview Questions

* Why Redis?
* What should and shouldn't be cached?
* What is cache invalidation?
* Cache-aside vs write-through?
* What happens when Redis goes down?
* How do you prevent cross-tenant cache leakage?

---

# 14. RabbitMQ

RabbitMQ will be used for asynchronous workloads.

Example:

```text
HTTP Request
     ↓
Create Job
     ↓
RabbitMQ
     ↓
Worker
     ↓
Process
     ↓
Update status
     ↓
Notify user
```

Potential use cases:

* Report generation
* Email notifications
* Audit processing
* Background processing
* Long-running jobs

## Important concepts to understand

* Producer
* Consumer
* Queue
* Exchange
* Routing key
* Acknowledgement
* Retry
* Dead-letter queue
* Idempotency
* Message durability

---

# 15. Transaction Management

Use database transactions for operations requiring atomicity.

Example:

```text
Create Order
+
Reduce Inventory
+
Create Order Items
```

These operations may need to succeed or fail together.

Understand:

* ACID
* Isolation levels
* Dirty reads
* Non-repeatable reads
* Phantom reads
* Optimistic locking
* Pessimistic locking

---

# 16. Inventory Concurrency

Inventory is a critical concurrency problem.

Example:

```text
Stock = 10

User A → purchases 7
User B → purchases 6
```

Without concurrency control, both requests might read:

```text
Stock = 10
```

and produce incorrect inventory.

Possible solutions:

* Optimistic locking
* Pessimistic locking
* Atomic SQL updates

The chosen solution must be documented with the actual use case.

---

# 17. Idempotency

Operations such as payments, order creation, and message processing may be retried.

Example:

```text
Client
 ↓
POST /orders
 ↓
Server processes
 ↓
Network timeout
 ↓
Client retries
```

The server should not accidentally create two orders.

Potential solution:

```text
Idempotency-Key
```

Example:

```text
Idempotency-Key: abc-123
```

Store and validate the key before repeating the operation.

---

# 18. Pagination

Never return thousands/millions of records in a single response.

Use pagination.

Initial approach:

```text
?page=0&size=20
```

For very large datasets, investigate cursor/keyset pagination.

Interview topics:

* Offset pagination
* Cursor pagination
* Keyset pagination
* Performance implications

---

# 19. Heavy Operations

Do not perform expensive operations synchronously.

Bad:

```text
HTTP Request
 ↓
Generate huge report
 ↓
30 seconds
 ↓
Response
```

Better:

```text
POST /reports
 ↓
Create report job
 ↓
RabbitMQ
 ↓
Worker
 ↓
Generate report
 ↓
S3
 ↓
Notify user
```

---

# 20. File Storage

Large files should not be stored directly in PostgreSQL.

Use:

```text
AWS S3
```

Potential files:

* Invoices
* Reports
* Product images
* Documents

Tenant-aware organization:

```text
tenant-A/
    invoices/
    reports/

tenant-B/
    invoices/
    reports/
```

---

# 21. Observability

Every request should be traceable.

Important fields:

```text
requestId
tenantId
userId
endpoint
HTTP method
status
latency
```

Monitor:

* Error rate
* Request rate
* p95 latency
* p99 latency
* DB performance
* Queue depth
* Cache hit ratio
* CPU
* Memory

---

# 22. Testing Strategy

## Unit tests

Test business logic independently.

```text
Service → JUnit + Mockito
```

## Integration tests

Test:

```text
Spring Boot
+
PostgreSQL
+
Redis
+
RabbitMQ
```

using Testcontainers where appropriate.

## API tests

Verify:

* Authentication
* Authorization
* Tenant isolation
* Validation
* Error responses

---

# 23. Git Strategy

Do not directly develop everything on `main`.

Branches:

```text
main
develop
feature/*
fix/*
refactor/*
docs/*
```

Example:

```text
feature/tenant-management
feature/jwt-authentication
feature/product-module
feature/inventory-concurrency
feature/redis-product-cache
feature/rabbitmq-reporting
```

Recommended workflow:

```text
main
  ↑
Pull Request
  ↑
feature/tenant-management
```

Every meaningful feature should go through a PR.

---

# 24. Commit Convention

Use meaningful commits.

Preferred format:

```text
feat: add tenant management
feat: implement JWT authentication
feat: add product CRUD APIs
feat: implement inventory reservation
fix: prevent cross-tenant product access
perf: add tenant-aware product indexes
refactor: separate inventory domain service
test: add tenant isolation integration tests
docs: document multi-tenancy decision
chore: configure Docker development environment
```

Avoid:

```text
update
changes
final
done
working
fix
```

---

# 25. Pull Request Guidelines

Every PR should contain:

## What

What was implemented?

## Why

Why was this change needed?

## How

How was it implemented?

## Testing

How was it tested?

## Design Decisions

Were there important technical decisions?

## Example

```text
## What
Implemented tenant-aware product APIs.

## Why
Products belong to individual organizations and must not be
accessible across tenants.

## How
Added TenantContext and repository-level tenant filtering.

## Testing
Added integration tests verifying Tenant A cannot access
Tenant B products.

## Design Decision
Tenant ID is obtained from authenticated security context
rather than request parameters.
```

---

# 26. Definition of Done

A feature is not considered complete simply because the API works.

Before marking a feature complete:

* [ ] Business logic implemented
* [ ] Validation added
* [ ] Authorization checked
* [ ] Tenant isolation verified
* [ ] Error handling added
* [ ] Database migration created
* [ ] Appropriate indexes considered
* [ ] Unit tests added
* [ ] Integration tests added where needed
* [ ] API documented
* [ ] Logging considered
* [ ] Performance considered
* [ ] Git commit created
* [ ] Pull request created
* [ ] Engineering decision documented if significant

---

# 27. Engineering Decision Template

Whenever an important decision is made, add:

## ADR: [Decision Name]

### Context
What problem are we solving?

### Decision
What did we choose?

### Why?
Why did we choose it?

### Alternatives
What else could we have used?

### Trade-offs
What are the advantages and disadvantages?

### Consequences
What does this decision affect?

### Interview Questions
1. Why did you choose this?
2. What alternatives did you consider?
3. What are the disadvantages?
4. When would you change this decision?
5. How does this scale?

### My Interview Answer
Write a 30–60 second explanation in your own words.

---

## ADR-001: Local PostgreSQL Development via Docker Compose

### Context
During early development of TexForge, developers need a reliable, isolated, and consistent PostgreSQL database environment that mirrors production PostgreSQL characteristics without polluting developer host operating systems or causing "works on my machine" version divergence.

### Decision
Use Docker Compose to run a containerized PostgreSQL 16 instance (`postgres:16-alpine`) exposing `localhost:5432` with:
* A dedicated application database: `texforge`
* A dedicated non-root application user: `texforge_user`
* Parameterized configuration via `.env` and `.env.example`
* Named volume data persistence: `texforge-postgres-data`
* Connection-level health check: `pg_isready -U texforge_user -d texforge`
* Restart policy: `unless-stopped`

At this stage, keep the Spring Boot backend and React frontend running natively on the host machine. Do not containerize application code, Redis, or RabbitMQ until those architectural layers are actively implemented.

### Why?
* **Environment Consistency**: Guarantees identical PostgreSQL version and extensions across Windows, macOS, and Linux developer environments.
* **Rapid Onboarding**: A new engineer can clone the repo, run `docker compose up -d`, and immediately have a working database.
* **Safe State Persistence**: Named Docker volume preserves database records across container restarts (`docker compose down` followed by `docker compose up -d`).
* **Clean Isolation**: Can be completely wiped and re-created cleanly without uninstalling or altering host services.
* **Developer Velocity**: Running Spring Boot and Vite natively avoids Docker rebuild latency and allows seamless IDE breakpoint debugging and Hot-Module-Replacement (HMR).

### Alternatives Considered
1. **Bare-metal Local PostgreSQL Installation**:
   * *Rejected*: Causes version conflicts, machine-specific pathing, configuration drift, and difficult cleanup across different developer operating systems.
2. **In-Memory Database (e.g., H2)**:
   * *Rejected*: H2 does not support PostgreSQL dialect specifics, JSONB functions, composite index subtleties, or strict transaction/locking behavior needed for multi-tenant ERP operations.
3. **Full Containerization (Dockerizing Backend, Frontend, and DB together)**:
   * *Rejected for early development*: Slower developer feedback loop, requires rebuilding containers on code edits, complicates IDE step-debugging, and adds premature operational complexity before the core application code stabilizes.

### Trade-offs
* **Prerequisites**: Developers must have Docker Desktop installed and running.
* **Resource Overhead**: Docker Desktop/WSL2 consumes a baseline amount of host RAM and CPU.
* **Volume Hygiene**: Developers must be trained not to use `docker compose down -v` unless they explicitly intend to destroy their local test data.

### Consequences
* Database credentials and ports are standardized in `.env.example` and injected into Spring Boot via `application.properties` with fallback defaults.
* Database schema changes are strictly governed by Flyway migrations; direct manual table alterations inside the container are prohibited.

### Interview Questions
1. *Why did you choose Docker Compose for local PostgreSQL rather than having developers install it natively?*
2. *Why not containerize the Spring Boot application and React frontend at this stage as well?*
3. *How do you ensure data is not lost when stopping containers? What is the difference between `docker compose down` and `docker compose down -v`?*
4. *How does the Docker health check work, and why is `pg_isready` preferred over checking if the container process is running?*
5. *Why is using a dedicated application database user preferred over the default `postgres` superuser?*

### My Interview Answer
"For TexForge, we adopted a hybrid local development approach: infrastructure dependencies like PostgreSQL run containerized via Docker Compose, while our Spring Boot application and React frontend run natively on the host. 

This gives us the best of both worlds. By containerizing PostgreSQL with a specific Alpine image and named volume, we eliminate environment drift, prevent 'works on my machine' issues, and guarantee dialect parity without polluting the host OS. At the same time, keeping application code on the host preserves sub-second hot reload and seamless IDE debugging. We configured active health checking with `pg_isready` and parameterized all connection variables through `.env` following 12-factor app principles."

---

## ADR-002: UUIDv7 for Multi-Tenant Entity Primary Keys

### Context
In a multi-tenant SaaS ERP, business entities such as Tenants and Users require globally unique identifiers. Traditional auto-increment integers (`BIGINT`) present security risks (enumeration attacks, leaking tenant business volume/growth) and complicate future data sharding or replication. Conversely, traditional random UUIDs (UUIDv4) cause severe B-tree index fragmentation and random disk I/O under high write volumes.

### Decision
Use **RFC 9562 UUIDv7** identifiers for all core business entities (`tenants.id`, `users.id`) using Hibernate 7's `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` mapped to PostgreSQL native `UUID` column types. Global reference data (`roles.id`) continues to use `BIGSERIAL`.

### Why?
* **Index Locality & Write Throughput**: UUIDv7 embeds a 48-bit millisecond Unix timestamp in its most-significant bits. New keys sort chronologically, causing B-tree insertions to append monotonically to the right-hand edge of the index. This keeps hot index pages in PostgreSQL `shared_buffers` and avoids random leaf-page splits.
* **Enumeration Defense**: UUIDv7 includes 74 bits of cryptographically secure pseudo-randomness, making IDs unguessable and preventing URL ID scraping.
* **Distributed Generation**: IDs can be generated on application nodes without roundtrips to database sequences or coordination locks.

### Alternatives Considered
1. **Auto-Increment BIGINT**:
   * *Rejected*: Exposes sequential numbers in REST URLs (`/api/v1/tenants/1`), allowing competitors to guess tenant counts and transaction volumes.
2. **Random UUIDv4**:
   * *Rejected*: Completely random distribution destroys B-tree index page locality once the index exceeds RAM, causing up to 50% index bloat and excessive disk thrashing.
3. **Twitter Snowflake / ULID**:
   * *Rejected*: Snowflake requires central worker ID coordination; ULID requires custom conversion for PostgreSQL native UUID storage. UUIDv7 is the IETF standard (RFC 9562) supported directly by Hibernate 7 and PostgreSQL.

### Trade-offs
* Consumes 16 bytes compared to 8 bytes for `BIGINT`.
* Slightly higher storage overhead in foreign key indexes, fully justified by security, sharding readiness, and monotonic write performance.

### Interview Questions
1. *What is UUIDv7 and how does it differ from UUIDv4?*
2. *Why do random UUIDs (UUIDv4) hurt B-tree index performance in PostgreSQL?*
3. *How does UUIDv7 avoid page splits in relational databases?*
4. *When would you still use BIGSERIAL instead of UUIDv7?*

### My Interview Answer
"In TexForge, we chose UUIDv7 for all primary keys on business entities like tenants and users. Standard UUIDv4 values are completely random, which scatters inserts across the entire B-tree index and causes heavy disk I/O and page splits once tables grow beyond memory cache. UUIDv7 solves this by encoding a 48-bit millisecond timestamp in the leading bits, making keys time-ordered and monotonic like an auto-incrementing integer, while retaining 74 bits of randomness to prevent enumeration attacks and eliminate coordination bottlenecks across distributed nodes."

---

## ADR-003: Platform vs. Tenant User Hierarchy & Compound Email Uniqueness

### Context
TexForge serves two distinct categories of users:
1. **Platform Administrators (`SUPER_ADMIN`)**: Oversee platform health, billing, and tenant onboarding across the entire SaaS.
2. **Tenant Users (`TENANT_ADMIN`, `EMPLOYEE`)**: Scoped strictly to an individual textile mill organization.

Additionally, in textile manufacturing, external auditors, suppliers, or specialized contractors may work with multiple independent tenant mills using a single professional email address.

### Decision
1. **Hierarchy Rule**:
   * `SUPER_ADMIN`: `tenant_id = NULL` (Platform-level user).
   * `TENANT_ADMIN` & `EMPLOYEE`: `tenant_id = valid tenant UUID` (Tenant-scoped user).
2. **Compound Uniqueness**:
   * `CONSTRAINT uq_users_tenant_email UNIQUE(tenant_id, email)`: Enforces that an email is unique within a tenant, but allows the same email to exist across different tenants.
3. **Partial Unique Index for Platform Users**:
   * Because PostgreSQL treats multiple `NULL` values in standard `UNIQUE(tenant_id, email)` as distinct, we added a partial unique index:
     `CREATE UNIQUE INDEX uq_platform_user_email ON users(email) WHERE tenant_id IS NULL;`
   * This guarantees that platform administrator emails cannot be duplicated.

### Why?
* **Multi-Tenant Flexibility**: Allows cross-tenant contractor or auditor collaboration without requiring artificial email aliases (`john+mill1@gmail.com`).
* **Platform Security**: Prevents identity collision and privilege escalation by separating platform administrative accounts from tenant-level organizational boundaries.

### Alternatives Considered
1. **Globally Unique Email Across Entire Database**:
   * *Rejected*: Prevents users from participating in multiple tenant mills with their primary work email, and leaks tenant membership during registration.
2. **System Tenant Row for Platform Admins**:
   * *Rejected*: Introduces an artificial "system" tenant that pollutes tenant-level reporting and complicates tenant listing queries.

### Interview Questions
1. *Why did you make `tenant_id` nullable in the `users` table?*
2. *How does PostgreSQL handle `NULL` in compound unique constraints?*
3. *What is a partial index in PostgreSQL, and why did you use it for platform users?*
4. *How does your design allow cross-tenant email reuse while preserving tenant isolation?*

### My Interview Answer
"We separated user identity into platform-level and tenant-level scopes. A `SUPER_ADMIN` has `tenant_id = NULL` because they govern the platform, whereas `TENANT_ADMIN` and `EMPLOYEE` require a non-null tenant reference. To support contractors who work across multiple textile mills, we applied a compound unique constraint on `(tenant_id, email)`. Because standard SQL treats NULLs as distinct in unique constraints, we also added a PostgreSQL partial unique index on `email WHERE tenant_id IS NULL`. This guarantees platform emails remain globally unique while enabling legitimate cross-tenant email reuse."

---

## ADR-004: Nullable Password Hash for Federated OAuth Compatibility

### Context
TexForge is architected to support both native email/password authentication and federated Single Sign-On (Google OAuth, Microsoft Entra). If the `password_hash` column is marked `NOT NULL`, registering an OAuth user would require storing a fake, randomly generated dummy password hash.

### Decision
Define `users.password_hash` as `NULLABLE`. Users registering or signing in exclusively via OAuth maintain `password_hash = NULL`. Future federated account links will be maintained in a separate `user_oauth_accounts` table.

### Why?
* **Security Hygiene**: Generating fake password hashes for SSO users creates confusion, potential bypass attack surfaces, and complicates password reset workflows.
* **Clean Account State**: A `null` password hash immediately communicates that the user has not established local credentials, allowing the frontend to prompt for password setup if desired.

### Alternatives Considered
1. **Generating Dummy Password Hashes**:
   * *Rejected*: Unnecessary hashing overhead, misleading account state, and potential security risk if the dummy generator has low entropy.
2. **Separate Tables for Native vs. OAuth Users**:
   * *Rejected*: Unnecessary duplication of profile, auditing, and role assignment logic.

### Interview Questions
1. *Why should password_hash be nullable when designing for OAuth?*
2. *How do you prevent an OAuth user from logging in via standard username/password forms if password_hash is null?*

### My Interview Answer
"We made `password_hash` nullable in the `users` table to accommodate federated OAuth authentication cleanly. When a user logs in via Google or SSO, there is no local password to hash. Storing random dummy hashes is an anti-pattern that creates security ambiguities and complicates account recovery. A null hash cleanly designates that local password authentication is disabled for that account until explicitly configured by the user."

---

# 28. Interview Preparation Log

For every significant feature, maintain:

| Topic            | Why Used                                               | Alternative                  | Trade-off                     | Interview Ready |
| ---------------- | ------------------------------------------------------ | ---------------------------- | ----------------------------- | --------------- |
| PostgreSQL       | Relational integrity, ACID transactions, multi-tenancy | MongoDB                      | Scaling complexity            | ✅               |
| Docker           | Reproducible local DB environment & volume persistence | Local bare-metal install, H2 | Docker Desktop overhead       | ✅               |
| UUIDv7           | Monotonic B-tree locality + non-enumerable security    | Auto-increment, UUIDv4       | 16 bytes vs 8 bytes           | ✅               |
| Multi-Tenancy    | Shared DB with tenant_id, compound uniqueness          | DB-per-tenant, schema-per-tenant | Isolation discipline       | ✅               |
| Flyway           | DB migrations & schema versioning                      | Manual SQL                   | Migration discipline          | ✅               |
| Partial Indexes  | Uniqueness enforcement on nullable subset columns      | Full unique index, triggers  | PostgreSQL-specific feature   | ✅               |
| Redis            | Reduce repeated reads                                  | DB only                      | Cache invalidation            | ⬜               |
| RabbitMQ         | Async processing                                       | Synchronous                  | Eventual consistency          | ⬜               |
| JWT              | Stateless auth                                         | Sessions                     | Token revocation              | ⬜               |
| TanStack Query   | Server-state management                                | Redux                        | Learning curve                | ⬜               |
| Modular Monolith | Avoid premature microservices                          | Microservices                | Shared deployment             | ⬜               |

Update this table as the project evolves.

---

# 29. Resume Evidence

Never put an unmeasured performance claim on the resume.

Record actual measurements here.

Example:

```text
API:
GET /products

Before optimization:
p95 = ___ ms

After Redis:
p95 = ___ ms

Test:
___ concurrent users
___ requests
```

Another example:

```text
Database:

Dataset:
___ orders

Query before index:
___ ms

Query after index:
___ ms
```

Resume numbers must come from actual experiments.

---

# 30. Scalability Experiments

As the project matures, conduct experiments such as:

* 100 tenants
* 1,000 tenants
* 10,000 tenants
* 100K products
* 1M orders
* Concurrent inventory updates
* Redis failure
* RabbitMQ consumer failure
* Database connection exhaustion
* Slow database query
* API load testing

Record:

```text
Test
Environment
Load
Result
Bottleneck
Optimization
Result after optimization
```

---

# 31. Important Interview Topics

While building TexForge, I should be able to explain:

## Backend

* Spring Boot architecture
* Dependency injection
* REST
* DTOs
* Validation
* Exception handling
* Transactions
* JPA/Hibernate
* N+1 problem
* Lazy vs eager loading
* Pagination
* Database indexing

## Security

* Authentication
* Authorization
* JWT
* RBAC
* Tenant isolation
* Password hashing
* Token expiration
* Refresh tokens
* CORS
* CSRF

## Databases

* ACID
* Isolation levels
* Locks
* Indexes
* Query optimization
* Replication
* Partitioning
* Connection pooling

## Distributed Systems

* Caching
* Message queues
* Retry
* Dead-letter queues
* Idempotency
* Eventual consistency
* Load balancing
* Horizontal scaling

## System Design

* Scalability
* Availability
* Consistency
* Fault tolerance
* Rate limiting
* Backpressure
* Observability
* Disaster recovery

---

# 32. Golden Rule

Do not add technology because it looks impressive on a resume.

Instead:

```text
Problem
   ↓
Requirement
   ↓
Possible solutions
   ↓
Trade-off analysis
   ↓
Engineering decision
   ↓
Implementation
   ↓
Measurement
   ↓
Document decision
```

The goal of TexForge is not to demonstrate that we know many technologies.

The goal is to demonstrate that we can make **good engineering decisions**.

---

## ADR-005: Auth V1 — JWT, Multi-Tenant Claim Propagation, and Hashed Refresh Token Rotation

### Context
TexForge is a multi-tenant SaaS ERP where requests must be strictly scoped to the authenticated organization while supporting platform-level administration (`SUPER_ADMIN`). To achieve high-throughput API communication without hitting the database on every micro-request, we require stateless JWT access tokens accompanied by secure, revocable refresh tokens for session management.

### Decision
1. **Modular Auth Isolation**:
   * Encapsulate authentication concerns within a dedicated package: `com.textile.erp.auth`.
   * The User module (`com.textile.erp.user`) retains user entities, lifecycle, and organizational persistence; the Auth module owns credentials verification, token generation, refresh tokens, and security filters.
2. **Password Security**:
   * Standardize on BCrypt via Spring Security's `PasswordEncoder`. Plaintext passwords are strictly forbidden.
3. **Stateless JWT Claims & Multi-Tenancy**:
   * Access tokens are signed using HMAC-SHA256 (`jjwt-api` 0.12.x) with short expiration (15 minutes).
   * JWT standard & custom claims:
     - `sub`: User UUID (`id`)
     - `tenant_id`: Tenant UUID (or `null` strictly for `SUPER_ADMIN`)
     - `roles`: Assigned roles (`["SUPER_ADMIN"]`, `["TENANT_ADMIN"]`, etc.)
     - `email`: Authenticated email
   * In-memory principal abstraction: `CurrentUser` record injected into Spring's `SecurityContext`. Downstream services read `tenantId` from `CurrentUser`, never from unverified request parameters.
4. **Database-Persisted Hashed Refresh Tokens**:
   * Refresh tokens are 32-byte cryptographically secure random tokens (`SecureRandom`, URL-safe Base64).
   * **Zero Plaintext Storage**: Only the deterministic SHA-256 hash (`TokenHashUtil.hashToken(raw)`) is stored in `refresh_tokens.token_hash`. If the database is compromised, active session refresh tokens cannot be forged.
   * **Single-Use Rotation**: Calling `POST /api/auth/refresh` immediately revokes the used refresh token and returns a fresh token pair.
   * **Logout by Revocation**: `POST /api/auth/logout` revokes the refresh token (`revoked = true`, `revoked_at = NOW()`), immediately preventing further session extensions.

### Trade-offs & Alternatives Considered
1. **Stateful Server-Side Sessions (Redis / DB)**:
   * *Rejected*: Requires persistent session store lookups on every single HTTP request, adding network hops and latency to ERP transactions.
2. **Plaintext Refresh Tokens in Database**:
   * *Rejected*: A database breach would grant attackers persistent access to user accounts. Hashing with SHA-256 provides $O(1)$ indexed lookup while keeping token values secret.
3. **Accepting `tenantId` in Login Request**:
   * *Rejected*: Dangerous security vulnerability. Identity and tenant scoping must be resolved solely from authenticated database state and cryptographically signed JWT claims.

