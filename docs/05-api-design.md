# 05 - API Design

Base URL: `http://localhost:8080/api/v1`

All responses use the envelope `ApiResponse<T>`:

```json
{ "success": true, "message": "...", "data": { } }
```

Errors use `ApiError`:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/v1/..." }
```

Pagination (list endpoints): `?page=0&size=20&sort=createdAt:desc` → `PageResponse<T>` with `content`, `page`, `size`, `totalElements`, `totalPages`, `last`.

Auth: `Authorization: Bearer <accessToken>`. Access token TTL 15 min, refresh token 7d (30d with `rememberMe`).

Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`.

---

## Auth — `/auth`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `/register` | `RegisterRequest` | Create account (CANDIDATE/RECRUITER), sends simulated verification token | public |
| POST | `/login` | `LoginRequest` | Login, returns `AuthResponse` (access + refresh) | public |
| POST | `/refresh` | `RefreshTokenRequest` | Rotate refresh token → new pair | public |
| POST | `/logout` | `{ email }` | Revoke all refresh tokens | auth |
| POST | `/forgot-password` | `ForgotPasswordRequest` | Send reset token (simulated email) | public |
| POST | `/reset-password` | `ResetPasswordRequest` | Set new password with token | public |
| POST | `/verify-email` | `{ token }` | Verify email, activate account | public |
| GET | `/me` | — | Current user profile | auth |

`RegisterRequest`: firstName, lastName, email, password, role. `AuthResponse`: accessToken, refreshToken, tokenType, expiresInMs, rememberMe, user.

---

## Users — `/users`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| GET | `/me` | — | Current user profile | auth |
| PUT | `/me` | `UpdateProfileRequest` | Update profile | auth |
| PUT | `/me/password` | `ChangePasswordRequest` | Change password | auth |
| GET | `/me/dashboard` | — | Candidate dashboard stats | CANDIDATE |
| GET | `` | `?q=&status=&page=&size=` | List users | ADMIN |
| PATCH | `/{id}/status` | `UpdateUserStatusRequest` | Set user status | ADMIN |
| DELETE | `/{id}` | — | Delete user | ADMIN |

---

## Companies — `/companies`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `` | `CreateCompanyRequest` | Create company (slug auto-generated) | RECRUITER/ADMIN |
| GET | `` | `?q=&page=&size=` | List companies | auth |
| GET | `/{id}` | — | Company detail with branding | auth |
| PUT | `/{id}` | `CreateCompanyRequest` | Update company | owner/ADMIN |
| POST | `/{id}/members` | `{ userId, roleInCompany, owner }` | Add member | owner/ADMIN |
| GET | `/{id}/members` | — | List members | auth |

---

## Jobs — `/jobs`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `` | `CreateJobRequest` | Create job (recruiter must be company member) | RECRUITER |
| GET | `` | `?q=&status=&companyId=&skill=&page=` | Search jobs (Specifications) | public |
| GET | `/{uuid}` | — | Job detail + skill list | public |
| PATCH | `/{uuid}/publish` | — | Publish job | RECRUITER |
| PATCH | `/{uuid}/close` | — | Close job | RECRUITER |
| POST | `/{uuid}/view` | — | Increment views | public |

---

## Applications — `/applications`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `` | `ApplyJobRequest` | Apply (unique per job+candidate) | CANDIDATE |
| GET | `/me` | `?page=` | My applications | CANDIDATE |
| GET | `/job/{jobId}` | `?page=` | Applications for a job | RECRUITER |
| PATCH | `/{id}/status` | `UpdateApplicationStatusRequest` | Advance status (sends notification) | RECRUITER |
| POST | `/{id}/withdraw` | — | Withdraw application | CANDIDATE |

---

## Interviews — `/interviews`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `` | `ScheduleInterviewRequest` | Schedule interview | RECRUITER |
| GET | `/me` | — | My interviews | CANDIDATE |
| GET | `/recruiter` | — | Interviews I scheduled | RECRUITER |
| GET | `/{uuid}` | — | Interview detail | involved parties |
| PATCH | `/{uuid}/reschedule` | `ScheduleInterviewRequest` | Reschedule | RECRUITER |
| PATCH | `/{uuid}/status` | `{ status }` | Update status | RECRUITER |
| PATCH | `/{uuid}/score` | `{ score }` | Set final score | RECRUITER |
| POST | `/slots` | `CreateInterviewSlotRequest` | Open availability slot | RECRUITER |
| GET | `/slots` | — | List slots | auth |
| POST | `/sessions` | `StartInterviewSessionRequest` | Start AI session (generates questions) | CANDIDATE |
| GET | `/sessions/{id}` | — | Session detail | participant |
| GET | `/sessions/{id}/questions` | — | Session questions | participant |
| POST | `/sessions/{id}/answers` | `SubmitAnswerRequest` | Submit answer (recorded for grading) | CANDIDATE |
| POST | `/sessions/{id}/feedback` | — (empty body) | Generate AI feedback (Gemini; heuristic fallback) | CANDIDATE |

---

## Coding — `/coding`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `/tests` | `CreateCodingTestRequest` | Create coding test | RECRUITER/ADMIN |
| GET | `/tests` | `?language=&difficulty=&page=` | List tests | auth |
| GET | `/tests/{id}` | — | Test detail | auth |
| GET | `/tests/{id}/cases` | — | Public test cases | auth |
| POST | `/submissions` | `SubmitCodeRequest` | Submit code (static evaluation stub) | auth |
| GET | `/submissions/me` | `?page=` | My submissions | auth |
| GET | `/submissions/{uuid}` | — | Submission detail | owner |

---

## Question Bank — `/questions`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| GET | `` | `?q=&topic=&difficulty=&page=` | Search questions | auth |
| GET | `/{id}` | — | Question detail | auth |
| POST | `` | `CreateQuestionRequest` | Add question | RECRUITER/ADMIN |
| POST | `/{id}/bookmark` | — | Bookmark question | auth |

---

## Bookmarks — `/bookmarks`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `` | `{ jobId }` or `{ questionId }` | Add bookmark | auth |
| DELETE | `` | `{ jobId }` or `{ questionId }` | Remove bookmark | auth |
| GET | `/me` | — | My bookmarks | auth |

---

## Resumes — `/resumes`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `/upload` | `multipart/file` | Upload resume (creates new version) | CANDIDATE |
| GET | `/me` | — | My resumes with versions | CANDIDATE |
| DELETE | `/{id}` | — | Delete resume | owner |
| POST | `/{id}/ats-score` | `{ "jobId": 1 }` | ATS score of the resume against a job (persists a report) | CANDIDATE |
| GET | `/{id}/ats-history` | — | ATS score history for the resume (newest first) | owner |

---

## Notifications — `/notifications`

| Method | Path | Description | Role |
|---|---|---|---|
| GET | `/me` | List my notifications | auth |
| GET | `/me/unread-count` | Unread count | auth |
| PATCH | `/{id}/read` | Mark read | owner |
| PATCH | `/read-all` | Mark all read | auth |

---

## Analytics — `/analytics`

| Method | Path | Description | Role |
|---|---|---|---|
| GET | `/admin` | Platform-wide summary | ADMIN |
| GET | `/recruiter/{companyId}` | Recruiter pipeline analytics | RECRUITER/ADMIN |

---

## Reports — `/reports`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| GET | `` | `?generatedBy=&page=` | List reports | auth |
| POST | `` | `{ type, title, scope, data, format }` | Generate report (PDF/CSV metadata) | auth |
| GET | `/{uuid}/download?format=pdf\|csv` | — | Download file | auth |
| DELETE | `/{uuid}` | — | Delete report | owner |

`ReportType`: RESUME, INTERVIEW, CODING, PERFORMANCE, HIRING.

---

## Files — `/files`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| POST | `/upload` | `multipart/file`, `category` | Store file locally → `StoredFile` | auth |
| GET | `/{uuid}` | — | Download by public UUID | auth |

---

## Admin — `/admin`

| Method | Path | Body | Description | Role |
|---|---|---|---|---|
| GET | `/audit-logs` | `?resource=&page=` | List audit logs | ADMIN |
| GET | `/subscriptions` | — | List subscriptions | ADMIN |
| PATCH | `/subscriptions/{id}` | `UpdateSubscriptionRequest` | Update plan/quota | ADMIN |

---

## System

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Liveness/DB check |
| GET | `/actuator/health` | Actuator health |

---

## Seed Accounts (DataSeeder)

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@interviewai.com | Admin@123 |
| RECRUITER | recruiter@interviewai.com | Recruiter@123 |
| CANDIDATE | candidate@interviewai.com | Candidate@123 |
