import { useState } from 'react';
import { Plus, Briefcase, Trash2 } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Textarea } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { servicesStore, logAudit } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatCurrency } from '../../lib/format';
import type { ServiceItem } from '../../types';

export function AdminServices() {
  const { user } = useAuth();
  const { show } = useToast();
  const services = servicesStore.useAll();

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ name: '', category: '', description: '', price: 0 });

  const openAdd = () => {
    setForm({ name: '', category: '', description: '', price: 0 });
    setModalOpen(true);
  };

  const save = () => {
    if (!form.name || !form.category) {
      show('Please fill in the service name and category.', 'error');
      return;
    }
    const id = genId('svc');
    servicesStore.add({ id, ...form, active: true });
    logAudit(user?.name ?? 'Admin', 'admin', 'Added service', 'Service', id, `Added "${form.name}" to the service catalog`);
    show('Service added.', 'success');
    setModalOpen(false);
  };

  const toggleActive = (s: ServiceItem) => servicesStore.update(s.id, { active: !s.active });
  const remove = (s: ServiceItem) => {
    if (!confirm(`Remove service "${s.name}"?`)) return;
    servicesStore.remove(s.id);
    show('Service removed.', 'success');
  };

  const columns: Column<ServiceItem>[] = [
    { header: 'Service', render: (s) => <span className="font-medium text-slate-900">{s.name}</span> },
    { header: 'Category', render: (s) => <Badge tone="blue">{s.category}</Badge> },
    { header: 'Description', render: (s) => <span className="text-slate-500">{s.description}</span> },
    { header: 'Price', render: (s) => formatCurrency(s.price) },
    {
      header: 'Active',
      render: (s) => (
        <button onClick={() => toggleActive(s)}>
          <Badge tone={s.active ? 'green' : 'slate'}>{s.active ? 'Active' : 'Inactive'}</Badge>
        </button>
      ),
    },
    {
      header: 'Actions',
      render: (s) => (
        <button onClick={() => remove(s)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
          <Trash2 className="size-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={openAdd}>
          <Plus className="size-4" /> Add Service
        </Button>
      </div>
      <Card>
        {services.length === 0 ? (
          <EmptyState icon={<Briefcase className="size-5" />} title="No services" message="Add services your firm offers to clients." />
        ) : (
          <Table columns={columns} rows={services} keyFor={(s) => s.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Add Service"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Add Service</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Input label="Service Name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input label="Category" required value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
          <Textarea label="Description" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <Input label="Price (INR)" type="number" min={0} value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />
        </div>
      </Modal>
    </div>
  );
}
