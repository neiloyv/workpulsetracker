import { Download, Loader2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, AppUsage, DashboardPeriod, DashboardWorker } from "../api";
import { exportCsv, formatDuration } from "../utils/format";
import { mapApiError } from "../utils/errors";
import { toast } from "../utils/toast";
import { Modal } from "./Modal";

type AnalyticsModalProps = {
  worker: DashboardWorker | null;
  onClose: () => void;
};

const PERIODS: Array<{ id: DashboardPeriod; label: string }> = [
  { id: "TODAY", label: "Сегодня" },
  { id: "WEEK", label: "Неделя" },
  { id: "MONTH", label: "Месяц" },
  { id: "YEAR", label: "Год" }
];

const ACTIVE_COLORS = ["#7458ff", "#8b7bff", "#38bdf8", "#34d399", "#fbbf24", "#f472b6"];
const IDLE_COLOR = "#94a3b8";

export function AnalyticsModal({ worker, onClose }: AnalyticsModalProps) {
  const [period, setPeriod] = useState<DashboardPeriod>("TODAY");
  const [usage, setUsage] = useState<AppUsage[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!worker) {
      return;
    }
    setPeriod("TODAY");
  }, [worker]);

  useEffect(() => {
    if (!worker) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    api
      .getWorkerApps(worker.id, period)
      .then((result) => {
        if (!cancelled) {
          setUsage(result);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setUsage([]);
          toast(
            mapApiError(error instanceof Error ? error.message : "", "Не удалось загрузить аналитику"),
            "error"
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [worker, period]);

  const chartData = useMemo(
    () =>
      usage
        .slice()
        .sort((a, b) => b.seconds - a.seconds)
        .map((item) => ({
          ...item,
          label: item.idle ? "Простой" : item.appName
        })),
    [usage]
  );

  const totalSeconds = useMemo(() => usage.reduce((sum, item) => sum + item.seconds, 0), [usage]);

  if (!worker) {
    return null;
  }

  function onDownloadReport() {
    if (!worker) {
      return;
    }
    exportCsv(
      `${worker.displayName.replace(/\s+/g, "_")}_report_${period}.csv`,
      usage.map((item) => ({
        Приложение: item.idle ? "Простой" : item.appName,
        Время: formatDuration(item.seconds),
        Секунды: item.seconds,
        Процент: `${item.percent}%`
      }))
    );
    toast("Отчет скачан", "success");
  }

  return (
    <Modal
      open={Boolean(worker)}
      onClose={onClose}
      title={worker.displayName}
      subtitle={worker.email}
      maxWidthClassName="max-w-2xl"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-xl bg-slate-100 p-1 dark:bg-white/5">
          {PERIODS.map((item) => (
            <button
              key={item.id}
              onClick={() => setPeriod(item.id)}
              className={`rounded-lg px-3.5 py-1.5 text-sm font-semibold transition ${
                period === item.id
                  ? "bg-white text-slate-900 shadow-sm dark:bg-white/10 dark:text-white"
                  : "text-slate-500 dark:text-slate-400"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <button
          onClick={onDownloadReport}
          disabled={usage.length === 0}
          className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-3.5 py-1.5 text-sm font-semibold text-slate-600 transition hover:border-brand-400 hover:text-brand-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
        >
          <Download className="h-4 w-4" />
          Скачать отчет
        </button>
      </div>

      <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 dark:border-white/10 dark:bg-white/5">
        <div className="mb-3 flex items-center justify-between text-sm">
          <span className="text-slate-500 dark:text-slate-400">Всего активности</span>
          <span className="font-display font-semibold text-slate-900 dark:text-white">
            {formatDuration(totalSeconds)}
          </span>
        </div>

        {loading ? (
          <div className="flex h-64 items-center justify-center text-slate-400">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : chartData.length === 0 ? (
          <div className="flex h-64 items-center justify-center text-sm text-slate-400">
            Нет данных за выбранный период
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="text-slate-200 dark:text-white/10" />
              <XAxis
                dataKey="label"
                tick={{ fontSize: 12, fill: "currentColor" }}
                className="text-slate-500 dark:text-slate-400"
                interval={0}
                angle={-20}
                textAnchor="end"
                height={60}
              />
              <YAxis
                tick={{ fontSize: 12, fill: "currentColor" }}
                className="text-slate-500 dark:text-slate-400"
                tickFormatter={(value: number) => formatDuration(value)}
                width={70}
              />
              <Tooltip
                cursor={{ fill: "rgba(116, 88, 255, 0.08)" }}
                contentStyle={{
                  background: "rgb(15 15 26)",
                  border: "1px solid rgba(255,255,255,0.1)",
                  borderRadius: 12,
                  color: "white",
                  fontSize: 13
                }}
                formatter={((value: number, _name: string, item: { payload: { percent: number; label: string } }) => [
                  `${formatDuration(value)} · ${item.payload.percent}%`,
                  item.payload.label
                ]) as never}
              />
              <Bar dataKey="seconds" radius={[8, 8, 0, 0]} maxBarSize={56}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={entry.appName}
                    fill={entry.idle ? IDLE_COLOR : ACTIVE_COLORS[index % ACTIVE_COLORS.length]}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {chartData.length > 0 && (
        <div className="mt-4 grid max-h-40 gap-1.5 overflow-y-auto pr-1 scrollbar-thin">
          {chartData.map((item, index) => (
            <div
              key={item.appName}
              className="flex items-center justify-between rounded-lg px-2.5 py-1.5 text-sm text-slate-600 dark:text-slate-300"
            >
              <span className="flex items-center gap-2">
                <span
                  className="h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: item.idle ? IDLE_COLOR : ACTIVE_COLORS[index % ACTIVE_COLORS.length] }}
                />
                {item.label}
              </span>
              <span className="font-medium text-slate-700 dark:text-slate-200">
                {formatDuration(item.seconds)} · {item.percent}%
              </span>
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}
