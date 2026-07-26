import { X } from "lucide-react";
import { ReactNode, useEffect } from "react";

type ModalProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  children: ReactNode;
  maxWidthClassName?: string;
  /** When true, backdrop/Escape/X cannot close the modal */
  closeLocked?: boolean;
};

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  children,
  maxWidthClassName = "max-w-lg",
  closeLocked = false
}: ModalProps) {
  useEffect(() => {
    if (!open || closeLocked) {
      return;
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, closeLocked, onClose]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm animate-fade-in"
        onClick={() => {
          if (!closeLocked) {
            onClose();
          }
        }}
      />
      <div
        className={`relative w-full ${maxWidthClassName} animate-fade-in-scale rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-white/10 dark:bg-slate-900`}
        role="dialog"
        aria-modal="true"
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <h2 className="font-display text-lg font-semibold text-slate-900 dark:text-white">{title}</h2>
            {subtitle && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
          </div>
          {!closeLocked && (
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-white/10 dark:hover:text-white"
              aria-label="Закрыть"
            >
              <X className="h-5 w-5" />
            </button>
          )}
        </div>
        {children}
      </div>
    </div>
  );
}
