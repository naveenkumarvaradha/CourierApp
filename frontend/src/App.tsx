import { Navigate, Route, Routes } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/admin/UsersPage';
import RolesPage from './pages/admin/RolesPage';
import ApprovalRoutingPage from './pages/admin/ApprovalRoutingPage';
import CompanySettingsPage from './pages/admin/CompanySettingsPage';
import CourierWaysPage from './pages/admin/CourierWaysPage';
import PackageTypesPage from './pages/admin/PackageTypesPage';
import FlexFieldsPage from './pages/admin/FlexFieldsPage';
import DepartmentsPage from './pages/admin/DepartmentsPage';
import AuditLogsPage from './pages/admin/AuditLogsPage';
import CompaniesPage from './pages/admin/CompaniesPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import PartiesPage from './pages/master/PartiesPage';
import BookingsPage from './pages/booking/BookingsPage';
import BookingFormPage from './pages/booking/BookingFormPage';
import ReportsPage from './pages/reports/ReportsPage';

function FullScreenLoader() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <CircularProgress />
    </Box>
  );
}

function RequirePermission({ codes, children }: { codes: string[]; children: JSX.Element }) {
  const { hasAnyPermission } = useAuth();
  if (codes.length > 0 && !hasAnyPermission(codes)) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return <FullScreenLoader />;
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />

        <Route
          path="admin/users"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <UsersPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/roles"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <RolesPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/approval-routing"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <ApprovalRoutingPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/company-settings"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <CompanySettingsPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/courier-ways"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <CourierWaysPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/package-types"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <PackageTypesPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/flex-fields"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <FlexFieldsPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/departments"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <DepartmentsPage />
            </RequirePermission>
          }
        />

        <Route
          path="admin/audit-logs"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <AuditLogsPage />
            </RequirePermission>
          }
        />
        <Route
          path="admin/companies"
          element={
            <RequirePermission codes={['ADMIN_VIEW']}>
              <CompaniesPage />
            </RequirePermission>
          }
        />

        <Route
          path="master/parties"
          element={
            <RequirePermission codes={['MASTER_VIEW']}>
              <PartiesPage />
            </RequirePermission>
          }
        />

        <Route
          path="bookings"
          element={
            <RequirePermission codes={['BOOKING_VIEW']}>
              <BookingsPage />
            </RequirePermission>
          }
        />
        <Route
          path="bookings/new"
          element={
            <RequirePermission codes={['BOOKING_CREATE']}>
              <BookingFormPage />
            </RequirePermission>
          }
        />
        <Route
          path="bookings/:id/edit"
          element={
            <RequirePermission codes={['BOOKING_UPDATE']}>
              <BookingFormPage />
            </RequirePermission>
          }
        />

        <Route
          path="reports"
          element={
            <RequirePermission codes={['REPORTS_VIEW']}>
              <ReportsPage />
            </RequirePermission>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
