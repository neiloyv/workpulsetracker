import { useCallback, useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { api, Me } from "./api";
import { AuthCallbackPage } from "./pages/AuthCallbackPage";
import { LandingPage } from "./pages/LandingPage";
import { OnboardingPage } from "./pages/OnboardingPage";
import { OrganizationPage } from "./pages/OrganizationPage";
import { t } from "./i18n";

export default function App() {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [, setTick] = useState(0);

  const refreshLanguage = useCallback(() => setTick((value) => value + 1), []);

  useEffect(() => {
    api
      .getMe()
      .then(setMe)
      .catch(() => setMe(null))
      .finally(() => setLoading(false));
  }, []);

  async function handleLogout() {
    try {
      await api.logout();
    } catch {
      // ignore
    }
    setMe(null);
  }

  if (loading) {
    return <div className="page">{t("common.loading")}</div>;
  }

  return (
    <Routes>
      <Route path="/" element={<LandingPage me={me} onLanguageChange={refreshLanguage} />} />
      <Route
        path="/auth/callback"
        element={<AuthCallbackPage onReady={setMe} />}
      />
      <Route
        path="/onboarding"
        element={me ? <OnboardingPage /> : <Navigate to="/" replace />}
      />
      <Route
        path="/org"
        element={
          me?.onboarded ? (
            <OrganizationPage me={me} onLogout={handleLogout} />
          ) : me ? (
            <Navigate to="/onboarding" replace />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
