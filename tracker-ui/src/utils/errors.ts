export function mapApiError(message: string, fallback = "Не удалось выполнить запрос"): string {
  const normalized = message.trim().toLowerCase();
  if (normalized === "unauthorized") {
    const fallbackLower = fallback.toLowerCase();
    if (fallbackLower.includes("войти") || fallbackLower.includes("регистр")) {
      return "Неверный email или пароль";
    }
    return "Сессия истекла. Войдите снова";
  }
  if (normalized.includes("already exists")) {
    return "Пользователь с таким email уже существует";
  }
  if (normalized.includes("companyname is required")) {
    return "Укажите название компании";
  }
  if (normalized.includes("only organization owner")) {
    return "Это действие доступно только владельцу организации";
  }
  if (normalized.includes("complete onboarding") || normalized.includes("organization not found")) {
    return "Организация недоступна";
  }
  if (normalized.includes("invalid credentials")) {
    return "Неверный email или пароль";
  }
  if (normalized.includes("must not be blank") || normalized.includes("size must be")) {
    return "Проверьте корректность заполненных полей";
  }
  if (normalized.includes("cannot view activity")) {
    return "Недостаточно прав для просмотра этой активности";
  }
  if (message.startsWith("email:") || message.startsWith("password:") || message.startsWith("displayName:")) {
    return "Проверьте корректность заполненных полей";
  }
  // Avoid leaking raw English/technical backend text when possible
  if (/^[A-Za-z]/.test(message) && !/[а-яА-ЯёЁ]/.test(message)) {
    return fallback;
  }
  return message || fallback;
}
