import en from "../i18n/en.json";
import uk from "../i18n/uk.json";

export type Language = "en" | "uk";

const dictionaries: Record<Language, Record<string, string>> = {
  en: en as Record<string, string>,
  uk: uk as Record<string, string>
};

let currentLanguage: Language = (localStorage.getItem("app.language") as Language) || "en";

export function getLanguage(): Language {
  return currentLanguage;
}

export function setLanguage(language: Language) {
  currentLanguage = language;
  localStorage.setItem("app.language", language);
}

export function t(key: string, fallback?: string): string {
  return dictionaries[currentLanguage][key] ?? dictionaries.en[key] ?? fallback ?? key;
}
