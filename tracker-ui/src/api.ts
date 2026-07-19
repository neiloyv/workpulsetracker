const API_BASE = "";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (response.status === 401) {
    throw new Error("UNAUTHORIZED");
  }

  if (!response.ok) {
    let message = "Request failed";
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export type Me = {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: string;
  onboarded: boolean;
  organizationId: string | null;
  organizationName: string | null;
};

export type Downloads = {
  windowsUrl: string;
  macosUrl: string;
  linuxUrl: string;
};

export type OrgUser = {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: string;
  onboarded: boolean;
  agentKeyPrefix: string | null;
  createdAt: string;
};

export type CreateUserResult = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  agentKey: string;
  agentKeyPrefix: string;
};

export type OrgStats = {
  totalUsers: number;
  activeUsersWithAgentKey: number;
  users: Array<{
    email: string;
    fullName: string;
    trackedSeconds: number;
  }>;
};

export const api = {
  getMe: () => request<Me>("/api/me"),
  getDownloads: () => request<Downloads>("/api/downloads"),
  completeOnboarding: (body: { companyName: string; firstName: string; lastName: string }) =>
    request<Me>("/api/onboarding", { method: "POST", body: JSON.stringify(body) }),
  getOrganization: () => request<{ id: string; name: string }>("/api/organization"),
  getUsers: () => request<OrgUser[]>("/api/organization/users"),
  createUser: (body: { email: string; firstName: string; lastName: string }) =>
    request<CreateUserResult>("/api/organization/users", {
      method: "POST",
      body: JSON.stringify(body)
    }),
  getSettings: () => request<Record<string, string>>("/api/organization/settings"),
  updateSettings: (settings: Record<string, string>) =>
    request<Record<string, string>>("/api/organization/settings", {
      method: "PUT",
      body: JSON.stringify({ settings })
    }),
  getStats: () => request<OrgStats>("/api/organization/stats"),
  logout: () => request<void>("/api/logout", { method: "POST" })
};

export function startGoogleLogin() {
  window.location.href = "/oauth2/authorization/google";
}
