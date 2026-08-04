import en from "./en.json";
import uk from "./uk.json";

export type AppLocale = "en" | "uk";

export type TranslationDictionary = typeof en;

export type TranslationKey = keyof TranslationDictionary;

export const LOCALES: AppLocale[] = ["uk", "en"];

export const dictionaries: Record<AppLocale, TranslationDictionary> = {
  en,
  uk
};

export function detectBrowserLocale(): AppLocale {
  const candidates = [navigator.language, ...(navigator.languages ?? [])];
  const hasUkrainian = candidates.some((localeTag) => {
    const normalized = localeTag.trim().toLowerCase();
    return normalized === "uk" || normalized.startsWith("uk-");
  });
  return hasUkrainian ? "uk" : "en";
}

export function formatMessage(
  template: string,
  params?: Record<string, string | number>
): string {
  if (!params) {
    return template;
  }
  return Object.entries(params).reduce(
    (result, [paramName, paramValue]) =>
      result.replaceAll(`{${paramName}}`, String(paramValue)),
    template
  );
}
