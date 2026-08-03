export interface ApiResponse<T> {
  status: number
  message?: string
  data: T
  timestamp?: string
  path?: string
}

export interface ApiErrorBody {
  status?: number
  message?: string
  error?: string
  fieldErrors?: { field: string; message: string }[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface User {
  id: number
  uuid: string
  firstName: string
  lastName: string
  email: string
  avatarUrl?: string
  phone?: string
  status: string
  emailVerified: boolean
  roles: string[]
  createdAt: string
}

export interface AuthData {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  rememberMe: boolean
  user: User
}

export interface Job {
  id: number
  uuid: string
  companyId: number
  companyName: string
  companyLogo?: string
  title: string
  slug: string
  description: string
  location?: string
  workMode?: string
  employmentType?: string
  experienceMin?: number
  experienceMax?: number
  salaryMin?: number
  salaryMax?: number
  currency?: string
  vacancyCount: number
  status: string
  viewsCount: number
  applicationsCount: number
  expiresAt?: string
  publishedAt?: string
  createdAt: string
  requiredSkills: string[]
}

export interface Application {
  id: number
  uuid: string
  jobId: number
  jobTitle: string
  companyName: string
  candidateId: number
  candidateName: string
  status: string
  coverLetter?: string
  atsScore?: number
  matchPercentage?: number
  recruiterNotes?: string
  appliedAt: string
  updatedAt: string
}

export interface Interview {
  id: number
  uuid: string
  applicationId?: number
  candidateId: number
  candidateName: string
  recruiterId?: number
  title: string
  type: string
  status: string
  scheduledAt?: string
  durationMin: number
  location?: string
  meetingLink?: string
  difficulty?: string
  score?: number
  hiringRecommendation?: string
  feedbackSummary?: string
}

export interface InterviewSession {
  id: number
  uuid?: string
  interviewId?: number
  candidateId: number
  status: string
  skill?: string
  difficulty?: string
  startedAt?: string
  endedAt?: string
  score?: number
  questions?: InterviewQuestion[]
}

export interface InterviewQuestion {
  id: number
  sessionId: number
  questionText: string
  topic?: string
  difficulty?: string
  category?: string
  followUp: boolean
  orderIndex: number
}

export interface InterviewAnswer {
  id: number
  questionId: number
  answerText?: string
  score?: number
  feedback?: string
  answeredAt?: string
}

export interface InterviewFeedback {
  id: number
  sessionId: number
  overallScore?: number
  communicationScore?: number
  confidenceScore?: number
  technicalScore?: number
  grammarScore?: number
  fluencyScore?: number
  keywordMatchScore?: number
  speakingSpeedScore?: number
  strengths: string[]
  weaknesses: string[]
  suggestions?: string
  hiringRecommendation?: string
  detailed?: { question?: string; score?: number; comment?: string }[]
}

export interface CodingTest {
  id: number
  title: string
  description: string
  language: string
  difficulty: string
  timeLimitSec: number
  memoryLimitMb: number
  starterCode?: string
  publicTest: boolean
  createdAt: string
}

export interface TestCase {
  id: number
  inputData?: string
  expectedOutput: string
  hidden: boolean
  orderIndex: number
}

export interface Submission {
  id: number
  uuid: string
  codingTestId: number
  language: string
  status: string
  passedCases: number
  totalCases: number
  executionTimeMs?: number
  memoryUsedKb?: number
  stdout?: string
  stderr?: string
  errorMessage?: string
  codeScore?: number
  complexityTime?: string
  complexitySpace?: string
  submittedAt: string
}

export interface Question {
  id: number
  topic: string
  subTopic?: string
  question: string
  answer?: string
  difficulty: string
  type: string
  source?: string
  tags?: string
  viewsCount: number
  createdAt: string
}

export interface QuestionBankQuestion {
  id: number
  topic: string
  subTopic?: string
  question: string
  answer?: string
  difficulty: string
  type?: string
  tags?: string
  viewsCount: number
}

export interface Company {
  id: number
  name: string
  slug: string
  description?: string
  logoUrl?: string
  website?: string
  industry?: string
  location?: string
  sizeRange?: string
  foundedYear?: number
  brandingColor?: string
  verified: boolean
  createdAt: string
}

export interface CompanyMember {
  id: number
  userId: number
  name: string
  email: string
  roleInCompany?: string
  owner: boolean
}

export interface ResumeVersion {
  id: number
  versionNo: number
  filePath: string
  fileSize?: number
  fileType?: string
  uploadedAt: string
}

export interface Resume {
  id: number
  title: string
  currentVersion: number
  primary: boolean
  createdAt: string
  versions: ResumeVersion[]
}

export interface AtsResult {
  resumeId: number
  jobId: number
  jobTitle: string
  score: number
  summary?: string
  strengths: string[]
  gaps: string[]
  matchedKeywords: string[]
  missingKeywords: string[]
  source: string
}

export interface AtsReportEntry {
  id: number
  resumeId: number
  versionNo: number
  jobId: number
  jobTitle: string
  score: number
  summary?: string
  strengths: string[]
  gaps: string[]
  matchedKeywords: string[]
  missingKeywords: string[]
  source: string
  createdAt: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  message: string
  read: boolean
  createdAt: string
}

export interface DashboardStats {
  totalApplications: number
  savedJobs: number
  interviewsScheduled: number
  interviewsCompleted: number
  submissions: number
  totalXp: number
  level: number
  notificationsUnread: number
  averageInterviewScore?: number
  bestResumeScore?: number
}

export interface AnalyticsSummary {
  totalUsers: number
  totalRecruiters: number
  totalCandidates: number
  totalCompanies: number
  totalJobs: number
  totalApplications: number
  totalInterviews: number
  pendingVerifications: number
  applicationsByStatus: Record<string, number>
  jobsByStatus: Record<string, number>
}

export interface RecruiterAnalytics {
  totalApplications: number
  submitted: number
  reviewing: number
  shortlisted: number
  interviewed: number
  hired: number
  rejected: number
  applicationsByJob: Record<string, number>
}

export interface ReportMeta {
  uuid: string
  reportType: string
  title: string
  generatedBy?: number
  format: string
  createdAt: string
}

export interface AuditLog {
  id: number
  userId?: number
  action: string
  resource: string
  resourceId?: string
  ip?: string
  createdAt: string
}

export interface Subscription {
  id: number
  companyId?: number
  userId?: number
  plan: string
  status: string
  aiQuota?: number
  aiUsed?: number
  aiQuotaMonth?: number
  aiUsedMonth?: number
  expiresAt?: string
}

export interface LeaderboardEntry {
  id: number
  userId: number
  name: string
  xp: number
  streak: number
  rank: number
}
