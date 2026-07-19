import { FormEvent, useEffect, useState } from "react";
import { api, CreateUserResult, Me, OrgStats, OrgUser } from "../api";
import { t } from "../i18n";

type Tab = "stats" | "users" | "settings";

type Props = {
  me: Me;
  onLogout: () => void;
};

export function OrganizationPage({ me, onLogout }: Props) {
  const [tab, setTab] = useState<Tab>("stats");
  const [users, setUsers] = useState<OrgUser[]>([]);
  const [stats, setStats] = useState<OrgStats | null>(null);
  const [settings, setSettings] = useState<Record<string, string>>({
    idleTimeoutSeconds: "60",
    timezone: "UTC"
  });
  const [createdKey, setCreatedKey] = useState<CreateUserResult | null>(null);
  const [email, setEmail] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [organizationName, setOrganizationName] = useState(me.organizationName ?? "");

  async function reload() {
    const [org, orgUsers, orgStats, orgSettings] = await Promise.all([
      api.getOrganization(),
      api.getUsers(),
      api.getStats(),
      api.getSettings()
    ]);
    setOrganizationName(org.name);
    setUsers(orgUsers);
    setStats(orgStats);
    setSettings({
      idleTimeoutSeconds: orgSettings.idleTimeoutSeconds ?? "60",
      timezone: orgSettings.timezone ?? "UTC",
      ...orgSettings
    });
  }

  useEffect(() => {
    reload().catch((err) => setError(err instanceof Error ? err.message : t("common.error")));
  }, []);

  async function onCreateUser(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const result = await api.createUser({ email, firstName, lastName });
      setCreatedKey(result);
      setEmail("");
      setFirstName("");
      setLastName("");
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : t("common.error"));
    }
  }

  async function onSaveSettings(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const saved = await api.updateSettings({
        idleTimeoutSeconds: settings.idleTimeoutSeconds ?? "60",
        timezone: settings.timezone ?? "UTC"
      });
      setSettings(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("common.error"));
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <div>
          <div className="muted">{t("org.title")}</div>
          <h1 style={{ fontFamily: "Fraunces, Georgia, serif", margin: "0.2rem 0 0" }}>
            {organizationName || t("ui.app.title")}
          </h1>
          <div className="muted">
            {me.firstName} {me.lastName} · {me.email}
          </div>
        </div>
        <button className="btn btn-secondary" onClick={onLogout}>
          {t("common.logout")}
        </button>
      </div>

      <div className="tabs">
        {([
          ["stats", "org.tab.stats"],
          ["users", "org.tab.users"],
          ["settings", "org.tab.settings"]
        ] as const).map(([id, labelKey]) => (
          <button
            key={id}
            className={`tab ${tab === id ? "active" : ""}`}
            onClick={() => setTab(id)}
          >
            {t(labelKey)}
          </button>
        ))}
      </div>

      {error && <div className="alert" style={{ marginBottom: "1rem" }}>{error}</div>}

      {tab === "stats" && (
        <div className="card">
          <p className="muted">{t("org.stats.empty")}</p>
          <p>
            <strong>{stats?.totalUsers ?? 0}</strong> users ·{" "}
            <strong>{stats?.activeUsersWithAgentKey ?? 0}</strong> with agent key
          </p>
          <table className="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Tracked</th>
              </tr>
            </thead>
            <tbody>
              {(stats?.users ?? []).map((user) => (
                <tr key={user.email}>
                  <td>{user.fullName}</td>
                  <td>{user.email}</td>
                  <td>{user.trackedSeconds}s</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === "users" && (
        <div style={{ display: "grid", gap: "1rem" }}>
          <div className="card">
            <h3 style={{ marginTop: 0 }}>{t("org.users.add")}</h3>
            <form className="form-grid" onSubmit={onCreateUser}>
              <label>
                {t("org.users.email")}
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </label>
              <label>
                {t("org.users.firstName")}
                <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
              </label>
              <label>
                {t("org.users.lastName")}
                <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
              </label>
              <button className="btn btn-primary" type="submit">
                {t("org.users.add")}
              </button>
            </form>
            {createdKey && (
              <div className="alert" style={{ marginTop: "1rem" }}>
                <div>{t("org.users.keyOnce")}</div>
                <strong>{createdKey.agentKey}</strong>
              </div>
            )}
          </div>

          <div className="card">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Agent key</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 && (
                  <tr>
                    <td colSpan={4}>{t("org.users.empty")}</td>
                  </tr>
                )}
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>
                      {user.firstName} {user.lastName}
                    </td>
                    <td>{user.email}</td>
                    <td>{user.role}</td>
                    <td>{user.agentKeyPrefix ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === "settings" && (
        <div className="card">
          <form className="form-grid" onSubmit={onSaveSettings}>
            <label>
              {t("org.settings.idle")}
              <input
                value={settings.idleTimeoutSeconds ?? "60"}
                onChange={(e) =>
                  setSettings((prev) => ({ ...prev, idleTimeoutSeconds: e.target.value }))
                }
              />
            </label>
            <label>
              {t("org.settings.timezone")}
              <input
                value={settings.timezone ?? "UTC"}
                onChange={(e) => setSettings((prev) => ({ ...prev, timezone: e.target.value }))}
              />
            </label>
            <button className="btn btn-primary" type="submit">
              {t("org.settings.save")}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
