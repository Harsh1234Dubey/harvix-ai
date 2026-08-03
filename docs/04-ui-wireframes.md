# 04 — UI Wireframes

> InterView AI — Phase 1 deliverable. Text wireframes for all key screens.

Design language: **Modern SaaS** · glassmorphism · gradient cards · rounded corners ·
floating sidebar · micro-animations · skeleton loading · responsive · dark/light mode.

---

## 1. Global Layouts

### 1.1 Landing Page
```
┌─────────────────────────────────────────────────────────────────────┐
│ Logo InterView AI    Features  Pricing  FAQ  Contact   Login  Signup │
├─────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────┐   ┌───────────────────────────┐ │
│  │ Animated Hero Headline        │   │  Floating product mockup  │ │
│  │ "AI-powered hiring. Done."    │   │  (glass card, gradients)  │ │
│  │ [Get Started] [Watch Demo]    │   │                           │ │
│  └───────────────────────────────┘   └───────────────────────────┘ │
│  Interactive Stats: 12k+ Candidates | 400+ Companies | 98% Match   │
│  Feature Grid (6 cards) · How It Works (3 steps) · Testimonials    │
│  Pricing (3 tiers) · FAQ (accordion) · Contact form                │
│  Animated Footer (columns + newsletter)                            │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 App Shell (Candidate / Recruiter / Admin)
```
┌────────────┬────────────────────────────────────────────────────────┐
│ FLOATING   │  Topbar: Search  |  Notifications 🔔  | Theme  | User │
│ SIDEBAR    ├────────────────────────────────────────────────────────┤
│ Dashboard  │                                                        │
│ Jobs       │            <Outlet /> — page content                   │
│ Interviews │                                                        │
│ Coding     │                                                        │
│ Reports    │                                                        │
│ Settings   │                                                        │
└────────────┴────────────────────────────────────────────────────────┘
```

---

## 2. Authentication Screens

### 2.1 Login
```
┌─────────────────────────────┐
│   InterView AI              │
│   ┌───────────────────────┐ │
│   │ Email  [__________]   │ │
│   │ Password [__________] │ │
│   │ [ ] Remember me       │ │
│   │ [Sign In]             │ │
│   │ Forgot password?      │ │
│   │ — or —  Register      │ │
│   └───────────────────────┘ │
└─────────────────────────────┘
```

### 2.2 Register (role selection)
```
Role: ( ) Candidate  ( ) Recruiter
First / Last Name · Email · Password · Confirm · Terms checkbox
[Create Account] → simulated email verification modal ("Check your inbox — demo")
```

### 2.3 Forgot / Reset Password
Email → "We sent a reset link" (simulated) → new password + confirm → success.

---

## 3. Candidate Screens

### 3.1 Candidate Dashboard
```
┌─── Stat Cards ───────────────────────────────────────────────────┐
│ Applications  |  Interviews  |  Resume Score  |  XP / Level      │
├────────────────────────────┬─────────────────────────────────────┤
│  Resume Score Gauge        │  Applications Chart (line)          │
│  (ATS + Resume %)          │  Upcoming Interviews (list)         │
│  Skill Heatmap             │  Recent Activity / Notifications    │
└────────────────────────────┴─────────────────────────────────────┘
```

### 3.2 Resume Upload & Analyzer
```
Resume List (versions)           Analyzer Panel
┌───────────────────────────┐   ┌──────────────────────────────────┐
│ My Resume v3  [Primary]   │   │ Score ring 82/100 · ATS 74/100    │
│  Upload New (PDF, drag)   │   │ Skills: [Java][Spring][SQL][…]    │
│  v3  2 days ago  [View]   │   │ Missing: Docker, Kafka            │
│  v2  …                    │   │ Grammar & Formatting suggestions  │
│  v1  …                    │   │ Match vs Job: 78%  [Skill Gap]    │
└───────────────────────────┘   │ Recommended certs / projects      │
                                └──────────────────────────────────┘
```

### 3.3 Job Portal & Application Tracking
```
Search [_______] Filters: Location | WorkMode | Type | Salary
┌──────────────────────────────────────────────────────────────┐
│ Senior Java Developer — Acme Corp   ★ Save   [Apply] 78% match│
│ (list of job cards with match badge)                          │
└──────────────────────────────────────────────────────────────┘
Application Status pipeline: SUBMITTED → REVIEWING → INTERVIEW → OFFERED
```

### 3.4 AI Mock Interview
```
┌──────────────────────────┬─────────────────────────────────────┐
│ Setup: Skill ▾ | Diff ▾   │  Question Panel                    │
│ Camera ▓ Mic ▓ Preview    │  Q3/8: Explain how JPA works...    │
│ Timer 00:04:32  ● REC     │  Answer box (text or voice→text)   │
│ Controls: Skip|Next|End   │  Transcript live stream            │
│                           │  [Submit]                          │
└──────────────────────────┴─────────────────────────────────────┘
Feedback screen: radar chart (communication/confidence/tech/grammar…)
Strengths · Weaknesses · Learning suggestions · Hiring rec
```

### 3.5 Coding Platform
```
┌─────────────────────────────┬──────────────────────────────────┐
│ Problem: Two Sum   [Java ▾] │  Monaco Editor                   │
│ Description + constraints   │  ┌────────────────────────────┐  │
│ Test: public ✓ hidden ✓     │  │  code goes here...         │  │
│ Time/Space limits           │  └────────────────────────────┘  │
│                             │  [Run] [Submit]   Console output │
└─────────────────────────────┴──────────────────────────────────┘
After submit: pass/fail cases + AI Code Review panel
```

### 3.6 Gamification / Leaderboard / Certificates
```
Level 7 · XP bar · Streak 12 🔥
Badges grid (unlocked/locked)   |   Leaderboard (rank, XP, streak)
Certificates (download PDF)     |   Performance dashboard (charts)
```

---

## 4. Recruiter Screens

### 4.1 Recruiter Dashboard
```
Stat cards: Open Jobs | Applications | Shortlisted | Hires
Hiring Funnel (bar) · Skill Distribution (doughnut) · Top Performers (table)
AI Assistant (side panel): "Show top Java candidates" → results
```

### 4.2 Job Management
```
Create Job (form: title, desc, skills, salary, mode, expiry)
Job List (table: title, status, applicants, views, actions)
Candidate pool per job: search → compare (side-by-side) → shortlist/reject → schedule
```

### 4.3 Candidate Search & Compare
```
Filters: skills | min ATS | location | experience
Results table (checkbox)  → [Compare] shows side-by-side scores
Shortlist / Reject actions → triggers notification to candidate
```

### 4.4 AI Recruiter Assistant (Chat)
```
"You: show top Java candidates"
AI: 5 candidates ≥85% ATS — names + match + actions [View][Compare]
"You: compare A vs B" → table of scores, skills, recommendation
"You: generate hiring summary" → downloadable report
```

---

## 5. Admin Screens

### 5.1 Admin Dashboard
```
Platform KPIs: Total Users | Active Recruiters | Jobs | Interviews
Platform Analytics charts · AI Usage (calls, tokens, cost) · Audit log feed
```

### 5.2 Management Tables
```
Tabs: Users | Recruiters | Companies | Jobs | Interviews | Reports
DataTable: search, filter, sort, pagination, bulk actions
Subscription panel: plan, quota used/left, renew/expire
```

---

## 6. Shared Components

- `DataTable` (sort/filter/search/pagination/row actions)
- `StatCard` (icon, value, delta, sparkline)
- `ScoreRing` / `ScoreBar` (circular + linear)
- `RadarChart`, `LineChart`, `DoughnutChart`, `BarChart` (Chart.js wrappers)
- `Skeleton` loaders for all async sections
- `EmptyState`, `ConfirmDialog`, `Toast`, `Badge`, `Avatar`, `Modal`
- `GlassCard` (glassmorphism), `GradientButton`, `ThemeToggle`
- `UploadDropzone`, `InterviewPlayer`, `AIChatPanel`, `CodeEditorPanel`

## 7. Responsive Breakpoints

| Breakpoint | Layout                              |
|-----------|-------------------------------------|
| < 640px    | Single column, hamburger sidebar    |
| 640–1024px | 2-column, collapsible sidebar       |
| > 1024px   | Full 3-column dashboard grids       |
