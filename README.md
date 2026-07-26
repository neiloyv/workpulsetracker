# WorkPulseTracker

Монорепозиторий автоматического тайм-трекера (B2B SaaS прототип).

## Модули
| Модуль | Назначение |
|---|---|
| `tracker-agent` | Локальное десктоп-приложение (трекинг, UI, трей) |
| `tracker-common` | Общий код (i18n и др.) |
| `tracker-server` | Spring Boot API (email/password auth, org, dashboard) |
| `tracker-ui` | React + Vite + Tailwind веб-кабинет |
| `tracker-android` | Мобильное приложение (заготовка) |

## Документация
См. **[docs/](docs/README.md)**.

## Локальная БД
| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5435` |
| Database | `workpulsetracker` |
| Username | `workpulsetracker` |
| Password | `workpulsetracker` |

```powershell
docker compose down -v   # если схема менялась — чистый старт
docker compose up -d
```

## Быстрый старт
```powershell
# API
.\gradlew.bat :tracker-server:bootRun

# Web UI
cd tracker-ui
npm install
npm run dev
```

UI: http://localhost:5173  
API: http://localhost:8080

### Auth
Email + пароль (без Google). На лендинге: Вход / Регистрация (личный аккаунт или организация).

Владелец организации видит команду и сотрудников; обычный сотрудник / личный аккаунт — только свою активность.
