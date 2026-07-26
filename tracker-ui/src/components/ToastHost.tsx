import { CheckCircle2, Info, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { subscribeToast, ToastEventDetail } from "../utils/toast";

const ICONS: Record<ToastEventDetail["variant"], typeof Info> = {
  info: Info,
  success: CheckCircle2,
  error: XCircle
};

const ACCENTS: Record<ToastEventDetail["variant"], string> = {
  info: "border-brand-300 text-brand-700 dark:border-brand-400/40 dark:text-brand-300",
  success: "border-emerald-300 text-emerald-700 dark:border-emerald-400/40 dark:text-emerald-300",
  error: "border-rose-300 text-rose-700 dark:border-rose-400/40 dark:text-rose-300"
};

export function ToastHost() {
  const [items, setItems] = useState<ToastEventDetail[]>([]);

  useEffect(() => {
    return subscribeToast((detail) => {
      setItems((prev) => [...prev, detail]);
      setTimeout(() => {
        setItems((prev) => prev.filter((item) => item.id !== detail.id));
      }, 3200);
    });
  }, []);

  if (items.length === 0) {
    return null;
  }

  return (
    <div className="fixed bottom-5 right-5 z-[100] flex flex-col gap-2">
      {items.map((item) => {
        const Icon = ICONS[item.variant];
        return (
          <div
            key={item.id}
            className={`flex animate-fade-in items-center gap-2.5 rounded-xl border bg-white/95 px-4 py-3 text-sm font-medium text-slate-700 shadow-card backdrop-blur dark:bg-slate-900/95 dark:text-slate-100 ${ACCENTS[item.variant]}`}
          >
            <Icon className="h-4 w-4 shrink-0" />
            {item.message}
          </div>
        );
      })}
    </div>
  );
}
