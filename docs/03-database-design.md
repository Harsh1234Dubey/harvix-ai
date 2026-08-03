# 03 — Database Design

> InterView AI — Phase 1 deliverable. Design rationale, module map, and conventions.

## 1. Goals

- **Normalized** to 3NF for transactional data (auth, jobs, applications).
- **JSONB only where justified**: parsed resume output, AI feedback breakdowns, transcripts, audit snapshots — documents that are read/written atomically.
- **Evolving statuses as Postgres ENUMs** for data integrity.
- **UUID public keys + BIGSERIAL internal PKs** (safe IDs on API, fast indexes locally).
- **Audit & AI usage** fully logged for admin dashboards.

## 2. Module Map (39 tables)

| Module            | Tables                                                                 |
|-------------------|------------------------------------------------------------------------|
| Security / RBAC   | roles, permissions, role_permissions, user_roles, audit_logs           |
| Auth              | users, refresh_tokens, password_reset_tokens, email_verifications      |
| Company           | companies, company_members, subscriptions                              |
| Jobs              | jobs, job_skills                                                       |
| Skills            | skills, candidate_skills                                               |
| Resume            | resumes, resume_versions                                               |
| Application       | applications                                                           |
| Interview         | interviews, interview_slots, interview_sessions, interview_questions, interview_answers, interview_feedback |
| Coding            | coding_tests, test_cases, coding_submissions, code_reviews             |
| Question Bank     | questions                                                              |
| Gamification      | achievements, user_achievements, xp_transactions, leaderboard_entries, coding_streaks |
| Certificates      | certificates                                                           |
| Notifications     | notifications, email_audits                                            |
| Reports           | reports                                                                |
| AI                | ai_usage_logs                                                          |
| Storage           | stored_files                                                           |
| Cross-cutting     | bookmarks                                                              |

## 3. Key Design Decisions

### 3.1 RBAC model
`users` → M:N `roles` → M:N `permissions`. This is extensible: admin can grant a
recruiter read-only company access, or a candidate the `report:download` permission,
without code changes.

### 3.2 Applications uniqueness
`UNIQUE(job_id, candidate_id)` — a candidate can apply to a job **once**. Resubmission
updates the existing row (new resume version), preserving history and analytics.

### 3.3 Interview pipeline
`interviews` (scheduled meeting) → `interview_sessions` (an actual run) →
`interview_questions` → `interview_answers` → `interview_feedback`. Follow-up
questions self-reference via `follow_up_of`.

### 3.4 Coding flow
`coding_tests` (problem) → `test_cases` (public + hidden) → `coding_submissions`
(execution results) → `code_reviews` (AI review, 1:1).

### 3.5 Resume versioning
`resumes` is the logical document; `resume_versions` stores immutable snapshots with
parsed JSONB. Uploading a new PDF bumps `current_version` — full audit trail for free.

### 3.6 Analytics are derived, not stored
Platform analytics (funnels, pass rates, top skills, average scores) are computed with
aggregation queries over the above tables. No denormalized counters except
`jobs.applications_count` / `views_count` (updated transactionally) to keep the job
list dashboard fast.

### 3.7 Storage
Files (resume PDFs, video, audio, certificates) live under `./uploads` on disk;
`stored_files` + `resume_versions.file_path` store metadata. Controllers stream files
with auth + path-safety checks (no user-supplied paths).

### 3.8 Security by schema
- Passwords: BCrypt hashes only.
- Tokens: hashed refresh tokens, expiry, revocation.
- `email_audits` simulates the mailer (visible in admin audit view).
- `audit_logs` captures before/after JSONB snapshots for admin investigation.

## 4. Conventions

| Convention       | Rule                                                        |
|------------------|-------------------------------------------------------------|
| Naming           | snake_case, plural tables, singular PK (`id`)               |
| Timestamps       | `created_at`, `updated_at` (TIMESTAMPTZ, trigger-maintained)|
| Soft fields      | `is_*` / `has_*` booleans, `status` enums                   |
| Money            | NUMERIC(12,2), currency column                              |
| Percentages      | NUMERIC(5,2) → 0.00–100.00                                 |
| FKs              | `ON DELETE` policy explicit per relationship (matrix in ER doc) |
| Indexes          | Every FK + every hot filter column                          |

## 5. Sample Queries (used by analytics)

```sql
-- Hiring funnel
SELECT status, count(*) FROM applications GROUP BY status;

-- Top in-demand skills across published jobs
SELECT s.name, count(js.job_id) AS demand
FROM job_skills js JOIN skills s ON s.id = js.skill_id
JOIN jobs j ON j.id = js.job_id AND j.status = 'PUBLISHED'
GROUP BY s.name ORDER BY demand DESC LIMIT 10;

-- Candidate leaderboard
SELECT u.email, le.xp_total, le.rank_no
FROM leaderboard_entries le JOIN users u ON u.id = le.user_id
WHERE le.period = 'ALL_TIME' ORDER BY le.xp_total DESC LIMIT 50;

-- Average interview score per skill
SELECT i.skill, round(avg(f.overall_score), 2) AS avg_score
FROM interview_sessions i
JOIN interview_feedback f ON f.session_id = i.id
GROUP BY i.skill ORDER BY avg_score DESC;
```

## 6. Migration Strategy

- `database/schema.sql` — canonical schema (idempotent enum guards).
- `database/seed.sql` — roles, permissions, admin user, demo skills, sample question bank, sample companies/jobs.
- In Phase 2, the Spring Boot app runs with `ddl-auto: validate` and a Flyway-style
  versioned folder `database/migrations/V1__init.sql` is introduced if desired.
