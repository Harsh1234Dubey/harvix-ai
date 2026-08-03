import { useAuth } from '@/providers/AuthProvider'
import CandidateDashboard from '@/pages/candidate/CandidateDashboard'
import RecruiterDashboard from '@/pages/recruiter/Dashboard'
import AdminDashboard from '@/pages/admin/Dashboard'

export default function Dashboard() {
  const { user } = useAuth()
  if (user?.roles.includes('ADMIN')) return <AdminDashboard />
  if (user?.roles.includes('RECRUITER')) return <RecruiterDashboard />
  return <CandidateDashboard />
}
