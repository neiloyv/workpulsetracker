import { Loader2 } from "lucide-react";
import { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { useApp } from "./context/AppContext";
import { useTheme } from "./context/ThemeContext";
import { DashboardPage } from "./pages/DashboardPage";
import { EmployeesPage } from "./pages/EmployeesPage";
import { LandingPage } from "./pages/LandingPage";

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

function RequireOwner({ children }: { children: ReactNode }) {
  const { me, isOwner } = useApp();
  if (me?.accountType === "PERSONAL" || !isOwner) {
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
          path="employees"
          element={
            <RequireOwner>
              <EmployeesPage />
            </RequireOwner>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
