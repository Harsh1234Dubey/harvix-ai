# 01 — System Architecture

> InterView AI — Phase 1 deliverable: architecture, component diagram, folder structure.

## 1. High-Level Architecture

```
                        ┌──────────────────────────────────────────────┐
                        │                  BROWSER                     │
                        │   React 19 SPA @ localhost:5173 (Vite)       │
                        │   Tailwind · Shadcn UI · Framer Motion       │
                        └───────────────┬──────────────────────────────┘
                                        │ HTTPS (dev: HTTP) / JSON
                                        ▼
                        ┌──────────────────────────────────────────────┐
                        │            FRONTEND CLIENT LAYER             │
                        │  Router · TanStack Query · RHF+Zod · Axios   │
                        │  Auth Context · Toast · Monaco · Chart.js    │
                        └───────────────┬──────────────────────────────┘
                                        │
                                        ▼
                        ┌──────────────────────────────────────────────┐
                        │     API GATEWAY (Spring Security filter)     │
                        │   CORS · Rate Limit · JWT Auth · RBAC        │
                        └───────────────┬──────────────────────────────┘
                                        ▼
                        ┌──────────────────────────────────────────────┐
                        │            SPRING BOOT @ :8080               │
                        │  REST Controllers (v1) → Service Layer → DAO │
                        │  AOP Audit · Global Exceptions · Cache       │
                        └──┬──────────┬───────────┬──────────┬─────────┘
                           │          │           │          │
                           ▼          ▼           ▼          ▼
                    PostgreSQL    Local File   Gemini API    MailSender
                   (:5432)        Storage      (HTTPS)       (simulated)
                   Spring Data    /uploads/    Resume Analysis
                   JPA/Hibernate              · Interview Gen
                                              · Feedback
                                              · Code Review
                                              · Recruiter NL Queries
```

### Layers
1. **Presentation Layer** — React SPA consuming a versioned REST API (`/api/v1`).
2. **API Layer** — Spring controllers, DTO in/out, Bean Validation, Swagger/OpenAPI.
3. **Service Layer** — business logic, transactions, `@Transactional`, AOP audit.
4. **Data Layer** — Spring Data JPA repositories, entities, native queries.
5. **External Integrations** — Gemini AI adapter, local storage service, simulated mail.

## 2. Backend Clean Architecture

Strict layering with dependency inversion: controllers depend on service interfaces,
services depend on repository interfaces, no entity leaks into DTOs.

```
backend/
└── src/main/java/com/interviewai/
    ├── InterviewAiApplication.java
    ├── config/            # Security, CORS, OpenAPI, Jackson, AI, Async
    ├── common/            # enums, constants, utils, pagination, ApiResponse
    ├── security/          # JWT filter, UserDetails, entry points, rate limit
    ├── audit/             # AOP auditing + audit trail service
    ├── ai/                # Gemini client + prompt engineering + DTOs
    ├── storage/           # local file storage service
    ├── notification/      # notification + simulated email + reminders
    ├── report/            # PDF/CSV generation
    ├── domain/            # JPA entities
    ├── repository/        # Spring Data repositories
    ├── dto/               # request/response DTOs
    ├── mapper/            # ModelMapper config + mappers
    ├── service/           # service interfaces
    ├── service/impl/      # service implementations
    ├── controller/        # REST controllers
    ├── exception/         # global exception handler + custom exceptions
    └── config/seed/       # data seeder (roles, admin, skills, question bank)
```

## 3. Frontend Architecture

```
frontend/
├── src/
│   ├── main.tsx                 # entry
│   ├── App.tsx                  # router + providers
│   ├── providers/               # Query, Theme, Auth, Toast
│   ├── routes/                  # protected/public route guards
│   ├── layouts/                 # Landing, Auth, AppShell (role-based sidebar)
│   ├── pages/
│   │   ├── landing/
│   │   ├── auth/
│   │   ├── candidate/
│   │   ├── recruiter/
│   │   └── admin/
│   ├── components/
│   │   ├── ui/                  # shadcn primitives
│   │   ├── common/              # reusable (DataTable, Skeleton, EmptyState…)
│   │   ├── charts/              # Chart.js wrappers
│   │   ├── candidate/ recruiter/ admin/
│   │   └── ai/                  # AI chat, editor, interview player
│   ├── features/                # feature slices (auth, jobs, interviews…)
│   ├── hooks/
│   ├── lib/                     # api client, utils, constants
│   ├── store/                   # zustand-style stores (theme, auth, notifications)
│   ├── types/                   # TS interfaces mirroring DTOs
│   ├── styles/
│   └── assets/
```

## 4. Component Diagram (Services)

```
┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐
│ AuthService  │  │ ResumeService │  │ JobService   │  │InterviewSvc  │
└──────────────┘  └───────────────┘  └──────────────┘  └──────────────┘
┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐
│ CodingSvc    │  │ QuestionBankSvc│ │ FeedbackSvc  │  │ ReportSvc    │
└──────────────┘  └───────────────┘  └──────────────┘  └──────────────┘
┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐
│ AIService    │  │ AnalyticsSvc  │  │ Gamification │  │ RecruiterAI  │
└──────────────┘  └───────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │                 │
        └─────────────────┴─────────────────┴─────────────────┘
                         ┌──────────────────────┐
                         │  Security / Audit /   │
                         │  Storage / Notification│
                         └──────────────────────┘
```

## 5. Cross-Cutting Concerns

| Concern          | Approach                                                        |
|------------------|-----------------------------------------------------------------|
| Security         | JWT access + refresh tokens, BCrypt, RBAC via `@PreAuthorize`, CORS, rate limiting filter |
| Validation       | Bean Validation (`jakarta.validation`) on request DTOs          |
| Errors           | Global `@RestControllerAdvice` → consistent `ApiError` payload  |
| Audit            | AOP aspect + `audit_logs` table (actor, action, entity, before/after, IP) |
| Pagination       | Spring `Pageable`, consistent `PageResponse<T>` DTO              |
| Idempotency      | Unique constraints + optimistic locking (`@Version`) where needed |
| AI Calls         | Async + retry + token budgeting + `ai_usage` logging             |
| File Storage     | `./uploads` local dir; path stored in DB, served by controller   |
| Rate Limiting    | Per-user/per-IP token-bucket filter                              |

## 6. API Conventions

- Base path: `/api/v1`
- Response envelope: `{ status, message, data, timestamp, path }`
- Errors: `{ status, error, message, fieldErrors[], timestamp, path }`
- Standard HTTP codes (200/201/204/400/401/403/404/409/422/429/500)
- OpenAPI at `/v3/api-docs`, UI at `/swagger-ui.html`

## 7. Environments & Configuration

```
application.yml        # shared config
application-dev.yml    # local dev (default profile: dev)
```

Key config: datasource, JWT secret/expiry, refresh expiry, Gemini API key (env var `GEMINI_API_KEY`), storage root, upload size limits, CORS origins (`http://localhost:5173`), rate limits.
