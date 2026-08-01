import { useState } from 'react';
import { Plus, KeyRound, Trash2, Eye, EyeOff } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select, Textarea } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { credentialsStore, clientsStore, logAudit } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { Credential } from '../../types';

export function AdminCredentials() {
  const { user } = useAuth();
  const { show } = useToast();
  const credentials = credentialsStore.useAll();
  const clients = clientsStore.useAll();

  const [modalOpen, setModalOpen] = useState(false);
  const [revealed, setRevealed] = useState<Set<string>>(new Set());
  const [form, setForm] = useState({ clientId: '', portalName: '', username: '', password: '', notes: '' });

  const toggleReveal = (id: string) => {
    setRevealed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const openAdd = () => {
    setForm({ clientId: '', portalName: '', username: '', password: '', notes: '' });
    setModalOpen(true);
  };

  const save = () => {
    if (!form.portalName || !form.username || !form.password) {
      show('Please fill in portal, username, and password.', 'error');
      return;
    }
    const client = clients.find((c) => c.id === form.clientId);
    const id = genId('cred');
    credentialsStore.add({ id, ...form, clientId: client?.id, clientName: client?.businessName, updatedAt: new Date().toISOString() });
    logAudit(user?.name ?? 'Admin', 'admin', 'Added credential', 'Credential', id, `Added portal account for ${form.portalName}`);
    show('Credential saved.', 'success');
    setModalOpen(false);
  };

  const remove = (c: Credential) => {
    if (!confirm(`Delete credential for ${c.portalName}?`)) return;
    credentialsStore.remove(c.id);
    show('Credential removed.', 'success');
  };

  const columns: Column<Credential>[] = [
    { header: 'Portal', render: (c) => <span className="font-medium text-slate-900">{c.portalName}</span> },
    { header: 'Client', render: (c) => c.clientName ?? <span className="text-slate-400">Firm-level</span> },
    { header: 'Username', render: (c) => c.username },
    {
      header: 'Password',
      render: (c) => (
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs">{revealed.has(c.id) ? c.password : '••••••••'}</span>
          <button onClick={() => toggleReveal(c.id)} className="text-slate-400 hover:text-slate-600">
            {revealed.has(c.id) ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      ),
    },
    { header: 'Updated', render: (c) => formatDate(c.updatedAt) },
    {
      header: 'Actions',
      render: (c) => (
        <button onClick={() => remove(c)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
          <Trash2 className="size-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-between">
        <p className="text-sm text-slate-500">Add New Portal Account credentials for government and third-party portals.</p>
        <Button onClick={openAdd}>
          <Plus className="size-4" /> Add Credential
        </Button>
      </div>
      <Card>
        {credentials.length === 0 ? (
          <EmptyState icon={<KeyRound className="size-5" />} title="No credentials stored" message="Add portal credentials to keep them organized per client." />
        ) : (
          <Table columns={columns} rows={credentials} keyFor={(c) => c.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Add Credential"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Save Credential</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Select label="Client (optional)" value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            <option value="">Firm-level</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.businessName}
              </option>
            ))}
          </Select>
          <Input label="Portal Name" required value={form.portalName} onChange={(e) => setForm({ ...form, portalName: e.target.value })} />
          <Input label="Username" required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
          <Input label="Password" type="text" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <Textarea label="Notes" rows={2} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
        </div>
      </Modal>
    </div>
  );
}
