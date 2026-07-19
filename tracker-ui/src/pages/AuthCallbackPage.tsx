import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, Me } from "../api";
import { t } from "../i18n";

export function AuthCallbackPage({ onReady }: { onReady: (me: Me) => void }) {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getMe()
      .then((me) => {
        onReady(me);
        navigate(me.onboarded ? "/org" : "/onboarding", { replace: true });
      })
      .catch(() => setError(t("common.error")));
  }, [navigate, onReady]);

  return (
    <div className="page">
      <div className="card">{error ?? t("common.loading")}</div>
    </div>
  );
}
