-- ============================================================================
-- InterView AI — PostgreSQL Schema (Phase 1)
-- ~39 normalized tables: auth, security, companies, jobs, candidates, resumes,
-- applications, interviews, coding, question bank, gamification, reports,
-- notifications, audit, ai usage, subscriptions, storage.
-- Target: PostgreSQL 15+  |  Database: interview_ai
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- gen_random_uuid()

-- ----------------------------------------------------------------------------
-- ENUMERATED TYPES
-- ----------------------------------------------------------------------------
DO $$ BEGIN
  CREATE TYPE user_role      AS ENUM ('ADMIN','RECRUITER','CANDIDATE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE user_status    AS ENUM ('ACTIVE','INACTIVE','BLOCKED','PENDING_VERIFICATION');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE job_status     AS ENUM ('DRAFT','PUBLISHED','CLOSED','FILLED','EXPIRED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE employment_type AS ENUM ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','FREELANCE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE work_mode      AS ENUM ('ONSITE','REMOTE','HYBRID');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE application_status AS ENUM ('SUBMITTED','REVIEWING','SHORTLISTED','INTERVIEW','OFFERED','REJECTED','WITHDRAWN','HIRED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE difficulty     AS ENUM ('EASY','MEDIUM','HARD');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE interview_type AS ENUM ('AI_MOCK','CODING','HR','TECHNICAL','VIDEO','SYSTEM_DESIGN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE interview_status AS ENUM ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW','RESCHEDULED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE question_type  AS ENUM ('MCQ','CODING','TEXT','SYSTEM_DESIGN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE submission_status AS ENUM ('ACCEPTED','WRONG_ANSWER','TIME_LIMIT','RUNTIME_ERROR','COMPILE_ERROR','MEMORY_LIMIT','PENDING');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE notification_type AS ENUM ('INTERVIEW_REMINDER','APPLICATION_STATUS','JOB_POSTED','RECRUITER_MESSAGE','REPORT_READY','ACHIEVEMENT','SYSTEM');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE audit_action   AS ENUM ('CREATE','UPDATE','DELETE','READ','LOGIN','LOGOUT','PASSWORD_RESET','EXPORT','AI_CALL');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE report_type    AS ENUM ('RESUME','INTERVIEW','CODING','PERFORMANCE','HIRING');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE subscription_plan AS ENUM ('FREE','PRO','ENTERPRISE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE subscription_status AS ENUM ('ACTIVE','TRIAL','EXPIRED','CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ----------------------------------------------------------------------------
-- 1. ROLES & PERMISSIONS (RBAC)
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(50)  NOT NULL UNIQUE,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(100) NOT NULL UNIQUE,   -- e.g. job:create, user:read
  name        VARCHAR(100) NOT NULL,
  resource    VARCHAR(50)  NOT NULL,
  action      VARCHAR(50)  NOT NULL
);

CREATE TABLE role_permissions (
  role_id       BIGINT NOT NULL REFERENCES roles(id)        ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES permissions(id)  ON DELETE CASCADE,
  PRIMARY KEY (role_id, permission_id)
);

-- ----------------------------------------------------------------------------
-- 2. USERS & AUTH
-- ----------------------------------------------------------------------------
CREATE TABLE users (
  id                 BIGSERIAL PRIMARY KEY,
  uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  first_name         VARCHAR(100) NOT NULL,
  last_name          VARCHAR(100) NOT NULL,
  email              VARCHAR(255) NOT NULL UNIQUE,
  password_hash      VARCHAR(255) NOT NULL,
  avatar_url         VARCHAR(500),
  phone              VARCHAR(30),
  status             user_status  NOT NULL DEFAULT 'PENDING_VERIFICATION',
  email_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
  last_login_at      TIMESTAMPTZ,
  last_login_ip      VARCHAR(45),
  failed_attempts    INT          NOT NULL DEFAULT 0,
  locked_until       TIMESTAMPTZ,
  remember_me_token  VARCHAR(255),
  session_timeout_min INT         NOT NULL DEFAULT 30,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
  user_id    BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  role_id    BIGINT NOT NULL REFERENCES roles(id)  ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token       VARCHAR(512) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ  NOT NULL,
  revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
  user_agent  VARCHAR(255),
  ip_address VARCHAR(45),
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE password_reset_tokens (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token      VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ  NOT NULL,
  used       BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE email_verifications (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token      VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ  NOT NULL,
  verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 3. COMPANIES & MEMBERSHIP
-- ----------------------------------------------------------------------------
CREATE TABLE companies (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(255) NOT NULL,
  slug            VARCHAR(255) NOT NULL UNIQUE,
  description     TEXT,
  logo_url        VARCHAR(500),
  website         VARCHAR(255),
  industry        VARCHAR(100),
  location        VARCHAR(255),
  size_range      VARCHAR(50),
  founded_year    INT,
  branding_color  VARCHAR(20),
  is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
  created_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE company_members (
  id           BIGSERIAL PRIMARY KEY,
  company_id   BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  user_id      BIGINT NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  role_in_company VARCHAR(100),
  is_owner     BOOLEAN NOT NULL DEFAULT FALSE,
  joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (company_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 4. SKILLS (master) & CANDIDATE SKILLS
-- ----------------------------------------------------------------------------
CREATE TABLE skills (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL UNIQUE,
  category   VARCHAR(100),            -- JAVA, FRONTEND, DB, CLOUD, SOFT...
  aliases    TEXT
);

CREATE TABLE candidate_skills (
  id            BIGSERIAL PRIMARY KEY,
  candidate_id  BIGINT NOT NULL,     -- FK to users (candidate) resolved below
  skill_id      BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
  proficiency   INT    CHECK (proficiency BETWEEN 1 AND 100),
  years         NUMERIC(4,1) DEFAULT 0,
  last_used     INT,
  endorsed      BOOLEAN DEFAULT FALSE,
  UNIQUE (candidate_id, skill_id)
);
ALTER TABLE candidate_skills ADD CONSTRAINT fk_cand_skills_user
  FOREIGN KEY (candidate_id) REFERENCES users(id) ON DELETE CASCADE;

-- ----------------------------------------------------------------------------
-- 5. JOBS
-- ----------------------------------------------------------------------------
CREATE TABLE jobs (
  id               BIGSERIAL PRIMARY KEY,
  uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  company_id       BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  posted_by        BIGINT NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  title            VARCHAR(255) NOT NULL,
  slug             VARCHAR(300) NOT NULL,
  description      TEXT NOT NULL,
  requirements     TEXT,
  responsibilities TEXT,
  location         VARCHAR(255),
  work_mode        work_mode DEFAULT 'ONSITE',
  employment_type  employment_type DEFAULT 'FULL_TIME',
  experience_min   INT,
  experience_max   INT,
  salary_min       NUMERIC(12,2),
  salary_max       NUMERIC(12,2),
  currency         VARCHAR(10) DEFAULT 'USD',
  vacancy_count    INT NOT NULL DEFAULT 1,
  status           job_status NOT NULL DEFAULT 'DRAFT',
  views_count      INT NOT NULL DEFAULT 0,
  applications_count INT NOT NULL DEFAULT 0,
  expires_at       TIMESTAMPTZ,
  published_at     TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_jobs_company_slug ON jobs(company_id, slug);

CREATE TABLE job_skills (
  id       BIGSERIAL PRIMARY KEY,
  job_id   BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
  required BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (job_id, skill_id)
);

CREATE TABLE bookmarks (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  entity_type VARCHAR(30) NOT NULL,   -- 'JOB' | 'QUESTION' | 'CANDIDATE'
  entity_id   BIGINT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, entity_type, entity_id)
);

-- ----------------------------------------------------------------------------
-- 6. RESUMES & VERSION HISTORY
-- ----------------------------------------------------------------------------
CREATE TABLE resumes (
  id            BIGSERIAL PRIMARY KEY,
  candidate_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title         VARCHAR(150) NOT NULL DEFAULT 'My Resume',
  current_version INT NOT NULL DEFAULT 1,
  is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resume_versions (
  id            BIGSERIAL PRIMARY KEY,
  resume_id     BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
  version_no    INT NOT NULL,
  file_path     VARCHAR(500) NOT NULL,
  file_size     BIGINT,
  file_type     VARCHAR(20) DEFAULT 'application/pdf',
  extracted_data JSONB,              -- parsed skills/projects/experience/education
  parsed_skills JSONB,               -- skill -> confidence
  uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (resume_id, version_no)
);

-- ATS score history: one row per scored resume-version vs job
CREATE TABLE ats_reports (
  id              BIGSERIAL PRIMARY KEY,
  resume_id       BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
  candidate_id    BIGINT NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
  version_no      INT NOT NULL,
  job_id          BIGINT NOT NULL REFERENCES jobs(id)    ON DELETE CASCADE,
  job_title       VARCHAR(200),
  score           NUMERIC(5,2) NOT NULL,
  summary         TEXT,
  strengths       JSONB DEFAULT '[]',
  gaps            JSONB DEFAULT '[]',
  matched_keywords JSONB DEFAULT '[]',
  missing_keywords JSONB DEFAULT '[]',
  source          VARCHAR(20),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 7. APPLICATIONS
-- ----------------------------------------------------------------------------
CREATE TABLE applications (
  id            BIGSERIAL PRIMARY KEY,
  uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  job_id        BIGINT NOT NULL REFERENCES jobs(id)       ON DELETE CASCADE,
  candidate_id  BIGINT NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  resume_id     BIGINT REFERENCES resumes(id)             ON DELETE SET NULL,
  status        application_status NOT NULL DEFAULT 'SUBMITTED',
  cover_letter  TEXT,
  ats_score     NUMERIC(5,2),
  match_percentage NUMERIC(5,2),
  recruiter_notes TEXT,
  applied_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (job_id, candidate_id)
);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_job ON applications(job_id);

-- ----------------------------------------------------------------------------
-- 8. INTERVIEWS & SCHEDULING
-- ----------------------------------------------------------------------------
CREATE TABLE interviews (
  id              BIGSERIAL PRIMARY KEY,
  uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  application_id  BIGINT REFERENCES applications(id) ON DELETE SET NULL,
  candidate_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recruiter_id    BIGINT REFERENCES users(id) ON DELETE SET NULL,
  title           VARCHAR(255) NOT NULL,
  type            interview_type NOT NULL DEFAULT 'TECHNICAL',
  status          interview_status NOT NULL DEFAULT 'SCHEDULED',
  scheduled_at    TIMESTAMPTZ,
  duration_min    INT NOT NULL DEFAULT 60,
  location        VARCHAR(255),
  meeting_link    VARCHAR(500),
  difficulty      difficulty,
  score           NUMERIC(5,2),
  hiring_recommendation VARCHAR(30),   -- STRONG_HIRE/HIRE/MAYBE/NO
  feedback_summary TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_interviews_candidate ON interviews(candidate_id);
CREATE INDEX idx_interviews_schedule ON interviews(scheduled_at);

CREATE TABLE interview_slots (
  id           BIGSERIAL PRIMARY KEY,
  recruiter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  starts_at    TIMESTAMPTZ NOT NULL,
  ends_at      TIMESTAMPTZ NOT NULL,
  is_booked    BOOLEAN NOT NULL DEFAULT FALSE,
  interview_id BIGINT REFERENCES interviews(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_slots_recruiter_time ON interview_slots(recruiter_id, starts_at);

CREATE TABLE interview_sessions (
  id               BIGSERIAL PRIMARY KEY,
  uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  interview_id     BIGINT REFERENCES interviews(id) ON DELETE CASCADE,
  candidate_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  skill            VARCHAR(100),
  difficulty       difficulty NOT NULL DEFAULT 'MEDIUM',
  total_questions  INT DEFAULT 0,
  answered_questions INT DEFAULT 0,
  transcript_json  JSONB,
  video_path       VARCHAR(500),
  duration_seconds INT DEFAULT 0,
  started_at       TIMESTAMPTZ,
  completed_at     TIMESTAMPTZ,
  status           interview_status NOT NULL DEFAULT 'IN_PROGRESS'
);
CREATE INDEX idx_sessions_candidate ON interview_sessions(candidate_id);

CREATE TABLE interview_questions (
  id               BIGSERIAL PRIMARY KEY,
  session_id       BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
  question_text    TEXT NOT NULL,
  topic            VARCHAR(100),
  difficulty       difficulty,
  category         VARCHAR(50),       -- TECHNICAL/BEHAVIORAL/CODING/HR
  is_follow_up     BOOLEAN NOT NULL DEFAULT FALSE,
  follow_up_of     BIGINT REFERENCES interview_questions(id) ON DELETE SET NULL,
  order_index      INT NOT NULL DEFAULT 0,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE interview_answers (
  id              BIGSERIAL PRIMARY KEY,
  question_id     BIGINT NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE,
  answer_text     TEXT,
  audio_path      VARCHAR(500),
  voice_to_text   TEXT,
  confidence_score NUMERIC(5,2),
  is_skipped      BOOLEAN NOT NULL DEFAULT FALSE,
  answered_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE interview_feedback (
  id                  BIGSERIAL PRIMARY KEY,
  session_id          BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
  overall_score       NUMERIC(5,2),
  communication       NUMERIC(5,2),
  confidence          NUMERIC(5,2),
  technical_knowledge NUMERIC(5,2),
  grammar             NUMERIC(5,2),
  fluency             NUMERIC(5,2),
  keyword_match       NUMERIC(5,2),
  speaking_speed      NUMERIC(5,2),
  strengths_json      JSONB,
  weaknesses_json     JSONB,
  learning_suggestions TEXT,
  hiring_recommendation VARCHAR(30),
  detailed_json       JSONB,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 9. CODING PLATFORM
-- ----------------------------------------------------------------------------
CREATE TABLE coding_tests (
  id              BIGSERIAL PRIMARY KEY,
  title           VARCHAR(255) NOT NULL,
  description     TEXT,
  language        VARCHAR(30) NOT NULL,          -- JAVA/PYTHON/JAVASCRIPT/CPP/SQL
  difficulty      difficulty NOT NULL DEFAULT 'MEDIUM',
  time_limit_sec  INT NOT NULL DEFAULT 10,
  memory_limit_mb INT NOT NULL DEFAULT 256,
  starter_code    TEXT,
  solution_code   TEXT,
  is_public       BOOLEAN NOT NULL DEFAULT TRUE,
  created_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE test_cases (
  id               BIGSERIAL PRIMARY KEY,
  coding_test_id   BIGINT NOT NULL REFERENCES coding_tests(id) ON DELETE CASCADE,
  input_data       TEXT,
  expected_output  TEXT NOT NULL,
  is_hidden        BOOLEAN NOT NULL DEFAULT FALSE,
  order_index      INT NOT NULL DEFAULT 0
);

CREATE TABLE coding_submissions (
  id               BIGSERIAL PRIMARY KEY,
  uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  candidate_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  coding_test_id   BIGINT NOT NULL REFERENCES coding_tests(id) ON DELETE CASCADE,
  session_id       BIGINT REFERENCES interview_sessions(id) ON DELETE SET NULL,
  language         VARCHAR(30) NOT NULL,
  source_code      TEXT NOT NULL,
  status           submission_status NOT NULL DEFAULT 'PENDING',
  passed_cases     INT DEFAULT 0,
  total_cases      INT DEFAULT 0,
  execution_time_ms BIGINT,
  memory_used_kb   BIGINT,
  stdout           TEXT,
  stderr           TEXT,
  error_message    TEXT,
  code_score       NUMERIC(5,2),
  complexity_time  VARCHAR(50),
  complexity_space VARCHAR(50),
  submitted_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_submissions_candidate ON coding_submissions(candidate_id);
CREATE INDEX idx_submissions_test ON coding_submissions(coding_test_id);

CREATE TABLE code_reviews (
  id              BIGSERIAL PRIMARY KEY,
  submission_id   BIGINT NOT NULL UNIQUE REFERENCES coding_submissions(id) ON DELETE CASCADE,
  time_complexity VARCHAR(50),
  space_complexity VARCHAR(50),
  code_quality_score NUMERIC(5,2),
  naming_suggestions JSONB,
  optimization_suggestions JSONB,
  bugs_found       JSONB,
  best_practices   JSONB,
  overall_review   TEXT,
  reviewed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 10. QUESTION BANK
-- ----------------------------------------------------------------------------
CREATE TABLE questions (
  id          BIGSERIAL PRIMARY KEY,
  topic       VARCHAR(100) NOT NULL,      -- JAVA/SPRING/HIBERNATE/SQL/MONGODB/REACT/JS/DSA/OS/DBMS/CN/HR/SYSTEM_DESIGN
  sub_topic   VARCHAR(150),
  question    TEXT NOT NULL,
  answer      TEXT,
  difficulty  difficulty NOT NULL DEFAULT 'MEDIUM',
  type        question_type NOT NULL DEFAULT 'TEXT',
  source      VARCHAR(30) DEFAULT 'AI',
  tags        JSONB,
  views_count INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_questions_topic ON questions(topic);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);

-- ----------------------------------------------------------------------------
-- 11. GAMIFICATION
-- ----------------------------------------------------------------------------
CREATE TABLE achievements (
  id           BIGSERIAL PRIMARY KEY,
  code         VARCHAR(100) NOT NULL UNIQUE,
  name         VARCHAR(150) NOT NULL,
  description  TEXT,
  icon_url     VARCHAR(500),
  xp_reward    INT NOT NULL DEFAULT 0,
  criteria_json JSONB
);

CREATE TABLE user_achievements (
  id              BIGSERIAL PRIMARY KEY,
  user_id         BIGINT NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
  achievement_id  BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
  unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, achievement_id)
);

CREATE TABLE xp_transactions (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  xp_change   INT NOT NULL,
  reason      VARCHAR(150) NOT NULL,
  reference_id BIGINT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_xp_user ON xp_transactions(user_id);

CREATE TABLE leaderboard_entries (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  period      VARCHAR(20) NOT NULL DEFAULT 'ALL_TIME',   -- WEEKLY/MONTHLY/ALL_TIME
  xp_total    INT NOT NULL DEFAULT 0,
  rank_no     INT,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, period)
);

CREATE TABLE coding_streaks (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  current_streak INT NOT NULL DEFAULT 0,
  longest_streak INT NOT NULL DEFAULT 0,
  last_active_date DATE,
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id)
);

-- ----------------------------------------------------------------------------
-- 12. CERTIFICATES
-- ----------------------------------------------------------------------------
CREATE TABLE certificates (
  id           BIGSERIAL PRIMARY KEY,
  uuid         UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  candidate_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  issued_for   VARCHAR(50),            -- interview/course/coding/achievement
  reference_id BIGINT,
  grade        VARCHAR(30),
  score        NUMERIC(5,2),
  file_path    VARCHAR(500),
  issued_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_certs_candidate ON certificates(candidate_id);

-- ----------------------------------------------------------------------------
-- 13. NOTIFICATIONS & SIMULATED EMAIL
-- ----------------------------------------------------------------------------
CREATE TABLE notifications (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type        notification_type NOT NULL DEFAULT 'SYSTEM',
  title       VARCHAR(255) NOT NULL,
  message     TEXT,
  data_json   JSONB,
  is_read     BOOLEAN NOT NULL DEFAULT FALSE,
  read_at     TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE email_audits (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
  to_email    VARCHAR(255) NOT NULL,
  subject     VARCHAR(255) NOT NULL,
  body        TEXT,
  sent_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 14. REPORTS
-- ----------------------------------------------------------------------------
CREATE TABLE reports (
  id            BIGSERIAL PRIMARY KEY,
  uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  report_type   report_type NOT NULL,
  title         VARCHAR(255) NOT NULL,
  generated_by  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  subject_user  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  scope_json    JSONB,
  data_json     JSONB,
  file_path     VARCHAR(500),
  format        VARCHAR(10) DEFAULT 'PDF',    -- PDF/CSV
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_generated_by ON reports(generated_by);

-- ----------------------------------------------------------------------------
-- 15. SUBSCRIPTIONS (Admin / billing simulation)
-- ----------------------------------------------------------------------------
CREATE TABLE subscriptions (
  id             BIGSERIAL PRIMARY KEY,
  company_id     BIGINT REFERENCES companies(id) ON DELETE CASCADE,
  user_id        BIGINT REFERENCES users(id) ON DELETE CASCADE,
  plan           subscription_plan NOT NULL DEFAULT 'FREE',
  status         subscription_status NOT NULL DEFAULT 'TRIAL',
  ai_quota_month INT NOT NULL DEFAULT 50,
  ai_used_month  INT NOT NULL DEFAULT 0,
  starts_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at     TIMESTAMPTZ,
  CHECK (company_id IS NOT NULL OR user_id IS NOT NULL)
);

-- ----------------------------------------------------------------------------
-- 16. AI USAGE & AUDIT
-- ----------------------------------------------------------------------------
CREATE TABLE ai_usage_logs (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  feature      VARCHAR(100) NOT NULL,     -- resume_analyzer/mock_interview/code_review...
  model        VARCHAR(100),
  input_tokens INT DEFAULT 0,
  output_tokens INT DEFAULT 0,
  cost_estimate NUMERIC(10,6) DEFAULT 0,
  latency_ms   INT,
  prompt_preview TEXT,
  success      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_usage_user ON ai_usage_logs(user_id, created_at);

CREATE TABLE audit_logs (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  action       audit_action NOT NULL,
  resource     VARCHAR(100),
  resource_id  VARCHAR(50),
  entity_before JSONB,
  entity_after  JSONB,
  ip_address VARCHAR(45),
  user_agent   VARCHAR(255),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_user ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_resource ON audit_logs(resource, resource_id);

-- ----------------------------------------------------------------------------
-- 17. FILE STORAGE METADATA
-- ----------------------------------------------------------------------------
CREATE TABLE stored_files (
  id            BIGSERIAL PRIMARY KEY,
  uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
  original_name VARCHAR(255) NOT NULL,
  storage_path  VARCHAR(500) NOT NULL,
  mime_type     VARCHAR(100),
  size_bytes    BIGINT,
  uploaded_by   BIGINT REFERENCES users(id) ON DELETE SET NULL,
  entity_type   VARCHAR(30),
  entity_id     BIGINT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- TRIGGERS: auto-maintain updated_at
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated       BEFORE UPDATE ON users          FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_companies_updated   BEFORE UPDATE ON companies      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_jobs_updated        BEFORE UPDATE ON jobs           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_applications_updated BEFORE UPDATE ON applications FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_interviews_updated  BEFORE UPDATE ON interviews     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_coding_tests_updated BEFORE UPDATE ON coding_tests FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------------------
-- INDEXES (search/filter hot paths)
-- ----------------------------------------------------------------------------
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_title ON jobs USING GIN (to_tsvector('english', title || ' ' || description));
CREATE INDEX idx_questions_text ON questions USING GIN (to_tsvector('english', question || ' ' || COALESCE(answer,'')));
CREATE INDEX idx_companies_name ON companies(name);
CREATE INDEX idx_resume_versions_resume ON resume_versions(resume_id);
CREATE INDEX idx_ats_reports_resume ON ats_reports(resume_id, created_at DESC);
CREATE INDEX idx_ats_reports_candidate ON ats_reports(candidate_id, score DESC);

-- ============================================================================
-- END OF SCHEMA — 40 tables
-- ============================================================================
