# SpecialService — Enterprise Resource Planning & Business Management Platform Backend

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-green.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Swagger / OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-85EA2D.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui/index.html)
[![JWT Auth](https://img.shields.io/badge/Auth-JJWT%20(Stateless)-black.svg?style=flat-square&logo=jsonwebtokens)](https://jwt.io/)

**SpecialService** is an enterprise-grade, multi-tenant backend architecture designed to manage and automate end-to-end operations for manufacturing, multi-warehouse inventory logistics, financial accounting, human resources, invoice management, and construction project execution.

---

## Architecture Overview

The system is developed following modern enterprise **Clean Layered Architecture** principles, enforcing separation of concerns, transactional integrity, and strong multi-tenancy boundaries:

```
                  ┌──────────────────────────────────────────┐
                  │          Client Applications             │
                  │   (React / Vue / Mobile POS Clients)     │
                  └────────────────────┬─────────────────────┘
                                       │ HTTPS / REST (JSON)
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │            API Gateway / Filter          │
                  │    - JwtAuthFilter (Bearer Auth)         │
                  │    - ModuleAccessInterceptor (RBAC)      │
                  │    - GlobalExceptionHandler              │
                  └────────────────────┬─────────────────────┘
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │             REST Controllers             │
                  │   (Auth, Inventory, Invoice, HR, etc.)   │
                  └────────────────────┬─────────────────────┘
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │               Service Layer              │
                  │   - Business Logic & Workflow Engine     │
                  │   - Optimistic Locking Reconciliation    │
                  │   - Automated Schedulers                 │
                  └────────────────────┬─────────────────────┘
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │          Data Access Layer (JPA)         │
                  │   - Spring Data Repositories             │
                  │   - Multi-tenant Isolated Queries        │
                  └────────────────────┬─────────────────────┘
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │          PostgreSQL Database             │
                  └──────────────────────────────────────────┘
```

---

## Core Modules & Capabilities

### 1. Multi-Tenant Architecture & Dynamic RBAC
- **Multi-Company Isolation:** Every core entity is scoped by `company_id`, guaranteeing data segregation across tenants.
- **Dynamic Module Subscriptions:** Tenants can subscribe to specific operational modules (`INVOICE`, `INVENTORY`, `HR`, `MANUFACTURING`, `FINANCE`, `CONSTRUCTION`, `SERVICE`, `DASHBOARD`).
- **Granular Permissions:** Custom role templates, module-level access control, and hierarchical user privileges (`SUPER_ADMIN`, `COMPANY_ADMIN`, `USER_ADMIN`, `USER`).

### 2. Multi-Warehouse & Inventory Logistics
- **Multi-Warehouse Stock Management:** Track inventory quantities, movements, reservations, and transfers across multiple physical warehouses.
- **Optimistic Concurrency Control:** Enforces `@Version` tokens on stock and product entities to eliminate race conditions during high-volume sales and stock transfers.
- **Barcode Engine & External Lookup:** Built-in barcode generation via **ZXing** and automatic fallback lookup against external global catalog databases for unrecognized barcodes.
- **Stock Audits & Reconciliations:** Comprehensive stock count workflows (`StockCount`) with itemized variance reports between expected and actual counts.

### 3. Manufacturing & Production Pipeline
- **Bill of Materials (BOM) & Recipes:** Multi-ingredient tracking with specified consumption rates from specific warehouses.
- **Multi-Stage Production Orders:** Track production lifecycle (`PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).
- **Semi-Finished Products (WIP):** Support for multi-tier manufacturing chains where output from one order serves as raw material for subsequent assemblies.
- **Indirect Expense Allocation:** Direct and indirect manufacturing cost distributions (labor, electricity, equipment amortisation) factored into final product cost calculations.

### 4. Billing, Invoices & VAT Engine
- **Sales & Purchase Invoices:** Invoices with itemized price, discount, and VAT status (`WITH_VAT`, `WITHOUT_VAT`, `EXEMPT`).
- **Sequential Invoice Number Generation:** Auto-incrementing, collision-safe invoice sequence generator per tenant.
- **Installment Payment Tracking:** Support for installment schedules, payment status monitoring, and late payment alerts.

### 5. Financial Ledger & Cashflow (Kassa)
- **Multi-Account Kassa System:** Cash registers, bank accounts, and currency wallets with real-time balance calculations.
- **Multi-Currency Support:** Foreign currency exchange rates and automated conversion for global trade operations.
- **Financial Categorization:** Inflow and outflow classification for income statement generation and balance sheet analytics.

### 6. Human Resources & Payroll
- **Employee Lifecycle Management:** Employee onboarding, department assignment, contract management, and document archiving.
- **Attendance & Leave Management:** Leave request approvals (`VACATION`, `SICK`, `UNPAID`), balance tracking, and audit trails.
- **Automated Payroll Engine:** Calculation of base salary, bonuses, advance payments, deductions, and tax withholdings.

### 7. Construction & Project Execution
- **Project Work Breakdown Structure:** Track multi-phase construction projects (`ProjectStage`), milestones, and deadlines.
- **Material Consumption Tracking:** Material allocation per stage from central or on-site project warehouses.
- **Contractor & Subcontractor Expenses:** Contractor expense tracking against agreed contracts and budgets.

### 8. Dynamic Reporting & Export Engine
- **Excel Analytics:** High-performance XLSX reports generated using **Apache POI** for sales, purchases, turnover, cashflow, and profit & loss statements.
- **PDF Document Generation:** Formatted invoices, payment receipts, and employee statements produced via **OpenPDF** with embedded UTF-8 TrueType fonts (`Arial`, `Arial Bold`) supporting localized alphabets.

### 9. Automated Background Schedulers
- **Recurring Expenses:** Automated periodic generation of recurring utility, lease, and service expenses.
- **Subscription Expiry & Freezes:** Automated verification of company subscription lifecycles.
- **Deduction Cleanups:** Monthly settlement of employee deductions and advance payments.

---

## Tech Stack & Libraries

| Technology | Purpose | Version |
| :--- | :--- | :--- |
| **Java** | Core Programming Language | 17 (LTS) |
| **Spring Boot** | Application Framework | 3.5.6 |
| **Spring Security** | Authentication & Authorization | 6.x |
| **Spring Data JPA** | Object Relational Mapping & Data Access | 3.5.6 |
| **PostgreSQL** | Relational Database Engine | 15+ |
| **JJWT (io.jsonwebtoken)** | Stateless JWT Generation & Validation | 0.11.5 |
| **Springdoc OpenAPI** | REST API Documentation & Swagger UI | 2.8.5 |
| **ModelMapper** | DTO <-> Entity Mapping | 3.2.0 |
| **Apache POI** | Excel Workbook & Sheet Generation | 5.2.3 |
| **OpenPDF** | PDF Rendering & Typography Engine | 1.3.41 |
| **ZXing** | Barcode & QR Code Processing | 3.5.3 |
| **Lombok** | Boilerplate Reduction | 1.18.x |

---

## Project Structure

```
specialservice/
├── .mvn/wrapper/                  # Maven Wrapper binaries & properties
├── src/
│   ├── main/
│   │   ├── java/az/techvibeds/specialservice/
│   │   │   ├── config/            # Beans, CORS, ModelMapper, OpenAPI & Seed Runner
│   │   │   ├── constants/         # System constants & security keys
│   │   │   ├── controllers/       # REST API Endpoints (Auth, Inventory, Invoice, etc.)
│   │   │   ├── dtos/              # Domain DTOs organized by business module
│   │   │   ├── enums/             # Business state, role & type enumerations
│   │   │   ├── exceptions/        # Custom exceptions & GlobalExceptionHandler
│   │   │   ├── models/            # JPA Domain Entities & relational mappings
│   │   │   ├── payloads/          # Generic API response and pagination wrappers
│   │   │   ├── repositories/      # Spring Data JPA repositories
│   │   │   ├── schedulers/        # Scheduled cron jobs for automation
│   │   │   ├── security/          # JWT filter, token provider, security config
│   │   │   ├── services/          # Service interfaces & contracts
│   │   │   │   └── impl/          # Service implementations & business logic
│   │   │   ├── utils/             # Barcode generation and helper utilities
│   │   │   └── SpecialServiceApplication.java # Spring Boot entrypoint
│   │   └── resources/
│   │       ├── application.properties     # Core application configuration
│   │       ├── application-dev.properties # Local development profile
│   │       └── fonts/                     # Localized TrueType fonts for PDF exports
│   └── test/
│       └── java/az/techvibeds/specialservice/ # Mockito & Spring Boot Test suites
├── .gitignore                     # Production Git ignore rules
├── mvnw / mvnw.cmd                # Maven Wrapper executables
├── pom.xml                        # Maven project descriptor & dependencies
└── README.md                      # System documentation
```

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher
- **PostgreSQL Database:** Version 14 or higher
- **Maven:** 3.8+ (or use the included `./mvnw` wrapper)

### 1. Database Setup
Create a PostgreSQL database for the application:
```sql
CREATE DATABASE "SpecialService";
```

### 2. Environment Configuration
The application is pre-configured with default local fallback values. You can override settings via environment variables:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `PORT` | Server HTTP port | `8080` |
| `DB_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://localhost:5432/SpecialService` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | HMAC SHA signing secret key | *(Pre-configured development secret)* |
| `SHOW_SQL` | Hibernate SQL output logging | `false` |
| `UPLOAD_DIR` | Storage path for uploads | `uploads` |

### 3. Build & Run
Clone the repository and run the application using Maven Wrapper:

```bash
# Clone the repository
git clone https://github.com/rzajahangirov/specialservice.git
cd specialservice

# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

### 4. Default Seed Data
On initial startup, `DataInitializer` automatically provisions:
- Core System Modules (`INVOICE`, `INVENTORY`, `HR`, `MANUFACTURING`, etc.)
- Default Currencies (`AZN`, `USD`, `EUR`, `TRY`)
- Standard Product & Service Units (`Ədəd`, `Kq`, `Metr`, `Saat`, etc.)
- Super Administrator Account:
  - **Email:** `superadmin@specialservice.com`
  - **Password:** `superadmin123`
- Demonstration Tenant (`Test Company LLC`) with configured warehouse, inventory, and company admin.

---

## API Documentation

Once the application is running, explore and test the interactive OpenAPI 3.0 endpoints via Swagger UI:

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To authenticate in Swagger UI:
1. Call `/api/v1/auth/login` with your credentials.
2. Copy the returned `token`.
3. Click the **Authorize** button in Swagger and paste the token with `Bearer <token>` format.

---

## Security & Best Practices

- **Stateless Authentication:** All REST interactions utilize cryptographically signed JWT tokens with expiry checks.
- **Fine-Grained Authorization:** Interceptors and Method Security (`@PreAuthorize`) validate tenant identity and module access on every call.
- **SQL Injection Prevention:** 100% parameterized query execution through Spring Data JPA / Hibernate criteria.
- **Optimistic Concurrency:** Protects inventory quantities and invoice revisions against concurrent race conditions.

---

## Author

- **Mahammad Rza Jahangirov**
  - GitHub: [@rzajahangirov](https://github.com/rzajahangirov)
  - Email: [jahangirovmahammadrza@gmail.com](mailto:jahangirovmahammadrza@gmail.com)

---

## License

This project is licensed under the [MIT License](LICENSE).
