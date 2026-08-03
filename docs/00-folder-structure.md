# 00 — Folder Structure

> InterView AI — Phase 1 deliverable. Canonical folder structure (skeletons created on disk).

## Root

```
interview-ai/
├── backend/                    # Spring Boot 3 (Java 21)
├── frontend/                   # React 19 + Vite
├── database/                   # PostgreSQL
│   ├── schema.sql              # 39-table canonical schema (Phase 1)
│   └── seed.sql                # roles, admin, skills, question bank (Phase 2)
├── docs/                       # all documentation
│   ├── 00-folder-structure.md
│   ├── 01-architecture.md
│   ├── 02-er-diagram.md
│   ├── 03-database-design.md
│   ├── 04-ui-wireframes.md
│   ├── 05-api-design.md        # Phase 2
│   ├── 06-installation-guide.md# Phase 6
│   └── postman/                # Phase 5
└── README.md
```

## Backend (src/main/java/com/interviewai)

```
config/        SecurityConfig, CorsConfig, OpenApiConfig, JacksonConfig,
               AIConfig, AsyncConfig
common/        enums/ constants/ util/ response/ (ApiResponse, PageResponse, ApiError)
security/      JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
               AuthEntryPoint, RateLimitFilter
audit/         AuditAspect, AuditTrailService
ai/            client/ (GeminiClient), prompts/, dto/ (ResumeAnalysis, Feedback, CodeReview…)
storage/       FileStorageService
notification/  NotificationService, EmailSimulator, InterviewReminderScheduler
report/        PdfGenerator, CsvGenerator, ReportService
domain/        (39 JPA entities mirroring schema.sql)
repository/    (Spring Data JPA repositories)
dto/           request/ response/
mapper/        ModelMapperConfig + mappers
service/       (interfaces)
service/impl/  (implementations)
controller/    (REST controllers, /api/v1)
exception/     GlobalExceptionHandler + custom exceptions
resources/     application.yml, application-dev.yml, db/migration (optional)
src/test/      unit + integration tests
uploads/       local file storage root (gitignored)
```

## Frontend (src)

```
main.tsx / App.tsx
providers/     QueryProvider, ThemeProvider, AuthProvider, ToastProvider
routes/        PublicRoute, ProtectedRoute, RoleRoute
layouts/       LandingLayout, AuthLayout, AppLayout (role-aware sidebar)
pages/         landing/ auth/ candidate/ recruiter/ admin/
components/    ui/ (shadcn), common/, charts/, candidate/, recruiter/, admin/, ai/
features/      auth/ jobs/ resume/ interviews/ coding/ analytics/ gamification/
hooks/         useAuth, useTheme, useNotifications, useDebounce…
lib/           axios client, token storage, constants, formatters
store/         theme store, auth store, notification store
types/         TS interfaces mirroring backend DTOs
styles/        globals.css, tailwind tokens
assets/        logos, illustrations
```

## Database

```
database/
├── schema.sql        # canonical DDL (Phase 1 complete)
├── seed.sql          # seed data (Phase 2)
└── migrations/       # optional Flyway versioned scripts
```

## Docs Index

| Doc | Status |
|-----|--------|
| 00-folder-structure.md | ✅ Phase 1 |
| 01-architecture.md     | ✅ Phase 1 |
| 02-er-diagram.md       | ✅ Phase 1 |
| 03-database-design.md  | ✅ Phase 1 |
| 04-ui-wireframes.md    | ✅ Phase 1 |
| 05-api-design.md       | ⏳ Phase 2 |
| 06-installation-guide.md | ⏳ Phase 6 |
