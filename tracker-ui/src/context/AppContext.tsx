import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, Me, Structure } from "../api";

export type ViewMode = "OWNER" | "EMPLOYEE";

export const ALL_BRANCHES = "ALL";

type AppContextValue = {
  me: Me | null;
  setMe: (me: Me | null) => void;
  loading: boolean;
  refreshMe: () => Promise<Me | null>;
  logout: () => Promise<void>;
  viewMode: ViewMode;
  setViewMode: (mode: ViewMode) => void;
  selectedBranchId: string;
  setSelectedBranchId: (branchId: string) => void;
  structure: Structure | null;
  refreshStructure: () => Promise<void>;
};

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>("OWNER");
  const [selectedBranchId, setSelectedBranchId] = useState<string>(ALL_BRANCHES);
  const [structure, setStructure] = useState<Structure | null>(null);

  const refreshMe = useCallback(async () => {
    try {
      const current = await api.getMe();
      setMe(current);
      return current;
    } catch {
      setMe(null);
      return null;
    }
  }, []);

  const refreshStructure = useCallback(async () => {
    try {
      const current = await api.getStructure();
      setStructure(current);
    } catch {
      setStructure(null);
    }
  }, []);

  useEffect(() => {
    refreshMe().finally(() => setLoading(false));
  }, [refreshMe]);

  useEffect(() => {
    if (me?.accountType === "ORGANIZATION") {
      refreshStructure();
    } else {
      setStructure(null);
    }
  }, [me?.accountType, me?.organizationId, refreshStructure]);

  useEffect(() => {
    if (me) {
      setViewMode(me.role === "OWNER" ? "OWNER" : "EMPLOYEE");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [me?.id]);

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // ignore network errors on logout
    }
    setMe(null);
    setSelectedBranchId(ALL_BRANCHES);
    setViewMode("OWNER");
  }, []);

  const value = useMemo<AppContextValue>(
    () => ({
      me,
      setMe,
      loading,
      refreshMe,
      logout,
      viewMode,
      setViewMode,
      selectedBranchId,
      setSelectedBranchId,
      structure,
      refreshStructure
    }),
    [me, loading, refreshMe, logout, viewMode, selectedBranchId, structure, refreshStructure]
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp(): AppContextValue {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error("useApp must be used within AppProvider");
  }
  return context;
}
