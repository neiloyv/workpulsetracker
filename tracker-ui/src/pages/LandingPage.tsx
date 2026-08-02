import { BarChart3, Building2, Moon, Shield, Sun, User, Zap } from "lucide-react";
import { FormEvent, ReactNode, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { useApp } from "../context/AppContext";
import { useTheme } from "../context/ThemeContext";
import { mapApiError } from "../utils/errors";

type AuthTab = "login" | "register";
type RegisterKind = "INDIVIDUAL" | "COMPANY";

const FEATURES = [
  {
    icon: Shield,
    title: "100% Приватно",
    description: "Никаких скриншотов и записи экрана — только активные приложения и рабочие часы."
  },
  {
    icon: Zap,
    title: "Автоматический учет софта",
    description: "Легкий Windows-агент фиксирует реальную активность без ручного трекинга."
  },
  {
    icon: BarChart3,
    title: "Наглядные отчеты",
    description: "Выгрузка в Excel / PDF в один клик и понятная аналитика по команде."
  }
];

export function LandingPage() {
  const navigate = useNavigate();
  const { me, setMe } = useApp();
  const { theme, toggleTheme } = useTheme();
  const [tab, setTab] = useState<AuthTab>("login");
  const [registerKind, setRegisterKind] = useState<RegisterKind>("INDIVIDUAL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  async function onLogin(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const result = await api.login({ email: loginEmail, password: loginPassword });
      setMe(result);
      navigate("/app");
    } catch (err) {
      setError(mapApiError(err instanceof Error ? err.message : "", "Не удалось войти"));
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
      setError(mapApiError(err instanceof Error ? err.message : "", "Не удалось зарегистрироваться"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={`min-h-screen w-full ${theme === "dark" ? "app-bg-dark" : "app-bg-light"}`}>
      <button
        onClick={toggleTheme}
        className="fixed right-5 top-5 z-20 flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white/80 text-slate-600 shadow-sm backdrop-blur transition hover:scale-105 hover:text-brand-600 dark:border-white/10 dark:bg-white/5 dark:text-slate-300 dark:hover:text-brand-300"
        aria-label="Переключить тему"
      >
        {theme === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
      </button>

      <div className="mx-auto grid min-h-screen w-full max-w-7xl grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-2 lg:gap-8 lg:px-10">
        <div className="animate-fade-in">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-brand-500/30 bg-brand-500/10 px-3.5 py-1.5 text-xs font-semibold uppercase tracking-wider text-brand-600 dark:text-brand-300">
            <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
            WorkPulseTracker
          </div>
          <h1 className="font-display text-4xl font-bold leading-[1.08] tracking-tight text-slate-900 dark:text-white sm:text-5xl lg:text-[3.4rem]">
            Прозрачный учет времени.
            <br />
            <span className="bg-gradient-to-r from-brand-600 via-brand-500 to-sky-500 bg-clip-text text-transparent dark:from-brand-400 dark:via-brand-300 dark:to-sky-300">
              Без шпионажа и скриншотов.
            </span>
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-slate-600 dark:text-slate-300">
            WorkPulseTracker фиксирует только активные приложения и реальные рабочие часы. Полное доверие
            в команде без контроля личного пространства.
          </p>

          <div className="mt-10 grid gap-5 sm:grid-cols-1">
            {FEATURES.map((feature) => (
              <div key={feature.title} className="flex items-start gap-4">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-brand-600 shadow-sm dark:border-white/10 dark:bg-white/5 dark:text-brand-300">
                  <feature.icon className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-display text-base font-semibold text-slate-900 dark:text-white">
                    {feature.title}
                  </div>
                  <div className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">{feature.description}</div>
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
                Вход
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
                Регистрация
              </button>
            </div>

            {tab === "login" ? (
              <form className="grid gap-4" onSubmit={onLogin}>
                <Field label="Email">
                  <input
                    type="email"
                    required
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    placeholder="you@company.com"
                    className={inputClass}
                  />
                </Field>
                <Field label="Пароль">
                  <input
                    type="password"
                    required
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    placeholder="••••••••"
                    className={inputClass}
                  />
                </Field>
                {error && <ErrorBanner message={error} />}
                <button type="submit" disabled={loading} className={primaryButtonClass}>
                  {loading ? "Входим..." : "Войти"}
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
                    Для себя
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
                    Организация
                  </button>
                </div>

                {registerKind === "COMPANY" && (
                  <Field label="Название компании">
                    <input
                      required
                      value={companyName}
                      onChange={(e) => setCompanyName(e.target.value)}
                      placeholder="ООО «Компания»"
                      className={inputClass}
                    />
                  </Field>
                )}
                <Field label={registerKind === "COMPANY" ? "Имя владельца" : "Ваше имя"}>
                  <input
                    required
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Иван Иванов"
                    className={inputClass}
                  />
                </Field>
                <Field label="Email">
                  <input
                    type="email"
                    required
                    value={registerEmail}
                    onChange={(e) => setRegisterEmail(e.target.value)}
                    placeholder="you@company.com"
                    className={inputClass}
                  />
                </Field>
                <Field label="Пароль">
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={registerPassword}
                    onChange={(e) => setRegisterPassword(e.target.value)}
                    placeholder="Минимум 8 символов"
                    className={inputClass}
                  />
                </Field>
                {error && <ErrorBanner message={error} />}
                <button type="submit" disabled={loading} className={primaryButtonClass}>
                  {loading ? "Создаем аккаунт..." : "Создать аккаунт"}
                </button>
              </form>
            )}
          </div>
          <p className="mt-5 text-center text-xs text-slate-400 dark:text-slate-500">
            Продолжая, вы соглашаетесь с условиями использования WorkPulseTracker.
          </p>
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

const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500";

const primaryButtonClass =
  "mt-1 w-full rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:scale-[1.01] hover:brightness-110 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60";
