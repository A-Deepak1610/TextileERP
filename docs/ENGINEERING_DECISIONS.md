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

# 28. Interview Preparation Log

For every significant feature, maintain:

| Topic            | Why Used                      | Alternative   | Trade-off            | Interview Ready |
| ---------------- | ----------------------------- | ------------- | -------------------- | --------------- |
| PostgreSQL       | Relational + transactions     | MongoDB       | Scaling complexity   | ⬜               |
| Redis            | Reduce repeated reads         | DB only       | Cache invalidation   | ⬜               |
| RabbitMQ         | Async processing              | Synchronous   | Eventual consistency | ⬜               |
| JWT              | Stateless auth                | Sessions      | Token revocation     | ⬜               |
| Flyway           | DB migrations                 | Manual SQL    | Migration discipline | ⬜               |
| TanStack Query   | Server-state management       | Redux         | Learning curve       | ⬜               |
| Modular Monolith | Avoid premature microservices | Microservices | Shared deployment    | ⬜               |
| Docker           | Reproducible environment      | Local setup   | Container overhead   | ⬜               |

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
