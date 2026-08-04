import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import {
  AppLocale,
  detectBrowserLocale,
  dictionaries,
  formatMessage,
  TranslationKey
} from "../i18n";

const STORAGE_KEY = "wpt.locale";

type LocaleContextValue = {
  locale: AppLocale;
  setLocale: (locale: AppLocale) => void;
  t: (key: TranslationKey, params?: Record<string, string | number>) => string;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

function readInitialLocale(): AppLocale {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "en" || stored === "uk") {
    return stored;
  }
  return detectBrowserLocale();
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<AppLocale>(readInitialLocale);

  useEffect(() => {
    document.documentElement.lang = locale;
    localStorage.setItem(STORAGE_KEY, locale);
  }, [locale]);

  const value = useMemo<LocaleContextValue>(
    () => ({
      locale,
      setLocale: setLocaleState,
      t: (key, params) => {
        const dictionary = dictionaries[locale] ?? dictionaries.en;
        const template = dictionary[key] ?? dictionaries.en[key] ?? key;
        return formatMessage(template, params);
      }
    }),
    [locale]
  );

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextValue {
  const context = useContext(LocaleContext);
  if (!context) {
    throw new Error("useLocale must be used within LocaleProvider");
  }
  return context;
}
