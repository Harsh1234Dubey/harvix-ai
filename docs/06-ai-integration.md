# 06 — AI Integration (Gemini)

Phase 4 wires Google Gemini into the platform through a single provider-agnostic
client with graceful fallbacks, so the app is fully usable without credentials.

## Architecture

```
CodingService ──────────────► AiCodeReviewService ─┐
                                                    │
InterviewService ──► AiQuestionService ──► GeminiClient ──► generativelanguage.googleapis.com
              └──► AiFeedbackService ──► GeminiClient ──► generativelanguage.googleapis.com

ResumeService ────► AiResumeService ──► GeminiClient ──► generativelanguage.googleapis.com
```

- `com.interviewai.ai.GeminiClient` — the ONLY class that talks to Google. Calls
  the `generateContent` REST API with `responseMimeType=application/json`.
  Returns `Optional.empty()` when no API key is configured or the call fails.
- `AiQuestionService` — generates mock-interview questions.
  Fallback: question bank → generic templates.
- `AiFeedbackService` — produces structured feedback (scores, strengths,
  weaknesses, suggestions, hiring recommendation, per-question breakdown).
  Fallback: deterministic length/coverage heuristic.
- `AiCodeReviewService` — judges a coding submission against visible test cases
  and returns a code review (status, codeScore, time/space complexity).
  Fallback: deterministic substring check.
- `AiResumeService` — ATS-scans a resume against a job posting and returns a
  0–100 score plus strengths, gaps, and matched/missing keywords.
  Fallback: keyword-match against the job text (coverage %).
  Resume text is extracted with PDFBox (`ResumeTextExtractor` supports PDF/txt).

## Configuration

```yaml
app:
  ai:
    gemini:
      api-key: ${GEMINI_API_KEY:}
      base-url: https://generativelanguage.googleapis.com
      model: gemini-flash-latest
      timeout-seconds: 60
```

Enable live AI by setting the `GEMINI_API_KEY` environment variable (never commit
it). Without it every AI call silently falls back to the deterministic path.
`gemini-flash-latest` is an auto-updating alias; older pinned aliases
(`gemini-1.5-flash`, `gemini-2.5-flash`) may 404 depending on key access.

## Endpoints touched

| Endpoint | Change |
|---|---|
| `POST /interviews/sessions` | Now generates up to 5 questions via AI |
| `POST /interviews/sessions/{id}/feedback` | Empty body; backend calls Gemini and persists the report |
| `POST /coding/submissions` | Evaluation now AI-assisted; returns `codeScore`, `complexityTime`, `complexitySpace` |
| `POST /resumes/{id}/ats-score` | Returns `AtsScoreResponse` (score, summary, strengths, gaps, matched/missing keywords, source) and persists an `AtsReport` |
| `GET /resumes/{id}/ats-history` | Returns persisted `AtsReportResponse` list (per resume version vs job) |
| `POST /applications?jobUuid=` | When `resumeId` is supplied, `atsScore` is computed and persisted on the application |
| `GET /users/me/dashboard` | `bestResumeScore` now populated from the best stored ATS report |

The feedback endpoint no longer trusts a client-provided payload. All AI output is
stored on the existing `interview_feedback` JSONB columns and surfaced through
`InterviewFeedbackResponse`. The response `source` field reports `AI` when Gemini
answered and `KEYWORD_FALLBACK` when the deterministic path was used.

## Testing

- Unit tests cover the AI and fallback paths: `AiQuestionServiceTest`,
  `AiFeedbackServiceTest`, `AiCodeReviewServiceTest`.
- No API key is required for CI: mocks return `Optional.empty()`, exercising the
  deterministic fallbacks.
