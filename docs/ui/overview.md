# tracker-ui — overview

React + Vite web client.

## Requirements
- Node.js 20+ and npm

## Run
```powershell
cd D:\Projects\timetracker\tracker-ui
npm install
npm run dev
```
UI: `http://localhost:5173`  
API proxy → `http://localhost:8080`

## Pages
| Route | Purpose |
|---|---|
| `/` | Landing: brand, Google login, download buttons (Win/macOS/Linux) |
| `/auth/callback` | After Google OAuth, loads `/api/me`, routes to onboarding or org |
| `/onboarding` | First-time: company name, first name, last name |
| `/org` | Organization tabs: Statistics / Users / Settings |

## Users tab
- Add user → API creates member + agent key
- Key shown once in UI alert; later only prefix is visible in the table

## Settings tab
- `idleTimeoutSeconds`, `timezone` (stored in `organization_setting`)

## i18n
- Files: `tracker-ui/i18n/en.json`, `uk.json`
- Default language: English (`localStorage app.language`)

## PlantUML
See [`tracker-ui/docs/puml/`](../../tracker-ui/docs/puml/)
