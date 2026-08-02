import { KeyRound, Mail, Pencil, Plus, Search, UserPlus, Users } from "lucide-react";
import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import { api, CreateWorkerResult, Worker } from "../api";
import { Modal } from "../components/Modal";
import { ALL_BRANCHES, useApp } from "../context/AppContext";
import { mapApiError } from "../utils/errors";
import { initials } from "../utils/format";
import { toast } from "../utils/toast";

type FormState = {
  displayName: string;
  email: string;
  branchId: string;
  departmentId: string;
};

const EMPTY_FORM: FormState = {
  displayName: "",
  email: "",
  branchId: "",
  departmentId: ""
};

export function WorkersPage() {
  const { structure, selectedBranchId } = useApp();
  const [workers, setWorkers] = useState<Worker[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdResult, setCreatedResult] = useState<CreateWorkerResult | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const handle = setTimeout(() => {
      api
        .getWorkers({
          search: search.trim() || undefined,
          branchId: selectedBranchId === ALL_BRANCHES ? undefined : selectedBranchId
        })
        .then((result) => {
          if (!cancelled) {
            setWorkers(result);
          }
        })
        .catch((loadError) => {
          if (!cancelled) {
            toast(
              mapApiError(loadError instanceof Error ? loadError.message : "", "Не удалось загрузить сотрудников"),
              "error"
            );
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
  }, [search, selectedBranchId]);

  const departmentOptions = useMemo(() => {
    const branch = structure?.branches.find((item) => String(item.id) === form.branchId);
    return branch?.departments ?? [];
  }, [structure, form.branchId]);

  function reloadWorkers() {
    api
      .getWorkers({
        search: search.trim() || undefined,
        branchId: selectedBranchId === ALL_BRANCHES ? undefined : selectedBranchId
      })
      .then(setWorkers)
      .catch(() => undefined);
  }

  function openCreateModal() {
    setEditingId(null);
    setForm({
      ...EMPTY_FORM,
      branchId: selectedBranchId === ALL_BRANCHES ? "" : selectedBranchId
    });
    setError(null);
    setCreatedResult(null);
    setModalOpen(true);
  }

  function openEditModal(worker: Worker) {
    setEditingId(worker.id);
    setForm({
      displayName: worker.displayName,
      email: worker.email,
      branchId: worker.branchId != null ? String(worker.branchId) : "",
      departmentId: worker.departmentId != null ? String(worker.departmentId) : ""
    });
    setError(null);
    setCreatedResult(null);
    setModalOpen(true);
  }

  function closeModal() {
    if (createdResult) {
      return;
    }
    setModalOpen(false);
  }

  function acknowledgeCreated() {
    setModalOpen(false);
    setCreatedResult(null);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      if (editingId != null) {
        await api.updateWorker(editingId, {
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          branchId: form.branchId ? Number(form.branchId) : null,
          departmentId: form.departmentId ? Number(form.departmentId) : null
        });
        toast("Данные сотрудника обновлены", "success");
        setModalOpen(false);
        reloadWorkers();
      } else {
        const result = await api.createWorker({
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          branchId: form.branchId ? Number(form.branchId) : null,
          departmentId: form.departmentId ? Number(form.departmentId) : null
        });
        setCreatedResult(result);
        reloadWorkers();
      }
    } catch (err) {
      setError(mapApiError(err instanceof Error ? err.message : "", "Не удалось сохранить сотрудника"));
    } finally {
      setSaving(false);
    }
  }

  async function onCopyAccessKey(workerId: number) {
    try {
      const result = await api.getWorkerAccessKey(workerId);
      await navigator.clipboard.writeText(result.accessKey);
      toast("Access key скопирован", "success");
    } catch (err) {
      toast(mapApiError(err instanceof Error ? err.message : "", "Не удалось скопировать ключ"), "error");
    }
  }

  async function onResendAccessKey(workerId: number) {
    try {
      await api.resendWorkerAccessKey(workerId);
      toast("Ключ отправлен на email сотрудника", "success");
    } catch (err) {
      toast(mapApiError(err instanceof Error ? err.message : "", "Не удалось отправить ключ"), "error");
    }
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold text-brand-600 dark:text-brand-300">
            <Users className="h-4 w-4" />
            Сотрудники
          </div>
          <h1 className="mt-1 font-display text-2xl font-bold text-slate-900 dark:text-white">
            Workers организации
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            При создании ключ уходит на email. При необходимости его можно скопировать или переотправить.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 px-4 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:brightness-110"
        >
          <UserPlus className="h-4 w-4" />
          Добавить сотрудника
        </button>
      </div>

      <div className="relative mb-5 max-w-md">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Поиск по имени или email"
          className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-3.5 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white"
        />
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card dark:border-white/10 dark:bg-white/[0.03]">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400 dark:border-white/10">
                <th className="px-5 py-3.5 font-semibold">Сотрудник</th>
                <th className="px-5 py-3.5 font-semibold">Email</th>
                <th className="px-5 py-3.5 font-semibold">Филиал / отдел</th>
                <th className="px-5 py-3.5 font-semibold">Агент</th>
                <th className="px-5 py-3.5 text-right font-semibold">Действия</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-slate-400">
                    Загрузка...
                  </td>
                </tr>
              ) : workers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-slate-400">
                    Сотрудники не найдены
                  </td>
                </tr>
              ) : (
                workers.map((worker) => (
                  <tr
                    key={worker.id}
                    className="border-b border-slate-100 transition hover:bg-slate-50/60 dark:border-white/5 dark:hover:bg-white/5"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-sky-500 text-xs font-bold text-white">
                          {initials(worker.displayName)}
                        </span>
                        <div>
                          <div className="font-semibold text-slate-800 dark:text-white">{worker.displayName}</div>
                          <div className="text-xs text-slate-400">id {worker.id}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">{worker.email}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-wrap gap-1.5">
                        {worker.branchName && <Badge>{worker.branchName}</Badge>}
                        {worker.departmentName && <Badge accent>{worker.departmentName}</Badge>}
                        {!worker.branchName && !worker.departmentName && (
                          <span className="text-slate-400">—</span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      {worker.agentInstalled ? (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                          {worker.agentVersion ? `Установлен v${worker.agentVersion}` : "Установлен"}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-white/5 dark:text-slate-400">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
                          Не установлен
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1.5">
                        <ActionIconButton
                          title="Скопировать access key"
                          onClick={() => onCopyAccessKey(worker.id)}
                        >
                          <KeyRound className="h-4 w-4" />
                        </ActionIconButton>
                        <ActionIconButton
                          title="Отправить ключ на email"
                          onClick={() => onResendAccessKey(worker.id)}
                        >
                          <Mail className="h-4 w-4" />
                        </ActionIconButton>
                        <ActionIconButton title="Редактировать" onClick={() => openEditModal(worker)}>
                          <Pencil className="h-4 w-4" />
                        </ActionIconButton>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={modalOpen}
        onClose={closeModal}
        closeLocked={Boolean(createdResult)}
        title={createdResult ? "Сотрудник добавлен" : editingId != null ? "Редактировать сотрудника" : "Новый сотрудник"}
        subtitle={
          createdResult
            ? "Access key отправлен на email сотрудника"
            : undefined
        }
      >
        {createdResult ? (
          <div className="grid gap-4">
            <div className="rounded-xl border border-emerald-300/40 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
              Сотрудник <strong>{createdResult.displayName}</strong> создан.
              {createdResult.accessKeySent
                ? " Ключ отправлен на email."
                : " Ключ можно скопировать в таблице."}
            </div>
            <button
              type="button"
              onClick={acknowledgeCreated}
              className="mt-1 w-full rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:brightness-110"
            >
              Готово
            </button>
          </div>
        ) : (
          <form className="grid gap-4" onSubmit={onSubmit}>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Имя">
                <input
                  required
                  value={form.displayName}
                  onChange={(e) => setForm((prev) => ({ ...prev, displayName: e.target.value }))}
                  className={inputClass}
                />
              </Field>
              <Field label="Email">
                <input
                  type="email"
                  required
                  value={form.email}
                  onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
                  className={inputClass}
                />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Филиал">
                <select
                  value={form.branchId}
                  onChange={(e) => setForm((prev) => ({ ...prev, branchId: e.target.value, departmentId: "" }))}
                  className={inputClass}
                >
                  <option value="">По умолчанию</option>
                  {structure?.branches.map((branch) => (
                    <option key={branch.id} value={branch.id}>
                      {branch.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Отдел">
                <select
                  value={form.departmentId}
                  onChange={(e) => setForm((prev) => ({ ...prev, departmentId: e.target.value }))}
                  disabled={!form.branchId}
                  className={`${inputClass} disabled:cursor-not-allowed disabled:opacity-50`}
                >
                  <option value="">По умолчанию</option>
                  {departmentOptions.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            {error && (
              <div className="rounded-xl border border-rose-300/50 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-600 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={saving}
              className="mt-1 flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Plus className="h-4 w-4" />
              {saving ? "Сохраняем..." : editingId != null ? "Сохранить изменения" : "Создать сотрудника"}
            </button>
          </form>
        )}
      </Modal>
    </div>
  );
}

function ActionIconButton({
  title,
  onClick,
  children
}: {
  title: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
    >
      {children}
    </button>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="grid gap-1.5 text-sm">
      <span className="font-medium text-slate-600 dark:text-slate-300">{label}</span>
      {children}
    </label>
  );
}

function Badge({ children, accent }: { children: ReactNode; accent?: boolean }) {
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
        accent
          ? "bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300"
          : "bg-slate-100 text-slate-600 dark:bg-white/5 dark:text-slate-300"
      }`}
    >
      {children}
    </span>
  );
}

const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white dark:placeholder:text-slate-500";
