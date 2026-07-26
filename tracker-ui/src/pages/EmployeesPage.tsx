import { Check, Copy, KeyRound, Pencil, Plus, Search, UserPlus, Users } from "lucide-react";
import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import { api, CreateEmployeeResult, Employee } from "../api";
import { Modal } from "../components/Modal";
import { ALL_BRANCHES, useApp } from "../context/AppContext";
import { initials } from "../utils/format";
import { toast } from "../utils/toast";

type FormState = {
  displayName: string;
  email: string;
  phone: string;
  branchId: string;
  departmentId: string;
  password: string;
};

const EMPTY_FORM: FormState = {
  displayName: "",
  email: "",
  phone: "",
  branchId: "",
  departmentId: "",
  password: ""
};

export function EmployeesPage() {
  const { structure, selectedBranchId } = useApp();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdResult, setCreatedResult] = useState<CreateEmployeeResult | null>(null);
  const [copied, setCopied] = useState(false);

  function loadEmployees() {
    setLoading(true);
    api
      .getEmployees({
        search: search.trim() || undefined,
        branchId: selectedBranchId === ALL_BRANCHES ? undefined : selectedBranchId
      })
      .then(setEmployees)
      .catch(() => toast("Не удалось загрузить сотрудников", "error"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    const handle = setTimeout(loadEmployees, 250);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, selectedBranchId]);

  const departmentOptions = useMemo(() => {
    const branch = structure?.branches.find((item) => item.id === form.branchId);
    return branch?.departments ?? [];
  }, [structure, form.branchId]);

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

  function openEditModal(employee: Employee) {
    setEditingId(employee.id);
    setForm({
      displayName: employee.displayName,
      email: employee.email,
      phone: employee.phone ?? "",
      branchId: employee.branchId ?? "",
      departmentId: employee.departmentId ?? "",
      password: ""
    });
    setError(null);
    setCreatedResult(null);
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setCreatedResult(null);
    setCopied(false);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      if (editingId) {
        await api.updateEmployee(editingId, {
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          phone: form.phone.trim() || undefined,
          branchId: form.branchId || null,
          departmentId: form.departmentId || null
        });
        toast("Данные сотрудника обновлены", "success");
        closeModal();
        loadEmployees();
      } else {
        const result = await api.createEmployee({
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          phone: form.phone.trim() || undefined,
          branchId: form.branchId || null,
          departmentId: form.departmentId || null,
          password: form.password.trim() || undefined
        });
        setCreatedResult(result);
        loadEmployees();
      }
    } catch (err) {
      setError(err instanceof Error ? mapError(err.message) : "Не удалось сохранить сотрудника");
    } finally {
      setSaving(false);
    }
  }

  function onCopyKey() {
    if (!createdResult?.agentKey) {
      return;
    }
    navigator.clipboard.writeText(createdResult.agentKey).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    });
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
            Команда организации
          </h1>
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
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400 dark:border-white/10">
                <th className="px-5 py-3.5 font-semibold">Сотрудник</th>
                <th className="px-5 py-3.5 font-semibold">Контакты</th>
                <th className="px-5 py-3.5 font-semibold">Филиал / отдел</th>
                <th className="px-5 py-3.5 font-semibold">Агент</th>
                <th className="px-5 py-3.5 font-semibold text-right">Действия</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-slate-400">
                    Загрузка...
                  </td>
                </tr>
              ) : employees.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-slate-400">
                    Сотрудники не найдены
                  </td>
                </tr>
              ) : (
                employees.map((employee) => (
                  <tr
                    key={employee.id}
                    className="border-b border-slate-100 transition hover:bg-slate-50/60 dark:border-white/5 dark:hover:bg-white/5"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-sky-500 text-xs font-bold text-white">
                          {initials(employee.displayName)}
                        </span>
                        <span className="font-semibold text-slate-800 dark:text-white">{employee.displayName}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">
                      <div>{employee.email}</div>
                      {employee.phone && <div className="text-xs">{employee.phone}</div>}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-wrap gap-1.5">
                        {employee.branchName && <Badge>{employee.branchName}</Badge>}
                        {employee.departmentName && <Badge accent>{employee.departmentName}</Badge>}
                        {!employee.branchName && !employee.departmentName && (
                          <span className="text-slate-400">—</span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      {employee.agentInstalled ? (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                          Установлен
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-white/5 dark:text-slate-400">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
                          Не установлен
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        onClick={() => openEditModal(employee)}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        Изменить
                      </button>
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
        title={createdResult ? "Сотрудник добавлен" : editingId ? "Редактировать сотрудника" : "Новый сотрудник"}
        subtitle={createdResult ? "Сохраните ключ агента — он больше не будет показан" : undefined}
      >
        {createdResult ? (
          <div className="grid gap-4">
            <div className="rounded-xl border border-emerald-300/40 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
              Сотрудник <strong>{createdResult.displayName}</strong> успешно создан.
            </div>
            {createdResult.agentKey && (
              <div>
                <div className="mb-1.5 flex items-center gap-1.5 text-sm font-medium text-slate-600 dark:text-slate-300">
                  <KeyRound className="h-4 w-4" />
                  Ключ агента (показывается один раз)
                </div>
                <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 dark:border-white/10 dark:bg-white/5">
                  <code className="flex-1 overflow-x-auto whitespace-nowrap text-sm text-slate-800 dark:text-slate-100">
                    {createdResult.agentKey}
                  </code>
                  <button
                    onClick={onCopyKey}
                    className="shrink-0 rounded-lg border border-slate-200 p-1.5 text-slate-500 transition hover:text-brand-600 dark:border-white/10 dark:text-slate-400"
                  >
                    {copied ? <Check className="h-4 w-4 text-emerald-500" /> : <Copy className="h-4 w-4" />}
                  </button>
                </div>
              </div>
            )}
            {createdResult.temporaryPassword && (
              <div>
                <div className="mb-1.5 text-sm font-medium text-slate-600 dark:text-slate-300">
                  Временный пароль
                </div>
                <div className="rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-800 dark:border-white/10 dark:bg-white/5 dark:text-slate-100">
                  {createdResult.temporaryPassword}
                </div>
              </div>
            )}
            <button
              onClick={closeModal}
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
              <Field label="Телефон">
                <input
                  value={form.phone}
                  onChange={(e) => setForm((prev) => ({ ...prev, phone: e.target.value }))}
                  placeholder="Необязательно"
                  className={inputClass}
                />
              </Field>
              {!editingId && (
                <Field label="Пароль">
                  <input
                    type="text"
                    value={form.password}
                    onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
                    placeholder="Авто-генерация"
                    className={inputClass}
                  />
                </Field>
              )}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Филиал">
                <select
                  value={form.branchId}
                  onChange={(e) => setForm((prev) => ({ ...prev, branchId: e.target.value, departmentId: "" }))}
                  className={inputClass}
                >
                  <option value="">Не выбран</option>
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
                  <option value="">Не выбран</option>
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
              {saving ? "Сохраняем..." : editingId ? "Сохранить изменения" : "Создать сотрудника"}
            </button>
          </form>
        )}
      </Modal>
    </div>
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

function mapError(message: string): string {
  if (message.toLowerCase().includes("already exists")) {
    return "Сотрудник с таким email уже существует";
  }
  return message;
}

const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white dark:placeholder:text-slate-500";
