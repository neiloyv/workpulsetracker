import { Loader2 } from "lucide-react";
import { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { useApp } from "./context/AppContext";
import { useTheme } from "./context/ThemeContext";
import { AgentPage } from "./pages/AgentPage";
import { DashboardPage } from "./pages/DashboardPage";
import { LandingPage } from "./pages/LandingPage";
import { ManagersPage } from "./pages/ManagersPage";
import { WorkersPage } from "./pages/WorkersPage";

function FullscreenLoader() {
  const { theme } = useTheme();
  return (
    <div
      className={`flex min-h-screen w-full items-center justify-center ${
        theme === "dark" ? "app-bg-dark" : "app-bg-light"
      }`}
    >
      <Loader2 className="h-8 w-8 animate-spin text-brand-500" />
    </div>
  );
}

function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useApp();
  if (loading) {
    return <FullscreenLoader />;
  }
  if (!me) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function RequireCompanyManager({ children }: { children: ReactNode }) {
  const { canManageCompany } = useApp();
  if (!canManageCompany) {
    return <Navigate to="/app" replace />;
  }
  return <>{children}</>;
}

function RequireLinkedWorker({ children }: { children: ReactNode }) {
  const { me } = useApp();
  if (!me?.workerId) {
    return <Navigate to="/app" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  const { loading } = useApp();

  if (loading) {
    return <FullscreenLoader />;
  }

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route
        path="/app"
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route
          path="workers"
          element={
            <RequireCompanyManager>
              <WorkersPage />
            </RequireCompanyManager>
          }
        />
        <Route
          path="managers"
          element={
            <RequireCompanyManager>
              <ManagersPage />
            </RequireCompanyManager>
          }
        />
        <Route
          path="agent"
          element={
            <RequireLinkedWorker>
              <AgentPage />
            </RequireLinkedWorker>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
