import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { t } from "../i18n";

export function OnboardingPage() {
  const navigate = useNavigate();
  const [companyName, setCompanyName] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await api.completeOnboarding({ companyName, firstName, lastName });
      navigate("/org");
    } catch (err) {
      setError(err instanceof Error ? err.message : t("common.error"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card" style={{ maxWidth: 520, margin: "4rem auto" }}>
        <h1 style={{ fontFamily: "Fraunces, Georgia, serif", marginTop: 0 }}>{t("onboarding.title")}</h1>
        <form className="form-grid" onSubmit={onSubmit}>
          <label>
            {t("onboarding.company")}
            <input value={companyName} onChange={(e) => setCompanyName(e.target.value)} required />
          </label>
          <label>
            {t("onboarding.firstName")}
            <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
          </label>
          <label>
            {t("onboarding.lastName")}
            <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
          </label>
          {error && <div className="alert">{error}</div>}
          <button className="btn btn-primary" disabled={loading}>
            {loading ? t("common.loading") : t("onboarding.submit")}
          </button>
        </form>
      </div>
    </div>
  );
}
