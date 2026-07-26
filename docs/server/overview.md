# tracker-server — overview

Spring Boot API for the web product.

## Stack
- PostgreSQL (local)
- **Hibernate / Spring Data JPA**
- **Liquibase** migrations (`db/changelog/`)
- Spring Security session cookie (`JSESSIONID`)
- Email + password auth (BCrypt)

## Database connection (local)

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `workpulsetracker` |
| Username | `workpulsetracker` |
| Password | `workpulsetracker_dev_password` |
| JDBC | `jdbc:postgresql://localhost:5432/workpulsetracker` |

Same values are in [`.env.example`](../../.env.example).

### Start DB
```powershell
docker compose up -d
```

После смены схемы (rename / init changelog) предпочтительно:
```powershell
docker compose down -v
docker compose up -d
```

## Run API
```powershell
cd D:\Projects\workpulsetracker
.\gradlew.bat :tracker-server:bootRun
```

## Auth
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | no | PERSONAL / ORGANIZATION + session |
| POST | `/api/auth/login` | no | email/password + session |
| POST | `/api/logout` | yes | Invalidate session |
| GET | `/api/me` | yes | Current user |

## Main API
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/downloads` | Download links |
| GET/POST | `/api/structure*` | Branches & departments |
| GET/POST/PUT | `/api/employees*` | Staff registry |
| GET | `/api/dashboard` | Worker hours (owner: team, member: self) |
| GET | `/api/dashboard/users/{id}/apps` | App breakdown (+ Idle), owner or self |
| GET/PUT | `/api/organization/settings` | Org settings map |

## Schema tables
`organization`, `branch`, `department`, `app_user`, `agent_key`, `organization_setting`, `activity_sample`

## PlantUML
See [`tracker-server/docs/puml/`](../../tracker-server/docs/puml/) — diagrams may be outdated vs email auth.
