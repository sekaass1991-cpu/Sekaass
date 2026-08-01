import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ensureSeedData } from './lib/seed';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminLayout } from './components/layout/AdminLayout';
import { ClientLayout } from './components/layout/ClientLayout';
import { Login } from './pages/Login';

import { AdminDashboard } from './pages/admin/Dashboard';
import { AdminClients } from './pages/admin/Clients';
import { AdminInvoices } from './pages/admin/Invoices';
import { AdminAdvancedInvoices } from './pages/admin/AdvancedInvoices';
import { AdminCompliance } from './pages/admin/Compliance';
import { AdminLedger } from './pages/admin/Ledger';
import { AdminFinancials } from './pages/admin/Financials';
import { AdminStatements } from './pages/admin/Statements';
import { AdminDocuments } from './pages/admin/Documents';
import { AdminMessages } from './pages/admin/Messages';
import { AdminNotifications } from './pages/admin/Notifications';
import { AdminAuditTrail } from './pages/admin/AuditTrail';
import { AdminReminders } from './pages/admin/Reminders';
import { AdminAiAssistant } from './pages/admin/AiAssistant';
import { AdminServices } from './pages/admin/Services';
import { AdminWorklist } from './pages/admin/Worklist';
import { AdminCredentials } from './pages/admin/Credentials';
import { AdminSettings } from './pages/admin/Settings';
import { AdminProfile } from './pages/admin/Profile';

import { ClientDashboard } from './pages/client/Dashboard';
import { ClientDocuments } from './pages/client/Documents';
import { ClientMessages } from './pages/client/Messages';
import { ClientNotifications } from './pages/client/Notifications';
import { ClientServices } from './pages/client/Services';
import { ClientAiAssistant } from './pages/client/AiAssistant';
import { ClientSettings } from './pages/client/Settings';
import { ClientProfile } from './pages/client/Profile';

function RootRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === 'admin' ? '/admin' : '/client'} replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<Login />} />

      <Route
        path="/admin"
        element={
          <ProtectedRoute role="admin">
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminDashboard />} />
        <Route path="clients" element={<AdminClients />} />
        <Route path="invoices" element={<AdminInvoices />} />
        <Route path="invoices/advanced" element={<AdminAdvancedInvoices />} />
        <Route path="compliance" element={<AdminCompliance />} />
        <Route path="ledger" element={<AdminLedger />} />
        <Route path="financials" element={<AdminFinancials />} />
        <Route path="statements" element={<AdminStatements />} />
        <Route path="documents" element={<AdminDocuments />} />
        <Route path="messages" element={<AdminMessages />} />
        <Route path="notifications" element={<AdminNotifications />} />
        <Route path="audit-trail" element={<AdminAuditTrail />} />
        <Route path="reminders" element={<AdminReminders />} />
        <Route path="ai" element={<AdminAiAssistant />} />
        <Route path="services" element={<AdminServices />} />
        <Route path="worklist" element={<AdminWorklist />} />
        <Route path="credentials" element={<AdminCredentials />} />
        <Route path="settings" element={<AdminSettings />} />
        <Route path="profile" element={<AdminProfile />} />
      </Route>

      <Route
        path="/client"
        element={
          <ProtectedRoute role="client">
            <ClientLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<ClientDashboard />} />
        <Route path="documents" element={<ClientDocuments />} />
        <Route path="messages" element={<ClientMessages />} />
        <Route path="notifications" element={<ClientNotifications />} />
        <Route path="services" element={<ClientServices />} />
        <Route path="ai" element={<ClientAiAssistant />} />
        <Route path="settings" element={<ClientSettings />} />
        <Route path="profile" element={<ClientProfile />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    ensureSeedData();
    setReady(true);
  }, []);

  if (!ready) return null;

  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
