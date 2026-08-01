import { useMemo, useState } from 'react';
import { Plus, Trash2, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge, Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { complianceStore, clientsStore, logAudit, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { ComplianceCategory, ComplianceItem, ComplianceStatus, Priority } from '../../types';

const CATEGORIES: ComplianceCategory[] = ['GST', 'TDS', 'ROC', 'Income Tax', 'Audit', 'Other'];
const PRIORITIES: Priority[] = ['Low', 'Medium', 'High'];

export function AdminCompliance() {
  const { user } = useAuth();
  const { show } = useToast();
  const items = complianceStore.useAll();
  const clients = clientsStore.useAll();

  const [categoryFilter, setCategoryFilter] = useState<'All' | ComplianceCategory>('All');
  const [statusFilter, setStatusFilter] = useState<'All' | ComplianceStatus>('All');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    clientId: '',
    title: '',
    category: 'GST' as ComplianceCategory,
    dueDate: new Date().toISOString().slice(0, 10),
    assignedTo: '',
    priority: 'Medium' as Priority,
  });

  const filtered = useMemo(() => {
    return items.filter((i) => (categoryFilter === 'All' || i.category === categoryFilter) && (statusFilter === 'All' || i.status === statusFilter));
  }, [items, categoryFilter, statusFilter]);

  const sorted = useMemo(() => [...filtered].sort((a, b) => (a.dueDate < b.dueDate ? -1 : 1)), [filtered]);

  const openAdd = () => {
    setForm({ clientId: clients[0]?.id ?? '', title: '', category: 'GST', dueDate: new Date().toISOString().slice(0, 10), assignedTo: '', priority: 'Medium' });
    setModalOpen(true);
  };

  const save = () => {
    const client = clients.find((c) => c.id === form.clientId);
    if (!client || !form.title) {
      show('Please fill in all required fields.', 'error');
      return;
    }
    const id = genId('cmp');
    const item: ComplianceItem = {
      id,
      clientId: client.id,
      clientName: client.businessName,
      title: form.title,
      category: form.category,
      dueDate: new Date(form.dueDate).toISOString(),
      status: 'Pending',
      assignedTo: form.assignedTo || 'Unassigned',
      priority: form.priority,
    };
    complianceStore.add(item);
    logAudit(user?.name ?? 'Admin', 'admin', 'Added compliance item', 'Compliance', id, `Added "${form.title}" for ${client.businessName}`);
    pushNotification('client', 'New compliance requirement', `${form.title} is due on ${formatDate(item.dueDate)}.`, 'warning', client.id, true);
    show('Compliance item added.', 'success');
    setModalOpen(false);
  };

  const complete = (item: ComplianceItem) => {
    complianceStore.update(item.id, { status: 'Completed', completedDate: new Date().toISOString() });
    logAudit(user?.name ?? 'Admin', 'admin', 'Completed compliance item', 'Compliance', item.id, `Marked "${item.title}" as completed`);
    show('Marked as completed.', 'success');
  };

  const remove = (item: ComplianceItem) => {
    if (!confirm(`Remove "${item.title}"?`)) return;
    complianceStore.remove(item.id);
    show('Compliance item removed.', 'success');
  };

  const columns: Column<ComplianceItem>[] = [
    { header: 'Requirement', render: (i) => <span className="font-medium text-slate-900">{i.title}</span> },
    { header: 'Client', render: (i) => i.clientName },
    { header: 'Category', render: (i) => <Badge tone="blue">{i.category}</Badge> },
    { header: 'Due Date', render: (i) => formatDate(i.dueDate) },
    { header: 'Priority', render: (i) => <StatusBadge status={i.priority} /> },
    { header: 'Assigned To', render: (i) => i.assignedTo },
    { header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
    {
      header: 'Actions',
      render: (i) => (
        <div className="flex gap-1">
          {i.status !== 'Completed' && (
            <button onClick={() => complete(i)} title="Mark complete" className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50">
              <CheckCircle2 className="size-4" />
            </button>
          )}
          <button onClick={() => remove(i)} title="Delete" className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
            <Trash2 className="size-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2">
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value as 'All' | ComplianceCategory)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
            <option value="All">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as 'All' | ComplianceStatus)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
            <option value="All">All statuses</option>
            <option value="Pending">Pending</option>
            <option value="Completed">Completed</option>
            <option value="Overdue">Overdue</option>
          </select>
        </div>
        <Button onClick={openAdd}>
          <Plus className="size-4" /> Add Compliance Item
        </Button>
      </div>

      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<ShieldCheck className="size-5" />} title="No compliance items" message="Add a compliance requirement to start tracking it." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(i) => i.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Add Compliance Item"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Add Item</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Select label="Client" required value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.businessName}
              </option>
            ))}
          </Select>
          <Input label="Requirement Title" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <div className="grid grid-cols-2 gap-3">
            <Select label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as ComplianceCategory })}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
            <Select label="Priority" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as Priority })}>
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Due Date" type="date" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
            <Input label="Assigned To" value={form.assignedTo} onChange={(e) => setForm({ ...form, assignedTo: e.target.value })} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
