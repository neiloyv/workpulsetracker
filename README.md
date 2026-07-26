# WorkPulseTracker

Монорепозиторий автоматического тайм-трекера.

## Модули
| Модуль | Назначение |
|---|---|
| `tracker-agent` | Локальное десктоп-приложение (трекинг, UI, трей) |
| `tracker-common` | Общий код (i18n и др.) |
| `tracker-server` | Spring Boot API (Hibernate + Liquibase + Google OAuth) |
| `tracker-ui` | React + Vite веб-кабинет |
| `tracker-android` | Мобильное приложение (заготовка) |

## Документация
См. **[docs/](docs/README.md)**.

## Локальная БД
| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `workpulsetracker` |
| Username | `workpulsetracker` |
| Password | `workpulsetracker_dev_password` |

```powershell
docker compose up -d
# or use local PostgreSQL with the same credentials
```

## Быстрый старт
```powershell
# Agent
.\gradlew.bat :tracker-agent:run

# API
.\gradlew.bat :tracker-server:bootRun

# Web UI (requires Node.js)
cd tracker-ui
npm install
npm run dev
```
