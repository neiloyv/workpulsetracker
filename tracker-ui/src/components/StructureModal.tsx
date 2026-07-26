import { Building2, ChevronDown, ChevronRight, FolderTree, Plus } from "lucide-react";
import { FormEvent, useState } from "react";
import { api } from "../api";
import { useApp } from "../context/AppContext";
import { toast } from "../utils/toast";
import { Modal } from "./Modal";

type StructureModalProps = {
  open: boolean;
  onClose: () => void;
};

export function StructureModal({ open, onClose }: StructureModalProps) {
  const { structure, refreshStructure } = useApp();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [newBranchName, setNewBranchName] = useState("");
  const [addingBranch, setAddingBranch] = useState(false);
  const [departmentBranchId, setDepartmentBranchId] = useState<string | null>(null);
  const [newDepartmentName, setNewDepartmentName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function toggle(branchId: string) {
    setExpanded((prev) => ({ ...prev, [branchId]: !prev[branchId] }));
  }

  async function onCreateBranch(event: FormEvent) {
    event.preventDefault();
    if (!newBranchName.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.createBranch(newBranchName.trim());
      setNewBranchName("");
      setAddingBranch(false);
      await refreshStructure();
      toast("Филиал добавлен", "success");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать филиал");
    } finally {
      setBusy(false);
    }
  }

  async function onCreateDepartment(event: FormEvent) {
    event.preventDefault();
    if (!departmentBranchId || !newDepartmentName.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.createDepartment(departmentBranchId, newDepartmentName.trim());
      setNewDepartmentName("");
      setExpanded((prev) => ({ ...prev, [departmentBranchId]: true }));
      setDepartmentBranchId(null);
      await refreshStructure();
      toast("Отдел добавлен", "success");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось создать отдел");
    } finally {
      setBusy(false);
    }
  }

  const branches = structure?.branches ?? [];

  return (
    <Modal open={open} onClose={onClose} title="Структура компании" subtitle="Филиалы и отделы вашей организации">
      <div className="max-h-[50vh] space-y-2 overflow-y-auto pr-1 scrollbar-thin">
        {branches.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-200 py-6 text-center text-sm text-slate-400 dark:border-white/10">
            Пока нет ни одного филиала
          </p>
        )}
        {branches.map((branch) => (
          <div
            key={branch.id}
            className="rounded-xl border border-slate-200 bg-slate-50/60 dark:border-white/10 dark:bg-white/5"
          >
            <button
              onClick={() => toggle(branch.id)}
              className="flex w-full items-center justify-between px-3.5 py-2.5 text-left"
            >
              <span className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-white">
                <Building2 className="h-4 w-4 text-brand-500" />
                {branch.name}
                <span className="rounded-full bg-slate-200 px-1.5 py-0.5 text-[11px] font-medium text-slate-500 dark:bg-white/10 dark:text-slate-400">
                  {branch.departments.length}
                </span>
              </span>
              {expanded[branch.id] ? (
                <ChevronDown className="h-4 w-4 text-slate-400" />
              ) : (
                <ChevronRight className="h-4 w-4 text-slate-400" />
              )}
            </button>
            {expanded[branch.id] && (
              <div className="border-t border-slate-200 px-3.5 py-2.5 dark:border-white/10">
                <div className="space-y-1.5">
                  {branch.departments.length === 0 && (
                    <p className="text-xs text-slate-400">Нет отделов</p>
                  )}
                  {branch.departments.map((department) => (
                    <div
                      key={department.id}
                      className="flex items-center gap-2 rounded-lg bg-white px-2.5 py-1.5 text-sm text-slate-600 dark:bg-white/5 dark:text-slate-300"
                    >
                      <FolderTree className="h-3.5 w-3.5 text-slate-400" />
                      {department.name}
                    </div>
                  ))}
                </div>
                {departmentBranchId === branch.id ? (
                  <form onSubmit={onCreateDepartment} className="mt-2 flex gap-2">
                    <input
                      autoFocus
                      value={newDepartmentName}
                      onChange={(e) => setNewDepartmentName(e.target.value)}
                      placeholder="Название отдела"
                      className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-sm outline-none focus:border-brand-500 dark:border-white/10 dark:bg-white/5 dark:text-white"
                    />
                    <button
                      disabled={busy}
                      className="shrink-0 rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-brand-500 disabled:opacity-60"
                    >
                      Добавить
                    </button>
                  </form>
                ) : (
                  <button
                    onClick={() => setDepartmentBranchId(branch.id)}
                    className="mt-2 flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-500 dark:text-brand-300"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Добавить отдел
                  </button>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {error && (
        <div className="mt-3 rounded-xl border border-rose-300/50 bg-rose-50 px-3.5 py-2 text-sm text-rose-600 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
          {error}
        </div>
      )}

      <div className="mt-4 border-t border-slate-200 pt-4 dark:border-white/10">
        {addingBranch ? (
          <form onSubmit={onCreateBranch} className="flex gap-2">
            <input
              autoFocus
              value={newBranchName}
              onChange={(e) => setNewBranchName(e.target.value)}
              placeholder="Название филиала"
              className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-sm outline-none focus:border-brand-500 dark:border-white/10 dark:bg-white/5 dark:text-white"
            />
            <button
              disabled={busy}
              className="shrink-0 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:opacity-60"
            >
              Создать
            </button>
          </form>
        ) : (
          <button
            onClick={() => setAddingBranch(true)}
            className="flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-500 dark:text-brand-300"
          >
            <Plus className="h-4 w-4" />
            Добавить филиал
          </button>
        )}
      </div>
    </Modal>
  );
}
