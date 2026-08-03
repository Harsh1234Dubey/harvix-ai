import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from '@/providers/AuthProvider'
import AppLayout from '@/layouts/AppLayout'
import { Spinner } from '@/components/ui'

import Landing from '@/pages/landing/Landing'
import Login from '@/pages/auth/Login'
import Register from '@/pages/auth/Register'
import ForgotPassword from '@/pages/auth/ForgotPassword'
import ResetPassword from '@/pages/auth/ResetPassword'
import VerifyEmail from '@/pages/auth/VerifyEmail'

import Profile from '@/pages/common/Profile'
import Reports from '@/pages/common/Reports'
import NotFound from '@/pages/common/NotFound'

import CandidateDashboard from '@/pages/candidate/Dashboard'
import Jobs from '@/pages/candidate/Jobs'
import JobDetail from '@/pages/candidate/JobDetail'
import Interviews from '@/pages/candidate/Interviews'
import InterviewSession from '@/pages/candidate/InterviewSession'
import Coding from '@/pages/candidate/Coding'
import CodingProblem from '@/pages/candidate/CodingProblem'
import Questions from '@/pages/candidate/Questions'
import Resumes from '@/pages/candidate/Resumes'
import Bookmarks from '@/pages/candidate/Bookmarks'

import RecruiterDashboard from '@/pages/recruiter/Dashboard'
import Company from '@/pages/recruiter/Company'
import PostJob from '@/pages/recruiter/PostJob'
import Applications from '@/pages/recruiter/Applications'
import Analytics from '@/pages/recruiter/Analytics'

import AdminDashboard from '@/pages/admin/Dashboard'
import AdminUsers from '@/pages/admin/Users'
import AdminSubscriptions from '@/pages/admin/Subscriptions'
import AdminAudit from '@/pages/admin/Audit'

function Protected({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Spinner label="Loading…" />
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  return <>{children}</>
}

function RequireRole({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user?.roles.some((r) => roles.includes(r))) {
    return <Navigate to="/app/dashboard" replace />
  }
  return <>{children}</>
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/verify-email" element={<VerifyEmail />} />

      <Route
        path="/app"
        element={
          <Protected>
            <AppLayout />
          </Protected>
        }
      >
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<CandidateDashboard />} />
        <Route path="profile" element={<Profile />} />
        <Route path="reports" element={<Reports />} />

        <Route path="jobs" element={<Jobs />} />
        <Route path="jobs/new" element={<RequireRole roles={['RECRUITER', 'ADMIN']}><PostJob /></RequireRole>} />
        <Route path="jobs/:uuid" element={<JobDetail />} />
        <Route path="jobs/:uuid/applications" element={<RequireRole roles={['RECRUITER', 'ADMIN']}><Applications /></RequireRole>} />
        <Route path="interviews" element={<Interviews />} />
        <Route path="interviews/session/:id" element={<InterviewSession />} />
        <Route path="coding" element={<Coding />} />
        <Route path="coding/:id" element={<CodingProblem />} />
        <Route path="questions" element={<Questions />} />
        <Route path="resumes" element={<Resumes />} />
        <Route path="bookmarks" element={<Bookmarks />} />

        <Route path="company" element={<RequireRole roles={['RECRUITER', 'ADMIN']}><Company /></RequireRole>} />
        <Route path="analytics" element={<RequireRole roles={['RECRUITER', 'ADMIN']}><Analytics /></RequireRole>} />

        <Route path="admin" element={<RequireRole roles={['ADMIN']}><AdminDashboard /></RequireRole>} />
        <Route path="admin/users" element={<RequireRole roles={['ADMIN']}><AdminUsers /></RequireRole>} />
        <Route path="admin/subscriptions" element={<RequireRole roles={['ADMIN']}><AdminSubscriptions /></RequireRole>} />
        <Route path="admin/audit" element={<RequireRole roles={['ADMIN']}><AdminAudit /></RequireRole>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
