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

export type AccountType = "PERSONAL" | "ORGANIZATION";
export type UserRole = "OWNER" | "MEMBER";

export type Me = {
  id: string;
  email: string;
  displayName: string;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  role: UserRole | string;
  accountType: AccountType;
  onboarded: boolean;
  organizationId: string | null;
  organizationName: string | null;
  branchId: string | null;
  departmentId: string | null;
  agentInstalled: boolean;
  agentVersion: string | null;
};

export type Downloads = {
  windowsUrl: string;
  macosUrl: string;
  linuxUrl: string;
};

export type DepartmentNode = {
  id: string;
  name: string;
};

export type BranchNode = {
  id: string;
  name: string;
  departments: DepartmentNode[];
};

export type Structure = {
  branches: BranchNode[];
};

export type DashboardWorker = {
  id: string;
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

export type Employee = {
  id: string;
  displayName: string;
  email: string;
  phone: string | null;
  role: string;
  branchId: string | null;
  branchName: string | null;
  departmentId: string | null;
  departmentName: string | null;
  agentInstalled: boolean;
  agentVersion: string | null;
  agentKeyPrefix: string | null;
  createdAt: string;
};

export type CreateEmployeeResult = {
  id: string;
  displayName: string;
  email: string;
  phone: string | null;
  role: string;
  branchId: string | null;
  departmentId: string | null;
  agentKey: string | null;
  agentKeyPrefix: string | null;
  temporaryPassword: string | null;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  accountType: AccountType;
  email: string;
  password: string;
  displayName: string;
  companyName?: string;
};

export type CreateEmployeePayload = {
  displayName: string;
  email: string;
  phone?: string;
  branchId?: string | null;
  departmentId?: string | null;
  password?: string;
};

export type UpdateEmployeePayload = {
  displayName: string;
  email: string;
  phone?: string;
  branchId?: string | null;
  departmentId?: string | null;
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

  createDepartment: (branchId: string, name: string) =>
    request<DepartmentNode>("/api/structure/departments", {
      method: "POST",
      body: JSON.stringify({ branchId, name })
    }),

  getEmployees: (filters: ListFilters = {}) =>
    request<Employee[]>(`/api/employees${buildQuery(filters)}`),

  createEmployee: (payload: CreateEmployeePayload) =>
    request<CreateEmployeeResult>("/api/employees", {
      method: "POST",
      body: JSON.stringify(payload)
    }),

  updateEmployee: (id: string, payload: UpdateEmployeePayload) =>
    request<Employee>(`/api/employees/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    }),

  getDashboard: (filters: ListFilters = {}) =>
    request<DashboardWorker[]>(`/api/dashboard${buildQuery(filters)}`),

  getUserApps: (userId: string, period: DashboardPeriod = "TODAY") =>
    request<AppUsage[]>(`/api/dashboard/users/${userId}/apps${buildQuery({ period })}`),

  getOrganizationSettings: () => request<Record<string, string>>("/api/organization/settings"),

  updateOrganizationSettings: (settings: Record<string, string>) =>
    request<Record<string, string>>("/api/organization/settings", {
      method: "PUT",
      body: JSON.stringify({ settings })
    })
};
