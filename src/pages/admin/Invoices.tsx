import { useMemo, useState } from 'react';
import { Plus, Trash2, FileText, CheckCircle2 } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { invoicesStore, clientsStore, logAudit, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatCurrency, formatDate, invoiceTotal } from '../../lib/format';
import type { Invoice, InvoiceItem, InvoiceStatus } from '../../types';

const STATUSES: InvoiceStatus[] = ['Draft', 'Pending', 'Paid', 'Overdue'];

function emptyItem(): InvoiceItem {
  return { id: genId('item'), description: '', qty: 1, rate: 0 };
}

export function AdminInvoices() {
  const { user } = useAuth();
  const { show } = useToast();
  const invoices = invoicesStore.useAll();
  const clients = clientsStore.useAll();

  const [statusFilter, setStatusFilter] = useState<'All' | InvoiceStatus>('All');
  const [modalOpen, setModalOpen] = useState(false);
  const [clientId, setClientId] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [items, setItems] = useState<InvoiceItem[]>([emptyItem()]);

  const filtered = useMemo(
    () => invoices.filter((i) => statusFilter === 'All' || i.status === statusFilter),
    [invoices, statusFilter],
  );

  const sorted = useMemo(() => [...filtered].sort((a, b) => (a.issueDate < b.issueDate ? 1 : -1)), [filtered]);

  const openAdd = () => {
    setClientId(clients[0]?.id ?? '');
    setDueDate(new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10));
    setItems([emptyItem()]);
    setModalOpen(true);
  };

  const updateItem = (id: string, patch: Partial<InvoiceItem>) => {
    setItems((prev) => prev.map((it) => (it.id === id ? { ...it, ...patch } : it)));
  };

  const total = invoiceTotal(items);

  const save = () => {
    const client = clients.find((c) => c.id === clientId);
    if (!client) {
      show('Select a client first.', 'error');
      return;
    }
    if (items.some((i) => !i.description || i.qty <= 0)) {
      show('Every line item needs a description and quantity.', 'error');
      return;
    }
    const id = genId('inv');
    const invoiceNumber = `INV-${new Date().getFullYear()}-${String(invoices.length + 1).padStart(3, '0')}`;
    const invoice: Invoice = {
      id,
      invoiceNumber,
      clientId: client.id,
      clientName: client.businessName,
      issueDate: new Date().toISOString(),
      dueDate: new Date(dueDate).toISOString(),
      items,
      amountPaid: 0,
      status: 'Pending',
    };
    invoicesStore.add(invoice);
    logAudit(user?.name ?? 'Admin', 'admin', 'Generated invoice', 'Invoice', id, `Created ${invoiceNumber} for ${client.businessName}`);
    pushNotification('client', 'New invoice', `Invoice ${invoiceNumber} for ${formatCurrency(invoiceTotal(items))} has been generated.`, 'info', client.id, true);
    show('Invoice created.', 'success');
    setModalOpen(false);
  };

  const markPaid = (inv: Invoice) => {
    const total = invoiceTotal(inv.items);
    invoicesStore.update(inv.id, { status: 'Paid', amountPaid: total });
    logAudit(user?.name ?? 'Admin', 'admin', 'Marked invoice paid', 'Invoice', inv.id, `${inv.invoiceNumber} marked as paid`);
    show('Invoice marked as paid.', 'success');
  };

  const remove = (inv: Invoice) => {
    if (!confirm(`Are you sure you want to delete invoice ${inv.invoiceNumber}?`)) return;
    invoicesStore.remove(inv.id);
    logAudit(user?.name ?? 'Admin', 'admin', 'Deleted invoice', 'Invoice', inv.id, `Deleted ${inv.invoiceNumber}`);
    show('Invoice deleted.', 'success');
  };

  const columns: Column<Invoice>[] = [
    { header: 'Invoice #', render: (i) => <span className="font-medium text-slate-900">{i.invoiceNumber}</span> },
    { header: 'Client', render: (i) => i.clientName },
    { header: 'Issue Date', render: (i) => formatDate(i.issueDate) },
    { header: 'Due Date', render: (i) => formatDate(i.dueDate) },
    { header: 'Amount', render: (i) => formatCurrency(invoiceTotal(i.items)) },
    { header: 'Paid', render: (i) => formatCurrency(i.amountPaid) },
    { header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
    {
      header: 'Actions',
      render: (i) => (
        <div className="flex gap-1">
          {i.status !== 'Paid' && (
            <button onClick={() => markPaid(i)} title="Mark as paid" className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50">
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
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as 'All' | InvoiceStatus)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="All">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <Button onClick={openAdd}>
          <Plus className="size-4" /> New Invoice
        </Button>
      </div>

      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<FileText className="size-5" />} title="No invoices" message="Create your first invoice to get started." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(i) => i.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="New Invoice"
        wide
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Create Invoice</Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Select label="Client" required value={clientId} onChange={(e) => setClientId(e.target.value)}>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.businessName}
                </option>
              ))}
            </Select>
            <Input label="Due Date" type="date" required value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-medium text-slate-600">Line Items</span>
              <button onClick={() => setItems((prev) => [...prev, emptyItem()])} className="text-xs font-medium text-indigo-600 hover:underline">
                + Add item
              </button>
            </div>
            <div className="space-y-2">
              {items.map((item) => (
                <div key={item.id} className="grid grid-cols-12 gap-2">
                  <input
                    className="col-span-6 rounded-lg border border-slate-300 px-2 py-1.5 text-sm"
                    placeholder="Description"
                    value={item.description}
                    onChange={(e) => updateItem(item.id, { description: e.target.value })}
                  />
                  <input
                    type="number"
                    min={1}
                    className="col-span-2 rounded-lg border border-slate-300 px-2 py-1.5 text-sm"
                    placeholder="Qty"
                    value={item.qty}
                    onChange={(e) => updateItem(item.id, { qty: Number(e.target.value) })}
                  />
                  <input
                    type="number"
                    min={0}
                    className="col-span-3 rounded-lg border border-slate-300 px-2 py-1.5 text-sm"
                    placeholder="Rate"
                    value={item.rate}
                    onChange={(e) => updateItem(item.id, { rate: Number(e.target.value) })}
                  />
                  <button
                    onClick={() => setItems((prev) => prev.filter((i) => i.id !== item.id))}
                    className="col-span-1 flex items-center justify-center text-rose-500 hover:text-rose-700"
                    disabled={items.length === 1}
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end border-t border-slate-100 pt-3">
            <p className="text-sm font-semibold text-slate-900">Total: {formatCurrency(total)}</p>
          </div>
        </div>
      </Modal>
    </div>
  );
}
