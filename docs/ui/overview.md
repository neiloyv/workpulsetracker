# tracker-ui — overview

React + Vite + Tailwind web client (WorkPulseTracker prototype).

## Requirements
- Node.js 20+ and npm

## Run
```powershell
cd D:\Projects\workpulsetracker\tracker-ui
npm install
npm run dev
```
UI: `http://localhost:5173`  
API proxy → `http://localhost:8080`

## Stack
- React 19, React Router 7
- Tailwind CSS 3, Lucide icons, Recharts
- Dark mode default (slate/navy + indigo accents), light toggle
- Fonts: Sora + Manrope

## Pages
| Route | Purpose |
|---|---|
| `/` | Landing + auth card (Вход / Регистрация, личный / организация) |
| `/app` | Dashboard analytics |
| `/app/employees` | Staff registry (org + owner view only) |

## App chrome
- Theme switcher
- Branch selector (organization owner only)
- Employees tab + company structure (organization owner only)
- Avatar menu: stubs for billing/managers, logout

## Export
CSV download for filtered dashboard / personal app report (Excel/PDF — next iteration).
