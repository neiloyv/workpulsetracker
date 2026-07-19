# tracker-server — overview

Spring Boot API for the web product.

## Stack
- PostgreSQL (local)
- **Hibernate / Spring Data JPA**
- **Liquibase** migrations (`db/changelog/`)
- Spring Security + Google OAuth2 Login
- Session cookie for browser (`JSESSIONID`)

## Database connection (local)

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `timetracker` |
| Username | `timetracker` |
| Password | `timetracker_dev_password` |
| JDBC | `jdbc:postgresql://localhost:5432/timetracker` |

Same values are in [`.env.example`](../../.env.example).

### Start DB
- Preferred: `docker compose up -d` (when Docker Desktop is running)
- Or local PostgreSQL with the same user/db/password (already created on this machine if setup ran)

## Run API
```powershell
cd D:\Projects\timetracker
.\gradlew.bat :tracker-server:bootRun
```

Put Google OAuth credentials into `.env` / environment:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Google Cloud Console (Web client):
- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
- Authorized JS origin: `http://localhost:5173`

## Main API
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/downloads` | no | Download links for landing |
| GET | `/api/me` | yes | Current user + onboarding flag |
| POST | `/api/onboarding` | yes | Company + first/last name → create org as OWNER |
| GET | `/api/organization` | yes | Org info |
| GET/POST | `/api/organization/users` | yes | List / add users (add returns plaintext agent key once) |
| GET/PUT | `/api/organization/settings` | yes | Org settings map |
| GET | `/api/organization/stats` | yes | Stats stub (trackedSeconds = 0 until agent sync) |
| POST | `/api/logout` | yes | Logout |

Login entry: `/oauth2/authorization/google` → redirect to UI `/auth/callback`.

## Agent key
- Generated as `tt_<hex>`
- Stored as SHA-256 hash + short prefix
- Plaintext returned only in `POST /api/organization/users` response (and owner also gets a key during onboarding, stored hashed)

## PlantUML
See [`tracker-server/docs/puml/`](../../tracker-server/docs/puml/)
