import { Download, LayoutDashboard, Loader2, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { api, DashboardWorker } from "../api";
import { AnalyticsModal } from "../components/AnalyticsModal";
import { ALL_BRANCHES, useApp } from "../context/AppContext";
import { mapApiError } from "../utils/errors";
import { exportCsv, formatDuration } from "../utils/format";
import { toast } from "../utils/toast";

export function DashboardPage() {
  const { me, structure, selectedBranchId, isOwner } = useApp();
  const [workers, setWorkers] = useState<DashboardWorker[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState<string>("ALL");
  const [selectedWorker, setSelectedWorker] = useState<DashboardWorker | null>(null);

  const isPersonal = me?.accountType === "PERSONAL";
  const showTeamFilters = !isPersonal && isOwner;

  const departmentOptions = useMemo(() => {
    if (!structure) {
      return [];
    }
    const branches =
      selectedBranchId === ALL_BRANCHES
        ? structure.branches
        : structure.branches.filter((branch) => branch.id === selectedBranchId);
    return branches.flatMap((branch) =>
      branch.departments.map((department) => ({
        ...department,
        label:
          selectedBranchId === ALL_BRANCHES ? `${department.name} (${branch.name})` : department.name
      }))
    );
  }, [structure, selectedBranchId]);

  useEffect(() => {
    setDepartmentId("ALL");
  }, [selectedBranchId]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const handle = setTimeout(() => {
      api
        .getDashboard({
          search: showTeamFilters ? search.trim() || undefined : undefined,
          branchId: showTeamFilters && selectedBranchId !== ALL_BRANCHES ? selectedBranchId : undefined,
          departmentId: showTeamFilters && departmentId !== "ALL" ? departmentId : undefined
        })
        .then((result) => {
          if (!cancelled) {
            setWorkers(result);
          }
        })
        .catch((error) => {
          if (!cancelled) {
            toast(mapApiError(error instanceof Error ? error.message : "", "Не удалось загрузить дашборд"), "error");
          }
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [search, departmentId, selectedBranchId, showTeamFilters]);

  function onExport() {
    if (workers.length === 0) {
      toast("Нет данных для экспорта", "info");
      return;
    }
    exportCsv(
      "dashboard_export.csv",
      workers.map((worker) => ({
        Имя: worker.displayName,
        Email: worker.email,
        Отдел: worker.departmentName ?? "—",
        Филиал: worker.branchName ?? "—",
        Сегодня: formatDuration(worker.todaySeconds),
        Неделя: formatDuration(worker.weekSeconds),
        Месяц: formatDuration(worker.monthSeconds),
        Год: formatDuration(worker.yearSeconds),
        Агент: worker.agentInstalled ? `Установлен (${worker.agentVersion ?? ""})` : "Не установлен"
      }))
    );
    toast("CSV файл скачан", "success");
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold text-brand-600 dark:text-brand-300">
            <LayoutDashboard className="h-4 w-4" />
            Дашборд
          </div>
          <h1 className="mt-1 font-display text-2xl font-bold text-slate-900 dark:text-white">
            {showTeamFilters ? "Активность команды" : "Моя активность"}
          </h1>
        </div>
        <button
          onClick={onExport}
          className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 shadow-sm transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:bg-white/5 dark:text-slate-300 dark:hover:text-brand-300"
        >
          <Download className="h-4 w-4" />
          Экспорт CSV
        </button>
      </div>

      {showTeamFilters && (
        <div className="mb-5 flex flex-wrap gap-3">
          <div className="relative min-w-[220px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Поиск по имени или email"
              className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-3.5 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white"
            />
          </div>
          <select
            value={departmentId}
            onChange={(e) => setDepartmentId(e.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-600 outline-none transition focus:border-brand-500 dark:border-white/10 dark:bg-white/5 dark:text-slate-200"
          >
            <option value="ALL">Все отделы</option>
            {departmentOptions.map((department) => (
              <option key={department.id} value={department.id}>
                {department.label}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card dark:border-white/10 dark:bg-white/[0.03]">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400 dark:border-white/10">
                <th className="px-5 py-3.5 font-semibold">Сотрудник</th>
                <th className="px-5 py-3.5 font-semibold">Отдел</th>
                <th className="px-5 py-3.5 font-semibold">Сегодня</th>
                <th className="px-5 py-3.5 font-semibold">Неделя</th>
                <th className="px-5 py-3.5 font-semibold">Месяц</th>
                <th className="px-5 py-3.5 font-semibold">Год</th>
                <th className="px-5 py-3.5 font-semibold">Агент</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    <Loader2 className="mx-auto h-6 w-6 animate-spin" />
                  </td>
                </tr>
              ) : workers.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    Пока нет данных активности. Они появятся после синхронизации агента.
                  </td>
                </tr>
              ) : (
                workers.map((worker) => (
                  <tr
                    key={worker.id}
                    onClick={() => setSelectedWorker(worker)}
                    className="cursor-pointer border-b border-slate-100 transition hover:bg-brand-50/60 dark:border-white/5 dark:hover:bg-white/5"
                  >
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-slate-800 dark:text-white">{worker.displayName}</div>
                      <div className="text-xs text-slate-400">{worker.email}</div>
                    </td>
                    <td className="px-5 py-3.5">
                      {worker.departmentName ? (
                        <span className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-700 dark:bg-brand-500/10 dark:text-brand-300">
                          {worker.departmentName}
                        </span>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5 font-medium text-slate-700 dark:text-slate-200">
                      {formatDuration(worker.todaySeconds)}
                    </td>
                    <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">
                      {formatDuration(worker.weekSeconds)}
                    </td>
                    <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">
                      {formatDuration(worker.monthSeconds)}
                    </td>
                    <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">
                      {formatDuration(worker.yearSeconds)}
                    </td>
                    <td className="px-5 py-3.5">
                      <AgentBadge installed={worker.agentInstalled} version={worker.agentVersion} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <AnalyticsModal worker={selectedWorker} onClose={() => setSelectedWorker(null)} />
    </div>
  );
}

function AgentBadge({ installed, version }: { installed: boolean; version: string | null }) {
  if (installed) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
        <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
        {version ? `Установлен v${version}` : "Установлен"}
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-white/5 dark:text-slate-400">
      <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
      Не установлен
    </span>
  );
}
