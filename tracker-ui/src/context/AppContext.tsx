import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, Me, SESSION_EXPIRED_EVENT, Structure } from "../api";
import { toast } from "../utils/toast";

export const ALL_BRANCHES = "ALL";

type AppContextValue = {
  me: Me | null;
  setMe: (me: Me | null) => void;
  loading: boolean;
  isOwner: boolean;
  refreshMe: () => Promise<Me | null>;
  logout: () => Promise<void>;
  selectedBranchId: string;
  setSelectedBranchId: (branchId: string) => void;
  structure: Structure | null;
  refreshStructure: () => Promise<void>;
};

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedBranchId, setSelectedBranchId] = useState<string>(ALL_BRANCHES);
  const [structure, setStructure] = useState<Structure | null>(null);

  const refreshMe = useCallback(async () => {
    try {
      const current = await api.getMe();
      setMe(current);
      return current;
    } catch (error) {
      if (error instanceof Error && error.message === "UNAUTHORIZED") {
        setMe(null);
        return null;
      }
      // Network/server errors should not silently look like logged-out state
      throw error;
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
    refreshMe()
      .catch(() => {
        setMe(null);
      })
      .finally(() => setLoading(false));
  }, [refreshMe]);

  useEffect(() => {
    function onSessionExpired() {
      setMe(null);
      setStructure(null);
      setSelectedBranchId(ALL_BRANCHES);
      toast("Сессия истекла. Войдите снова", "info");
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
  }, []);

  const isOwner = me?.role === "OWNER";

  useEffect(() => {
    if (me?.accountType === "ORGANIZATION" && isOwner) {
      refreshStructure();
    } else {
      setStructure(null);
      setSelectedBranchId(ALL_BRANCHES);
    }
  }, [me?.accountType, me?.organizationId, isOwner, refreshStructure]);

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // ignore network errors on logout
    }
    setMe(null);
    setStructure(null);
    setSelectedBranchId(ALL_BRANCHES);
  }, []);

  const value = useMemo<AppContextValue>(
    () => ({
      me,
      setMe,
      loading,
      isOwner,
      refreshMe,
      logout,
      selectedBranchId,
      setSelectedBranchId,
      structure,
      refreshStructure
    }),
    [me, loading, isOwner, refreshMe, logout, selectedBranchId, structure, refreshStructure]
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
