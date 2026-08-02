export const SESSION_EXPIRED_EVENT = "wpt:session-expired";

const API_BASE = "";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (!headers.has("Content-Type") && init?.body) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: "include",
    headers
  });

  if (response.status === 401) {
    if (!path.startsWith("/api/auth/")) {
      window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
    }
    throw new Error("UNAUTHORIZED");
  }

  if (!response.ok) {
    let message = "Не удалось выполнить запрос";
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

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export type OrganizationType = "COMPANY" | "INDIVIDUAL";
export type UserRole = "OWNER" | "MANAGER" | "WORKER";

export type Me = {
  id: number;
  email: string;
  displayName: string;
  role: UserRole | string;
  organizationType: OrganizationType;
  organizationId: number;
  organizationName: string;
  workerId: number | null;
  status: string;
};

export type Downloads = {
  windowsUrl: string;
  macosUrl: string;
  linuxUrl: string;
};

export type DepartmentNode = {
  id: number;
  name: string;
  isDefault?: boolean;
};

export type BranchNode = {
  id: number;
  name: string;
  isDefault?: boolean;
  departments: DepartmentNode[];
};

export type Structure = {
  branches: BranchNode[];
};

export type DashboardWorker = {
  id: number;
  displayName: string;
  email: string;
  departmentName: string | null;
  branchName: string | null;
  todaySeconds: number;
  weekSeconds: number;
  monthSeconds: number;
  yearSeconds: number;
  agentInstalled: boolean;
  agentVersion: string | null;
};

export type DashboardPeriod = "TODAY" | "WEEK" | "MONTH" | "YEAR";

export type AppUsage = {
  appName: string;
  seconds: number;
  idle: boolean;
  percent: number;
};

export type Worker = {
  id: number;
  displayName: string;
  email: string;
  branchId: number | null;
  branchName: string | null;
  departmentId: number | null;
  departmentName: string | null;
  status: string;
  agentInstalled: boolean;
  agentVersion: string | null;
  accessKeyPrefix: string | null;
  createdAt: string;
};

export type CreateWorkerResult = {
  id: number;
  displayName: string;
  email: string;
  branchId: number | null;
  departmentId: number | null;
  status: string;
  accessKeySent: boolean;
  createdAt: string;
};

export type AgentInfo = {
  workerId: number;
  displayName: string;
  email: string;
  status: string;
  accessKeyPrefix: string | null;
  agentInstalled: boolean;
  agentVersion: string | null;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  organizationType: OrganizationType;
  email: string;
  password: string;
  displayName: string;
  companyName?: string;
};

export type CreateWorkerPayload = {
  displayName: string;
  email: string;
  branchId?: number | null;
  departmentId?: number | null;
};

export type UpdateWorkerPayload = {
  displayName: string;
  email: string;
  branchId?: number | null;
  departmentId?: number | null;
  status?: string;
};

export type ListFilters = {
  search?: string;
  departmentId?: string;
  branchId?: string;
};

function buildQuery(params: Record<string, string | undefined>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      search.set(key, value);
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export const api = {
  login: (payload: LoginPayload) =>
    request<Me>("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),

  register: (payload: RegisterPayload) =>
    request<Me>("/api/auth/register", { method: "POST", body: JSON.stringify(payload) }),

  logout: () => request<void>("/api/logout", { method: "POST" }),

  getMe: () => request<Me>("/api/me"),

  getDownloads: () => request<Downloads>("/api/downloads"),

  getStructure: () => request<Structure>("/api/structure"),

  createBranch: (name: string) =>
    request<BranchNode>("/api/structure/branches", {
      method: "POST",
      body: JSON.stringify({ name })
    }),

  createDepartment: (branchId: number, name: string) =>
    request<DepartmentNode>("/api/structure/departments", {
      method: "POST",
      body: JSON.stringify({ branchId, name })
    }),

  getWorkers: (filters: ListFilters = {}) =>
    request<Worker[]>(`/api/workers${buildQuery(filters)}`),

  createWorker: (payload: CreateWorkerPayload) =>
    request<CreateWorkerResult>("/api/workers", {
      method: "POST",
      body: JSON.stringify(payload)
    }),

  updateWorker: (id: number, payload: UpdateWorkerPayload) =>
    request<Worker>(`/api/workers/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    }),

  getWorkerAccessKey: (id: number) =>
    request<{ accessKey: string }>(`/api/workers/${id}/access-key`),

  resendWorkerAccessKey: (id: number) =>
    request<void>(`/api/workers/${id}/resend-access-key`, { method: "POST" }),

  getAgentInfo: () => request<AgentInfo>("/api/agent"),

  getMyAccessKey: () => request<{ accessKey: string }>("/api/agent/access-key"),

  resendMyAccessKey: () => request<void>("/api/agent/resend-access-key", { method: "POST" }),

  getDashboard: (filters: ListFilters = {}) =>
    request<DashboardWorker[]>(`/api/dashboard${buildQuery(filters)}`),

  getWorkerApps: (workerId: number, period: DashboardPeriod = "TODAY") =>
    request<AppUsage[]>(`/api/dashboard/workers/${workerId}/apps${buildQuery({ period })}`),

  getOrganizationSettings: () => request<Record<string, string>>("/api/organization/settings"),

  updateOrganizationSettings: (settings: Record<string, string>) =>
    request<Record<string, string>>("/api/organization/settings", {
      method: "PUT",
      body: JSON.stringify({ settings })
    })
};
