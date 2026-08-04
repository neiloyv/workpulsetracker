import { Copy, Download, KeyRound, Mail, MonitorSmartphone } from "lucide-react";
import { useEffect, useState } from "react";
import { api, AgentInfo, Downloads } from "../api";
import { useLocale } from "../context/LocaleContext";
import { mapApiError } from "../utils/errors";
import { toast } from "../utils/toast";

export function AgentPage() {
  const { t } = useLocale();
  const [agentInfo, setAgentInfo] = useState<AgentInfo | null>(null);
  const [downloads, setDownloads] = useState<Downloads | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.getAgentInfo(), api.getDownloads()])
      .then(([info, downloadLinks]) => {
        if (!cancelled) {
          setAgentInfo(info);
          setDownloads(downloadLinks);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          toast(mapApiError(error instanceof Error ? error.message : "", t("agent.loadError")), "error");
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
  }, [t]);

  async function onCopyKey() {
    try {
      const result = await api.getMyAccessKey();
      await navigator.clipboard.writeText(result.accessKey);
      toast(t("agent.copySuccess"), "success");
    } catch (error) {
      toast(mapApiError(error instanceof Error ? error.message : "", t("agent.copyError")), "error");
    }
  }

  async function onResendKey() {
    try {
      await api.resendMyAccessKey();
      toast(t("agent.resendSuccess"), "success");
    } catch (error) {
      toast(mapApiError(error instanceof Error ? error.message : "", t("agent.resendError")), "error");
    }
  }

  if (loading) {
    return <div className="py-16 text-center text-slate-400">{t("common.loading")}</div>;
  }

  if (!agentInfo) {
    return <div className="py-16 text-center text-slate-400">{t("agent.noData")}</div>;
  }

  return (
    <div className="animate-fade-in mx-auto max-w-3xl">
      <div className="mb-6">
        <div className="flex items-center gap-2 text-sm font-semibold text-brand-600 dark:text-brand-300">
          <KeyRound className="h-4 w-4" />
          {t("agent.eyebrow")}
        </div>
        <h1 className="mt-1 font-display text-2xl font-bold text-slate-900 dark:text-white">
          {t("agent.title")}
        </h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t("agent.subtitle")}</p>
      </div>

      <div className="grid gap-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-card dark:border-white/10 dark:bg-white/[0.03]">
          <div className="flex items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-500/10 dark:text-brand-300">
              <MonitorSmartphone className="h-6 w-6" />
            </div>
            <div className="flex-1">
              <div className="font-display text-lg font-semibold text-slate-900 dark:text-white">
                {agentInfo.displayName}
              </div>
              <div className="text-sm text-slate-500">{agentInfo.email}</div>
              <div className="mt-3 flex flex-wrap gap-2">
                <StatusPill
                  ok={agentInfo.agentInstalled}
                  label={
                    agentInfo.agentInstalled
                      ? agentInfo.agentVersion
                        ? t("agent.status.installedVersion", { version: agentInfo.agentVersion })
                        : t("agent.status.installed")
                      : t("agent.status.notConnected")
                  }
                />
                <StatusPill
                  ok={agentInfo.status === "ACTIVE"}
                  label={t("agent.status.label", { status: agentInfo.status })}
                />
                {agentInfo.accessKeyPrefix && (
                  <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-white/5 dark:text-slate-300">
                    {t("agent.keyPrefix", { prefix: agentInfo.accessKeyPrefix })}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="mt-6 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={onCopyKey}
              className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 px-4 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:brightness-110"
            >
              <Copy className="h-4 w-4" />
              {t("agent.copyKey")}
            </button>
            <button
              type="button"
              onClick={onResendKey}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
            >
              <Mail className="h-4 w-4" />
              {t("agent.resendKey")}
            </button>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-card dark:border-white/10 dark:bg-white/[0.03]">
          <h2 className="font-display text-base font-semibold text-slate-900 dark:text-white">
            {t("agent.howToTitle")}
          </h2>
          <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm text-slate-600 dark:text-slate-300">
            <li>{t("agent.howTo.step1")}</li>
            <li>{t("agent.howTo.step2")}</li>
            <li>{t("agent.howTo.step3")}</li>
            <li>{t("agent.howTo.step4")}</li>
          </ol>

          {downloads && (
            <div className="mt-5 flex flex-wrap gap-2">
              <DownloadLink href={downloads.windowsUrl} label={t("agent.download.windows")} />
              <DownloadLink href={downloads.macosUrl} label={t("agent.download.macos")} />
              <DownloadLink href={downloads.linuxUrl} label={t("agent.download.linux")} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StatusPill({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${
        ok
          ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300"
          : "bg-slate-100 text-slate-500 dark:bg-white/5 dark:text-slate-400"
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${ok ? "bg-emerald-500" : "bg-slate-400"}`} />
      {label}
    </span>
  );
}

function DownloadLink({ href, label }: { href: string; label: string }) {
  if (!href) {
    return null;
  }
  const fileName = href.split("/").filter(Boolean).at(-1);
  return (
    <a
      href={href}
      download={fileName}
      className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-3.5 py-2 text-sm font-semibold text-slate-600 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
    >
      <Download className="h-4 w-4" />
      {label}
    </a>
  );
}
