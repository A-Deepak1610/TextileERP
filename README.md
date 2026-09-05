# TexForge

**TexForge** is a production-oriented multi-tenant Software-as-a-Service (SaaS) ERP platform designed for textile manufacturers. It covers operations across tenant management, authentication/authorization, inventory, raw materials, procurement, sales, production planning, machine management, invoicing, and reporting.

---

## 1. System Architecture Overview

TexForge is structured as a **modular monolith** with clean domain boundaries, designed for tenant isolation and future microservice extraction where required:

* **Backend (`textile-erp-api`)**: Java 17/25, Spring Boot 4, Spring Security, Spring Data JPA / Hibernate, Flyway migrations, PostgreSQL.
* **Frontend (`textile-erp-web`)**: React 19, TypeScript, Tailwind CSS, TanStack Query, Zustand, Axios, React Hook Form, Zod.
* **Infrastructure Dependencies**: Containerized PostgreSQL via Docker Compose (with Redis and RabbitMQ planned for upcoming modules).

### Multi-Tenancy Design
TexForge employs a **shared database with `tenant_id` isolation**:
* Every tenant-owned entity incorporates tenant context.
* Tenant context is strictly enforced server-side via authenticated security context / JWT claims, never trusting raw client parameters.
* Database indexes use tenant-aware composite keys (e.g., `(tenant_id, created_at)`).

---

## 2. Local Development Setup

During local development, infrastructure services run in Docker, while backend and frontend applications run natively on your machine for fast feedback loops.

```text
Host Machine
├── Docker Engine
│   └── texforge-postgres (PostgreSQL 16 on port 5432, persistent volume)
├── Local JVM
│   └── Spring Boot API (textile-erp-api)
└── Local Node.js
    └── React Frontend (textile-erp-web)
```

### Prerequisites
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v24+)
* [JDK 17 or JDK 25](https://www.oracle.com/java/technologies/downloads/)
* [Node.js](https://nodejs.org/) (v20+) & `npm`

---

## 3. Quickstart Guide

### Step 1: Clone and Configure Environment
Copy the environment template to create your local `.env` file:

```bash
cp .env.example .env
```

The default credentials in `.env.example` are preconfigured for local development:
* `POSTGRES_DB=texforge`
* `POSTGRES_USER=texforge_user`
* `POSTGRES_PASSWORD=texforge_password`
* `POSTGRES_PORT=5432`

### Step 2: Start PostgreSQL in Docker
Launch the database container in detached mode:

```bash
docker compose up -d
```

Verify that the container is running and healthy:

```bash
docker compose ps
```

You should see:
```text
NAME                IMAGE                STATUS
texforge-postgres   postgres:16-alpine   Up (healthy)
```

### Step 3: Run the Backend (`textile-erp-api`)
Navigate to `textile-erp-api` and start the Spring Boot application (Flyway will automatically validate and manage schema migrations on startup):

On Windows:
```powershell
cd textile-erp-api
.\mvnw.cmd spring-boot:run
```

On Linux/macOS:
```bash
cd textile-erp-api
./mvnw spring-boot:run
```

To run the backend test suite:
```powershell
.\mvnw.cmd test
```

### Step 4: Run the Frontend (`textile-erp-web`)
In a separate terminal, start the Vite development server:

```bash
cd textile-erp-web
npm install
npm run dev
```

The web application will be accessible at `http://localhost:5173`.

---

## 4. Docker Developer Commands

| Action | Command | Notes |
|---|---|---|
| **Start Database** | `docker compose up -d` | Starts PostgreSQL in background |
| **Check Health / Status** | `docker compose ps` | Displays container status and health |
| **View Live Logs** | `docker compose logs -f postgres` | Streams PostgreSQL container logs |
| **Open PSQL Shell** | `docker compose exec postgres psql -U texforge_user -d texforge` | Direct interactive database shell |
| **Stop Database** | `docker compose down` | Stops and removes container; **preserves** data volume |
| **Reset Database & Volume** | `docker compose down -v` | ⚠️ **Deletes persistent volume and all data** |

> [!WARNING]
> Do NOT use `docker compose down -v` during standard development workflows. The `-v` flag permanently removes the `texforge-postgres-data` volume and destroys all database records.

---

## 5. Engineering Documentation & Standards

For architectural guidelines, design decisions, multi-tenancy rules, and interview preparation logs, refer to:
* [docs/ENGINEERING_DECISIONS.md](docs/ENGINEERING_DECISIONS.md)
