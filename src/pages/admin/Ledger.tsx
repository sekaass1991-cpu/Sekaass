import { useMemo, useState } from 'react';
import { Plus, BookOpen, Trash2 } from 'lucide-react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { ledgerStore, clientsStore, logAudit } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatCurrency, formatDate } from '../../lib/format';
import type { LedgerAccount, LedgerEntry } from '../../types';

const ACCOUNTS: LedgerAccount[] = ['Revenue', 'Accounts Receivable', 'Accounts Payable', 'Administrative Expenses', 'Bank', 'Cash', 'Assets', 'Liabilities'];

export function AdminLedger() {
  const { user } = useAuth();
  const { show } = useToast();
  const ledger = ledgerStore.useAll();
  const clients = clientsStore.useAll();

  const [accountFilter, setAccountFilter] = useState<'All' | LedgerAccount>('All');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    clientId: '',
    particulars: '',
    account: 'Revenue' as LedgerAccount,
    type: 'Credit' as 'Debit' | 'Credit',
    amount: 0,
  });

  const filtered = useMemo(() => (accountFilter === 'All' ? ledger : ledger.filter((l) => l.account === accountFilter)), [ledger, accountFilter]);
  const sorted = useMemo(() => [...filtered].sort((a, b) => (a.date < b.date ? 1 : -1)), [filtered]);

  const totalDebit = filtered.reduce((s, l) => (l.type === 'Debit' ? s + l.amount : s), 0);
  const totalCredit = filtered.reduce((s, l) => (l.type === 'Credit' ? s + l.amount : s), 0);

  const openAdd = () => {
    setForm({ clientId: '', particulars: '', account: 'Revenue', type: 'Credit', amount: 0 });
    setModalOpen(true);
  };

  const save = () => {
    if (!form.particulars || form.amount <= 0) {
      show('Please fill in particulars and a valid amount.', 'error');
      return;
    }
    const client = clients.find((c) => c.id === form.clientId);
    const id = genId('led');
    const entry: LedgerEntry = {
      id,
      clientId: client?.id,
      clientName: client?.businessName,
      date: new Date().toISOString(),
      particulars: form.particulars,
      account: form.account,
      type: form.type,
      amount: form.amount,
    };
    ledgerStore.add(entry);
    logAudit(user?.name ?? 'Admin', 'admin', 'Added ledger entry', 'Ledger', id, `${form.type} of ${formatCurrency(form.amount)} to ${form.account}`);
    show('Ledger entry added.', 'success');
    setModalOpen(false);
  };

  const remove = (entry: LedgerEntry) => {
    if (!confirm('Remove this ledger entry?')) return;
    ledgerStore.remove(entry.id);
    show('Ledger entry removed.', 'success');
  };

  const columns: Column<LedgerEntry>[] = [
    { header: 'Date', render: (l) => formatDate(l.date) },
    { header: 'Particulars', render: (l) => l.particulars },
    { header: 'Client', render: (l) => l.clientName ?? <span className="text-slate-400">Firm-level</span> },
    { header: 'Account', render: (l) => <Badge tone="purple">{l.account}</Badge> },
    { header: 'Type', render: (l) => <Badge tone={l.type === 'Credit' ? 'green' : 'red'}>{l.type}</Badge> },
    { header: 'Amount', render: (l) => formatCurrency(l.amount) },
    {
      header: 'Actions',
      render: (l) => (
        <button onClick={() => remove(l)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
          <Trash2 className="size-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader title="Total Debit" />
          <p className="p-4 text-xl font-semibold text-rose-600">{formatCurrency(totalDebit)}</p>
        </Card>
        <Card>
          <CardHeader title="Total Credit" />
          <p className="p-4 text-xl font-semibold text-emerald-600">{formatCurrency(totalCredit)}</p>
        </Card>
        <Card>
          <CardHeader title="Net Balance" />
          <p className="p-4 text-xl font-semibold text-slate-900">{formatCurrency(totalCredit - totalDebit)}</p>
        </Card>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <select value={accountFilter} onChange={(e) => setAccountFilter(e.target.value as 'All' | LedgerAccount)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="All">All accounts</option>
          {ACCOUNTS.map((a) => (
            <option key={a} value={a}>
              {a}
            </option>
          ))}
        </select>
        <Button onClick={openAdd}>
          <Plus className="size-4" /> Add Entry
        </Button>
      </div>

      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<BookOpen className="size-5" />} title="No ledger entries" message="Add your first entry to start tracking finances." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(l) => l.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Add Ledger Entry"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Add Entry</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Select label="Client (optional)" value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            <option value="">Firm-level entry</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.businessName}
              </option>
            ))}
          </Select>
          <Input label="Particulars" required value={form.particulars} onChange={(e) => setForm({ ...form, particulars: e.target.value })} />
          <div className="grid grid-cols-3 gap-3">
            <Select label="Account" value={form.account} onChange={(e) => setForm({ ...form, account: e.target.value as LedgerAccount })} className="col-span-2">
              {ACCOUNTS.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </Select>
            <Select label="Type" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as 'Debit' | 'Credit' })}>
              <option value="Debit">Debit</option>
              <option value="Credit">Credit</option>
            </Select>
          </div>
          <Input label="Amount" type="number" min={0} required value={form.amount} onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })} />
        </div>
      </Modal>
    </div>
  );
}
