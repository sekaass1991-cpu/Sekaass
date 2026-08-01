import { useMemo, useState } from 'react';
import { Plus, Search, Trash2, Pencil, Users } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select, Textarea } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { clientsStore, logAudit } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { Client, ClientStatus, EntityType } from '../../types';

const ENTITY_TYPES: EntityType[] = ['Individual', 'Proprietorship', 'Partnership', 'LLP', 'Private Limited', 'Public Limited', 'Artificial Juridical Person'];
const STATUSES: ClientStatus[] = ['Active', 'Pending', 'Inactive'];

const EMPTY_FORM = {
  name: '',
  businessName: '',
  email: '',
  phone: '',
  pan: '',
  gstin: '',
  entityType: 'Individual' as EntityType,
  category: '',
  status: 'Pending' as ClientStatus,
  assignedConsultant: '',
  address: '',
  notes: '',
};

export function AdminClients() {
  const { user } = useAuth();
  const { show } = useToast();
  const clients = clientsStore.useAll();
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'All' | ClientStatus>('All');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Client | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const filtered = useMemo(() => {
    return clients.filter((c) => {
      const matchesQuery =
        !query ||
        c.name.toLowerCase().includes(query.toLowerCase()) ||
        c.businessName.toLowerCase().includes(query.toLowerCase()) ||
        c.email.toLowerCase().includes(query.toLowerCase());
      const matchesStatus = statusFilter === 'All' || c.status === statusFilter;
      return matchesQuery && matchesStatus;
    });
  }, [clients, query, statusFilter]);

  const openAdd = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (client: Client) => {
    setEditing(client);
    setForm({
      name: client.name,
      businessName: client.businessName,
      email: client.email,
      phone: client.phone,
      pan: client.pan ?? '',
      gstin: client.gstin ?? '',
      entityType: client.entityType,
      category: client.category,
      status: client.status,
      assignedConsultant: client.assignedConsultant,
      address: client.address ?? '',
      notes: client.notes ?? '',
    });
    setModalOpen(true);
  };

  const save = () => {
    if (!form.name || !form.businessName || !form.email) {
      show('Please fill in the required fields.', 'error');
      return;
    }
    if (editing) {
      clientsStore.update(editing.id, form);
      logAudit(user?.name ?? 'Admin', 'admin', 'Updated client', 'Client', editing.id, `Updated ${form.businessName}`);
      show('Client updated.', 'success');
    } else {
      const id = genId('client');
      clientsStore.add({ id, onboardedDate: new Date().toISOString(), ...form });
      logAudit(user?.name ?? 'Admin', 'admin', 'Created client', 'Client', id, `Added client form for ${form.businessName}`);
      show('Client added.', 'success');
    }
    setModalOpen(false);
  };

  const remove = (client: Client) => {
    if (!confirm(`Remove ${client.businessName}? This cannot be undone.`)) return;
    clientsStore.remove(client.id);
    logAudit(user?.name ?? 'Admin', 'admin', 'Deleted client', 'Client', client.id, `Removed ${client.businessName}`);
    show('Client removed.', 'success');
  };

  const columns: Column<Client>[] = [
    {
      header: 'Client',
      render: (c) => (
        <div>
          <p className="font-medium text-slate-900">{c.businessName}</p>
          <p className="text-xs text-slate-400">{c.name}</p>
        </div>
      ),
    },
    { header: 'Entity Type', render: (c) => c.entityType },
    { header: 'Category', render: (c) => c.category },
    { header: 'Consultant', render: (c) => c.assignedConsultant },
    { header: 'Onboarded', render: (c) => formatDate(c.onboardedDate) },
    { header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
    {
      header: 'Actions',
      render: (c) => (
        <div className="flex gap-1">
          <button onClick={() => openEdit(c)} className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100">
            <Pencil className="size-4" />
          </button>
          <button onClick={() => remove(c)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
            <Trash2 className="size-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="relative w-full max-w-xs">
          <Search className="absolute left-3 top-2.5 size-4 text-slate-400" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search clients..."
            className="w-full rounded-lg border border-slate-300 py-2 pl-9 pr-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'All' | ClientStatus)}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="All">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <Button onClick={openAdd}>
            <Plus className="size-4" /> Add Client
          </Button>
        </div>
      </div>

      <Card>
        {filtered.length === 0 ? (
          <EmptyState icon={<Users className="size-5" />} title="No clients found" message="Try adjusting your search or add a new client." />
        ) : (
          <Table columns={columns} rows={filtered} keyFor={(c) => c.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Edit Client' : 'Add New Client'}
        wide
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>{editing ? 'Save Changes' : 'Add Client'}</Button>
          </>
        }
      >
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Input label="Contact Name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input label="Business Name" required value={form.businessName} onChange={(e) => setForm({ ...form, businessName: e.target.value })} />
          <Input label="Email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input label="Phone" required value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <Input label="PAN" value={form.pan} onChange={(e) => setForm({ ...form, pan: e.target.value })} />
          <Input label="GSTIN" value={form.gstin} onChange={(e) => setForm({ ...form, gstin: e.target.value })} />
          <Select label="Entity Type" value={form.entityType} onChange={(e) => setForm({ ...form, entityType: e.target.value as EntityType })}>
            {ENTITY_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </Select>
          <Input label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
          <Input label="Assigned Consultant" value={form.assignedConsultant} onChange={(e) => setForm({ ...form, assignedConsultant: e.target.value })} />
          <Select label="Status" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as ClientStatus })}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
          <div className="sm:col-span-2">
            <Input label="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </div>
          <div className="sm:col-span-2">
            <Textarea label="Notes" rows={3} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
