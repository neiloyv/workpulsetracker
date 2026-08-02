import { Pencil, Plus, Search, ShieldCheck, Trash2, UserPlus } from "lucide-react";
import { FormEvent, ReactNode, useEffect, useState } from "react";
import { api, Manager } from "../api";
import { Modal } from "../components/Modal";
import { useApp } from "../context/AppContext";
import { mapApiError } from "../utils/errors";
import { initials } from "../utils/format";
import { toast } from "../utils/toast";

type FormState = {
  displayName: string;
  email: string;
  password: string;
  status: string;
};

const EMPTY_FORM: FormState = {
  displayName: "",
  email: "",
  password: "",
  status: "ACTIVE"
};

export function ManagersPage() {
  const { me } = useApp();
  const [managers, setManagers] = useState<Manager[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const handle = setTimeout(() => {
      api
        .getManagers({ search: search.trim() || undefined })
        .then((result) => {
          if (!cancelled) {
            setManagers(result);
          }
        })
        .catch((loadError) => {
          if (!cancelled) {
            toast(
              mapApiError(loadError instanceof Error ? loadError.message : "", "Не удалось загрузить менеджеров"),
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
  }, [search]);

  function reloadManagers() {
    api
      .getManagers({ search: search.trim() || undefined })
      .then(setManagers)
      .catch(() => undefined);
  }

  function openCreateModal() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError(null);
    setModalOpen(true);
  }

  function openEditModal(manager: Manager) {
    setEditingId(manager.id);
    setForm({
      displayName: manager.displayName,
      email: manager.email,
      password: "",
      status: manager.status || "ACTIVE"
    });
    setError(null);
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      if (editingId != null) {
        await api.updateManager(editingId, {
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          password: form.password.trim() || undefined,
          status: form.status
        });
        toast("Данные менеджера обновлены", "success");
      } else {
        await api.createManager({
          displayName: form.displayName.trim(),
          email: form.email.trim(),
          password: form.password
        });
        toast("Менеджер создан", "success");
      }
      setModalOpen(false);
      reloadManagers();
    } catch (err) {
      setError(mapApiError(err instanceof Error ? err.message : "", "Не удалось сохранить менеджера"));
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(manager: Manager) {
    if (me?.id === manager.id) {
      toast("Нельзя удалить свой аккаунт", "error");
      return;
    }
    const confirmed = window.confirm(`Удалить менеджера «${manager.displayName}»?`);
    if (!confirmed) {
      return;
    }
    try {
      await api.deleteManager(manager.id);
      toast("Менеджер удалён", "success");
      reloadManagers();
    } catch (err) {
      toast(mapApiError(err instanceof Error ? err.message : "", "Не удалось удалить менеджера"), "error");
    }
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold text-brand-600 dark:text-brand-300">
            <ShieldCheck className="h-4 w-4" />
            Менеджеры
          </div>
          <h1 className="mt-1 font-display text-2xl font-bold text-slate-900 dark:text-white">
            Управление менеджерами
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Менеджеры входят с ролью MANAGER и пока имеют тот же доступ, что и владелец.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 px-4 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:brightness-110"
        >
          <UserPlus className="h-4 w-4" />
          Добавить менеджера
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
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400 dark:border-white/10">
                <th className="px-5 py-3.5 font-semibold">Менеджер</th>
                <th className="px-5 py-3.5 font-semibold">Email</th>
                <th className="px-5 py-3.5 font-semibold">Статус</th>
                <th className="px-5 py-3.5 text-right font-semibold">Действия</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-5 py-12 text-center text-slate-400">
                    Загрузка...
                  </td>
                </tr>
              ) : managers.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-5 py-12 text-center text-slate-400">
                    Менеджеры не найдены
                  </td>
                </tr>
              ) : (
                managers.map((manager) => (
                  <tr
                    key={manager.id}
                    className="border-b border-slate-100 transition hover:bg-slate-50/60 dark:border-white/5 dark:hover:bg-white/5"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-sky-500 text-xs font-bold text-white">
                          {initials(manager.displayName)}
                        </span>
                        <div>
                          <div className="font-semibold text-slate-800 dark:text-white">{manager.displayName}</div>
                          <div className="text-xs text-slate-400">id {manager.id}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">{manager.email}</td>
                    <td className="px-5 py-3.5">
                      {manager.status === "ACTIVE" ? (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                          Активен
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-white/5 dark:text-slate-400">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
                          Отключён
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1.5">
                        <ActionIconButton title="Редактировать" onClick={() => openEditModal(manager)}>
                          <Pencil className="h-4 w-4" />
                        </ActionIconButton>
                        <ActionIconButton title="Удалить" danger onClick={() => onDelete(manager)}>
                          <Trash2 className="h-4 w-4" />
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
        title={editingId != null ? "Редактировать менеджера" : "Новый менеджер"}
        subtitle={editingId != null ? "Пароль меняйте только если нужно" : "Менеджер сможет войти в веб-кабинет"}
      >
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
          <Field label={editingId != null ? "Новый пароль (необязательно)" : "Пароль"}>
            <input
              type="password"
              required={editingId == null}
              minLength={8}
              value={form.password}
              onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
              className={inputClass}
              placeholder={editingId != null ? "Оставьте пустым, чтобы не менять" : "Минимум 8 символов"}
            />
          </Field>
          {editingId != null && (
            <Field label="Статус">
              <select
                value={form.status}
                onChange={(e) => setForm((prev) => ({ ...prev, status: e.target.value }))}
                className={inputClass}
              >
                <option value="ACTIVE">Активен</option>
                <option value="DISABLED">Отключён</option>
              </select>
            </Field>
          )}
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
            {saving ? "Сохраняем..." : editingId != null ? "Сохранить изменения" : "Создать менеджера"}
          </button>
        </form>
      </Modal>
    </div>
  );
}

function ActionIconButton({
  title,
  onClick,
  children,
  danger
}: {
  title: string;
  onClick: () => void;
  children: ReactNode;
  danger?: boolean;
}) {
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border transition ${
        danger
          ? "border-slate-200 text-rose-500 hover:border-rose-400 hover:text-rose-600 dark:border-white/10 dark:text-rose-400 dark:hover:text-rose-300"
          : "border-slate-200 text-slate-600 hover:border-brand-400 hover:text-brand-600 dark:border-white/10 dark:text-slate-300 dark:hover:text-brand-300"
      }`}
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

const inputClass =
  "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-white/5 dark:text-white dark:placeholder:text-slate-500";
