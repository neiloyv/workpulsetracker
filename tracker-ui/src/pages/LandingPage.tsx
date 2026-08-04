import { Clock, Download, HardDrive, Moon, Shield, Sun, Building2, User } from "lucide-react";
import { FormEvent, ReactNode, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, Downloads } from "../api";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { useApp } from "../context/AppContext";
import { useLocale } from "../context/LocaleContext";
import { useTheme } from "../context/ThemeContext";
import { TranslationKey } from "../i18n";
import { mapApiError } from "../utils/errors";

type AuthTab = "login" | "register";
type RegisterKind = "INDIVIDUAL" | "COMPANY";

const FEATURES: Array<{
  icon: typeof Shield;
  titleKey: TranslationKey;
  descriptionKey: TranslationKey;
}> = [
  {
    icon: HardDrive,
    titleKey: "landing.feature.solo.title",
    descriptionKey: "landing.feature.solo.description"
  },
  {
    icon: Shield,
    titleKey: "landing.feature.privacy.title",
    descriptionKey: "landing.feature.privacy.description"
  },
  {
    icon: Clock,
    titleKey: "landing.feature.timeline.title",
    descriptionKey: "landing.feature.timeline.description"
  }
];

const DOWNLOAD_FALLBACK: Downloads = {
  windowsUrl: "/downloads/workpulsetracker-agent-windows.zip",
  macosUrl: "/downloads/workpulsetracker-agent-macos.dmg",
  linuxUrl: "/downloads/workpulsetracker-agent-linux.deb"
};

export function LandingPage() {
  const navigate = useNavigate();
  const { me, setMe } = useApp();
  const { theme, toggleTheme } = useTheme();
  const { t } = useLocale();
  const [tab, setTab] = useState<AuthTab>("login");
  const [registerKind, setRegisterKind] = useState<RegisterKind>("INDIVIDUAL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [downloads, setDownloads] = useState<Downloads>(DOWNLOAD_FALLBACK);

  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");

  const [companyName, setCompanyName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");

  useEffect(() => {
    if (me) {
      navigate("/app", { replace: true });
    }
  }, [me, navigate]);

  useEffect(() => {
    api
      .getDownloads()
      .then(setDownloads)
      .catch(() => {
        setDownloads(DOWNLOAD_FALLBACK);
      });
  }, []);

  async function onLogin(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const result = await api.login({ email: loginEmail, password: loginPassword });
      setMe(result);
      navigate("/app");
    } catch (err) {
      setError(mapApiError(err instanceof Error ? err.message : "", t("landing.auth.loginError")));
    } finally {
      setLoading(false);
    }
  }

  async function onRegister(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const result = await api.register({
        organizationType: registerKind,
        email: registerEmail,
        password: registerPassword,
        displayName,
        companyName: registerKind === "COMPANY" ? companyName : undefined
      });
      setMe(result);
      navigate(result.workerId ? "/app/agent" : "/app");
    } catch (err) {
      setError(mapApiError(err instanceof Error ? err.message : "", t("landing.auth.registerError")));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={`min-h-screen w-full ${theme === "dark" ? "app-bg-dark" : "app-bg-light"}`}>
      <div className="fixed right-5 top-5 z-20 flex items-center gap-2">
        <LanguageSwitcher />
        <button
          onClick={toggleTheme}
          className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white/80 text-slate-600 shadow-sm backdrop-blur transition hover:scale-105 hover:text-brand-600 dark:border-white/10 dark:bg-white/5 dark:text-slate-300 dark:hover:text-brand-300"
          aria-label={t("common.themeToggle")}
        >
          {theme === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
        </button>
      </div>

      <div className="mx-auto w-full max-w-7xl px-6 py-16 lg:px-10">
        <div className="grid grid-cols-1 items-center gap-12 lg:grid-cols-2 lg:gap-12">
          <div className="animate-fade-in">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-brand-500/30 bg-brand-500/10 px-3.5 py-1.5 text-xs font-semibold uppercase tracking-wider text-brand-600 dark:text-brand-300">
              <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
              {t("common.brand")}
            </div>

            <div className="mb-6 inline-flex max-w-xl items-start gap-2.5 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm font-medium leading-snug text-emerald-700 dark:text-emerald-300">
              <span className="mt-0.5 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-xs font-bold">
                ✓
              </span>
              {t("landing.trialBadge")}
            </div>

            <h1 className="font-display text-4xl font-bold leading-[1.08] tracking-tight text-slate-900 dark:text-white sm:text-5xl lg:text-[3.4rem]">
              {t("landing.hero.line1")}
              <br />
              <span className="bg-gradient-to-r from-brand-600 via-brand-500 to-sky-500 bg-clip-text text-transparent dark:from-brand-400 dark:via-brand-300 dark:to-sky-300">
                {t("landing.hero.line2")}
              </span>
            </h1>
            <p className="mt-6 max-w-2xl text-lg leading-relaxed text-slate-600 dark:text-slate-300">
              {t("landing.hero.subheadline")}
            </p>

            <div className="mt-10 grid gap-5">
              {FEATURES.map((feature) => (
                <div key={feature.titleKey} className="flex items-start gap-4">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-brand-600 shadow-sm dark:border-white/10 dark:bg-white/5 dark:text-brand-300">
                    <feature.icon className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="font-display text-base font-semibold text-slate-900 dark:text-white">
                      {t(feature.titleKey)}
                    </div>
                    <div className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
                      {t(feature.descriptionKey)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="animate-fade-in-scale mx-auto w-full max-w-md">
            <div className="glass-panel rounded-3xl p-7 shadow-glow sm:p-8">
              <div className="mb-6 flex rounded-xl bg-slate-100 p-1 dark:bg-white/5">
                <button
                  className={`flex-1 rounded-lg py-2 text-sm font-semibold transition ${
                    tab === "login"
                      ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
                      : "text-slate-500 dark:text-slate-400"
                  }`}
                  onClick={() => {
                    setTab("login");
                    setError(null);
                  }}
                >
                  {t("landing.auth.loginTab")}
                </button>
                <button
                  className={`flex-1 rounded-lg py-2 text-sm font-semibold transition ${
                    tab === "register"
                      ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
                      : "text-slate-500 dark:text-slate-400"
                  }`}
                  onClick={() => {
                    setTab("register");
                    setError(null);
                  }}
                >
                  {t("landing.auth.registerTab")}
                </button>
              </div>

              {tab === "login" ? (
                <form className="grid gap-4" onSubmit={onLogin}>
                  <Field label={t("landing.auth.email")}>
                    <input
                      type="email"
                      required
                      value={loginEmail}
                      onChange={(e) => setLoginEmail(e.target.value)}
                      placeholder="you@company.com"
                      className={inputClass}
                    />
                  </Field>
                  <Field label={t("landing.auth.password")}>
                    <input
                      type="password"
                      required
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      placeholder={t("landing.auth.passwordPlaceholder")}
                      className={inputClass}
                    />
                  </Field>
                  {error && <ErrorBanner message={error} />}
                  <button type="submit" disabled={loading} className={primaryButtonClass}>
                    {loading ? t("landing.auth.loginLoading") : t("landing.auth.loginSubmit")}
                  </button>
                </form>
              ) : (
                <form className="grid gap-4" onSubmit={onRegister}>
                  <div className="grid grid-cols-2 gap-2 rounded-xl bg-slate-100 p-1 dark:bg-white/5">
                    <button
                      type="button"
                      onClick={() => setRegisterKind("INDIVIDUAL")}
                      className={`flex items-center justify-center gap-1.5 rounded-lg py-2 text-sm font-semibold transition ${
                        registerKind === "INDIVIDUAL"
                          ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
                          : "text-slate-500 dark:text-slate-400"
                      }`}
                    >
                      <User className="h-4 w-4" />
                      {t("landing.auth.kindIndividual")}
                    </button>
                    <button
                      type="button"
                      onClick={() => setRegisterKind("COMPANY")}
                      className={`flex items-center justify-center gap-1.5 rounded-lg py-2 text-sm font-semibold transition ${
                        registerKind === "COMPANY"
                          ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
                          : "text-slate-500 dark:text-slate-400"
                      }`}
                    >
                      <Building2 className="h-4 w-4" />
                      {t("landing.auth.kindCompany")}
                    </button>
                  </div>

                  {registerKind === "COMPANY" && (
                    <Field label={t("landing.auth.companyName")}>
                      <input
                        required
                        value={companyName}
                        onChange={(e) => setCompanyName(e.target.value)}
                        placeholder={t("landing.auth.companyNamePlaceholder")}
                        className={inputClass}
                      />
                    </Field>
                  )}
                  <Field
                    label={
                      registerKind === "COMPANY" ? t("landing.auth.ownerName") : t("landing.auth.yourName")
                    }
                  >
                    <input
                      required
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder={t("landing.auth.namePlaceholder")}
                      className={inputClass}
                    />
                  </Field>
                  <Field label={t("landing.auth.email")}>
                    <input
                      type="email"
                      required
                      value={registerEmail}
                      onChange={(e) => setRegisterEmail(e.target.value)}
                      placeholder="you@company.com"
                      className={inputClass}
                    />
                  </Field>
                  <Field label={t("landing.auth.password")}>
                    <input
                      type="password"
                      required
                      minLength={8}
                      value={registerPassword}
                      onChange={(e) => setRegisterPassword(e.target.value)}
                      placeholder={t("landing.auth.passwordMinPlaceholder")}
                      className={inputClass}
                    />
                  </Field>
                  {error && <ErrorBanner message={error} />}
                  <button type="submit" disabled={loading} className={primaryButtonClass}>
                    {loading ? t("landing.auth.registerLoading") : t("landing.auth.registerSubmit")}
                  </button>
                </form>
              )}
            </div>
            <p className="mt-5 text-center text-xs text-slate-400 dark:text-slate-500">
              {t("landing.auth.terms")}
            </p>
          </div>
        </div>

        <div className="animate-fade-in mt-14 rounded-2xl border border-slate-200 bg-white/70 px-6 py-5 shadow-sm backdrop-blur dark:border-white/10 dark:bg-white/[0.04] sm:px-8">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="font-display text-base font-semibold text-slate-900 dark:text-white">
                {t("landing.download.title")}
              </div>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                {t("landing.download.subtitle")}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <DownloadLink href={downloads.windowsUrl} label={t("landing.download.windows")} />
              <DownloadLink href={downloads.macosUrl} label={t("landing.download.macos")} />
              <DownloadLink href={downloads.linuxUrl} label={t("landing.download.linux")} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="grid gap-1.5 text-sm">
      <span className="font-medium text-slate-600 dark:text-slate-300">{label}</span>
      {children}
    </label>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-rose-300/50 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-600 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
      {message}
    </div>
  );
}

function DownloadLink({ href, label }: { href: string; label: string }) {
  if (!href) {
    return null;
  }
  const fileName = href.split("/").filter(Boolean).at(-1);
  return (
    <a
      href={href}
      download={fileName}
      className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:text-brand-300"
    >
      <Download className="h-4 w-4" />
      {label}
    </a>
  );
}

const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white dark:placeholder:text-slate-500";

const primaryButtonClass =
  "mt-1 w-full rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:scale-[1.01] hover:brightness-110 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60";
