import { useMemo, useState } from 'react';
import { Plus, AlarmClock, Trash2, CheckCircle2 } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge, Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { remindersStore, clientsStore, logAudit } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { Reminder, ReminderFrequency } from '../../types';

const FREQUENCIES: ReminderFrequency[] = ['Once', 'Daily', 'Weekly', 'Monthly'];
const CHANNELS: Reminder['channel'][] = ['Email', 'SMS', 'In-App'];

export function AdminReminders() {
  const { user } = useAuth();
  const { show } = useToast();
  const reminders = remindersStore.useAll();
  const clients = clientsStore.useAll();
  const sorted = useMemo(() => [...reminders].sort((a, b) => (a.dueDate < b.dueDate ? -1 : 1)), [reminders]);

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    clientId: '',
    title: '',
    dueDate: new Date().toISOString().slice(0, 10),
    frequency: 'Once' as ReminderFrequency,
    channel: 'Email' as Reminder['channel'],
  });

  const openAdd = () => {
    setForm({ clientId: '', title: '', dueDate: new Date().toISOString().slice(0, 10), frequency: 'Once', channel: 'Email' });
    setModalOpen(true);
  };

  const save = () => {
    if (!form.title) {
      show('Please add a title for the reminder.', 'error');
      return;
    }
    const client = clients.find((c) => c.id === form.clientId);
    const id = genId('rem');
    remindersStore.add({
      id,
      clientId: client?.id,
      clientName: client?.businessName,
      title: form.title,
      dueDate: new Date(form.dueDate).toISOString(),
      frequency: form.frequency,
      channel: form.channel,
      status: 'Active',
    });
    logAudit(user?.name ?? 'Admin', 'admin', 'Created reminder', 'Reminder', id, `Created "${form.title}"`);
    show('Reminder created.', 'success');
    setModalOpen(false);
  };

  const complete = (r: Reminder) => remindersStore.update(r.id, { status: 'Completed' });
  const remove = (r: Reminder) => {
    if (!confirm(`Delete reminder "${r.title}"?`)) return;
    remindersStore.remove(r.id);
    show('Reminder deleted.', 'success');
  };

  const columns: Column<Reminder>[] = [
    { header: 'Title', render: (r) => <span className="font-medium text-slate-900">{r.title}</span> },
    { header: 'Client', render: (r) => r.clientName ?? <span className="text-slate-400">All clients</span> },
    { header: 'Due Date', render: (r) => formatDate(r.dueDate) },
    { header: 'Frequency', render: (r) => <Badge>{r.frequency}</Badge> },
    { header: 'Channel', render: (r) => <Badge tone="blue">{r.channel}</Badge> },
    { header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    {
      header: 'Actions',
      render: (r) => (
        <div className="flex gap-1">
          {r.status === 'Active' && (
            <button onClick={() => complete(r)} className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50" title="Mark complete">
              <CheckCircle2 className="size-4" />
            </button>
          )}
          <button onClick={() => remove(r)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50" title="Delete">
            <Trash2 className="size-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={openAdd}>
          <Plus className="size-4" /> New Reminder
        </Button>
      </div>
      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<AlarmClock className="size-5" />} title="No reminders" message="Create reminders for filing deadlines and follow-ups." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(r) => r.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="New Reminder"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Create Reminder</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Input label="Title" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <Select label="Client (optional)" value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            <option value="">All clients</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.businessName}
              </option>
            ))}
          </Select>
          <Input label="Due Date" type="date" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
          <div className="grid grid-cols-2 gap-3">
            <Select label="Frequency" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value as ReminderFrequency })}>
              {FREQUENCIES.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </Select>
            <Select label="Channel" value={form.channel} onChange={(e) => setForm({ ...form, channel: e.target.value as Reminder['channel'] })}>
              {CHANNELS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </div>
        </div>
      </Modal>
    </div>
  );
}
