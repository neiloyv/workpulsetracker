import { useLocale } from "../context/LocaleContext";
import { AppLocale } from "../i18n";

type LanguageSwitcherProps = {
  className?: string;
};

export function LanguageSwitcher({ className = "" }: LanguageSwitcherProps) {
  const { locale, setLocale, t } = useLocale();

  function renderOption(optionLocale: AppLocale, label: string) {
    const isActive = locale === optionLocale;
    return (
      <button
        type="button"
        onClick={() => setLocale(optionLocale)}
        className={`rounded-md px-2.5 py-1 text-xs font-semibold transition ${
          isActive
            ? "bg-white text-slate-900 shadow-sm dark:bg-white/15 dark:text-white"
            : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
        }`}
        aria-pressed={isActive}
      >
        {label}
      </button>
    );
  }

  return (
    <div
      className={`inline-flex items-center rounded-xl border border-slate-200 bg-slate-100 p-1 dark:border-white/10 dark:bg-white/5 ${className}`}
      role="group"
      aria-label={t("common.language.switcher")}
    >
      {renderOption("uk", t("common.language.ua"))}
      {renderOption("en", t("common.language.en"))}
    </div>
  );
}
