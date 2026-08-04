import {
  Activity,
  Building2,
  ChevronDown,
  CreditCard,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Moon,
  ShieldCheck,
  Sun,
  User,
  Users
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ALL_BRANCHES, useApp } from "../context/AppContext";
import { useLocale } from "../context/LocaleContext";
import { useTheme } from "../context/ThemeContext";
import { initials } from "../utils/format";
import { toast } from "../utils/toast";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { StructureModal } from "./StructureModal";
import { ToastHost } from "./ToastHost";

export function AppShell() {
  const {
    me,
    logout,
    canManageCompany,
    isIndividual,
    selectedBranchId,
    setSelectedBranchId,
    structure
  } = useApp();
  const { theme, toggleTheme } = useTheme();
  const { t } = useLocale();
  const location = useLocation();
  const navigate = useNavigate();

  const [avatarOpen, setAvatarOpen] = useState(false);
  const [branchMenuOpen, setBranchMenuOpen] = useState(false);
  const [structureOpen, setStructureOpen] = useState(false);
  const avatarRef = useRef<HTMLDivElement>(null);
  const branchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (avatarRef.current && !avatarRef.current.contains(event.target as Node)) {
        setAvatarOpen(false);
      }
      if (branchRef.current && !branchRef.current.contains(event.target as Node)) {
        setBranchMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  if (!me) {
    return null;
  }

  const showCompanyNav = canManageCompany;
  const showAgentTab = Boolean(me.workerId);
  const showBranchSelector = canManageCompany && Boolean(structure);
  const activeBranch = structure?.branches.find((branch) => String(branch.id) === selectedBranchId);

  async function onLogout() {
    await logout();
    navigate("/");
  }

  return (
    <div className={`min-h-screen w-full ${theme === "dark" ? "app-bg-dark" : "app-bg-light"}`}>
      <header className="sticky top-0 z-30 border-b border-slate-200/70 bg-white/80 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/70">
        <div className="mx-auto flex h-16 max-w-[1600px] items-center gap-4 px-5">
          <Link to="/app" className="flex items-center gap-2 font-display text-lg font-bold text-slate-900 dark:text-white">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-brand-600 to-sky-500 text-white">
              <Activity className="h-4 w-4" />
            </span>
            WorkPulseTracker
          </Link>

          <nav className="ml-4 hidden items-center gap-1 rounded-xl bg-slate-100 p-1 dark:bg-white/5 md:flex">
            <NavTab
              to="/app"
              icon={LayoutDashboard}
              label={t("nav.dashboard")}
              active={location.pathname === "/app"}
            />
            {showCompanyNav && (
              <NavTab
                to="/app/workers"
                icon={Users}
                label={t("nav.workers")}
                active={location.pathname.startsWith("/app/workers")}
              />
            )}
            {showCompanyNav && (
              <NavTab
                to="/app/managers"
                icon={ShieldCheck}
                label={t("nav.managers")}
                active={location.pathname.startsWith("/app/managers")}
              />
            )}
            {showAgentTab && (
              <NavTab
                to="/app/agent"
                icon={KeyRound}
                label={t("nav.agent")}
                active={location.pathname.startsWith("/app/agent")}
              />
            )}
          </nav>

          <div className="flex-1" />

          {showBranchSelector && structure && (
            <div className="relative hidden sm:block" ref={branchRef}>
              <button
                onClick={() => setBranchMenuOpen((prev) => !prev)}
                className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
              >
                <Building2 className="h-4 w-4" />
                {activeBranch ? activeBranch.name : t("nav.allBranches")}
                <ChevronDown className="h-3.5 w-3.5" />
              </button>
              {branchMenuOpen && (
                <div className="absolute right-0 z-40 mt-2 w-56 animate-fade-in-scale overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-xl dark:border-white/10 dark:bg-slate-900">
                  <MenuItem
                    label={t("nav.allBranches")}
                    active={selectedBranchId === ALL_BRANCHES}
                    onClick={() => {
                      setSelectedBranchId(ALL_BRANCHES);
                      setBranchMenuOpen(false);
                    }}
                  />
                  {structure.branches.map((branch) => (
                    <MenuItem
                      key={branch.id}
                      label={branch.name}
                      active={selectedBranchId === String(branch.id)}
                      onClick={() => {
                        setSelectedBranchId(String(branch.id));
                        setBranchMenuOpen(false);
                      }}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          <LanguageSwitcher />

          <button
            onClick={toggleTheme}
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-600 transition hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
            aria-label={t("common.themeToggle")}
          >
            {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          <div className="relative" ref={avatarRef}>
            <button
              onClick={() => setAvatarOpen((prev) => !prev)}
              className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-sky-500 text-xs font-bold text-white transition hover:scale-105"
            >
              {initials(me.displayName || me.email)}
            </button>
            {avatarOpen && (
              <div className="absolute right-0 z-40 mt-2 w-64 animate-fade-in-scale overflow-hidden rounded-xl border border-slate-200 bg-white py-1.5 shadow-xl dark:border-white/10 dark:bg-slate-900">
                <div className="border-b border-slate-100 px-3.5 py-2.5 dark:border-white/10">
                  <div className="truncate text-sm font-semibold text-slate-800 dark:text-white">
                    {me.displayName}
                  </div>
                  <div className="truncate text-xs text-slate-400">{me.email}</div>
                  <div className="mt-1 text-xs text-slate-400">
                    {me.organizationName} · {isIndividual ? t("nav.org.personal") : t("nav.org.company")} ·{" "}
                    {me.role}
                  </div>
                </div>
                <AvatarMenuItem
                  icon={User}
                  label={t("nav.profile")}
                  onClick={() => {
                    setAvatarOpen(false);
                    window.alert(
                      `${me.displayName}\n${me.email}\n${t("nav.profileRole", { role: me.role })}`
                    );
                  }}
                />
                {canManageCompany && (
                  <AvatarMenuItem
                    icon={Building2}
                    label={t("nav.companyStructure")}
                    onClick={() => {
                      setAvatarOpen(false);
                      setStructureOpen(true);
                    }}
                  />
                )}
                <AvatarMenuItem
                  icon={CreditCard}
                  label={t("nav.billing")}
                  onClick={() => {
                    setAvatarOpen(false);
                    toast(t("nav.billingSoon"), "info");
                  }}
                />
                <div className="my-1 border-t border-slate-100 dark:border-white/10" />
                <AvatarMenuItem icon={LogOut} label={t("nav.logout")} danger onClick={onLogout} />
              </div>
            )}
          </div>
        </div>

        <nav className="flex items-center gap-1 overflow-x-auto border-t border-slate-200/70 px-5 py-2 dark:border-white/10 md:hidden">
          <NavTab
            to="/app"
            icon={LayoutDashboard}
            label={t("nav.dashboard")}
            active={location.pathname === "/app"}
          />
          {showCompanyNav && (
            <NavTab
              to="/app/workers"
              icon={Users}
              label={t("nav.workers")}
              active={location.pathname.startsWith("/app/workers")}
            />
          )}
          {showCompanyNav && (
            <NavTab
              to="/app/managers"
              icon={ShieldCheck}
              label={t("nav.managers")}
              active={location.pathname.startsWith("/app/managers")}
            />
          )}
          {showAgentTab && (
            <NavTab
              to="/app/agent"
              icon={KeyRound}
              label={t("nav.agent")}
              active={location.pathname.startsWith("/app/agent")}
            />
          )}
        </nav>
      </header>

      <main className="mx-auto max-w-[1600px] px-5 py-8">
        <Outlet />
      </main>

      {canManageCompany && <StructureModal open={structureOpen} onClose={() => setStructureOpen(false)} />}
      <ToastHost />
    </div>
  );
}

function NavTab({
  to,
  icon: Icon,
  label,
  active
}: {
  to: string;
  icon: typeof LayoutDashboard;
  label: string;
  active: boolean;
}) {
  return (
    <Link
      to={to}
      className={`flex items-center gap-1.5 whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-semibold transition ${
        active
          ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
          : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
      }`}
    >
      <Icon className="h-4 w-4" />
      {label}
    </Link>
  );
}

function MenuItem({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center px-3.5 py-2 text-left text-sm transition ${
        active
          ? "bg-brand-50 font-semibold text-brand-700 dark:bg-brand-500/10 dark:text-brand-300"
          : "text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-white/5"
      }`}
    >
      {label}
    </button>
  );
}

function AvatarMenuItem({
  icon: Icon,
  label,
  onClick,
  danger
}: {
  icon: typeof User;
  label: string;
  onClick: () => void;
  danger?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center gap-2.5 px-3.5 py-2 text-left text-sm transition ${
        danger
          ? "text-rose-500 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-500/10"
          : "text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-white/5"
      }`}
    >
      <Icon className="h-4 w-4" />
      {label}
    </button>
  );
}
