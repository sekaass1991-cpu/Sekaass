import { useMemo, useState } from 'react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Select } from '../../components/ui/Field';
import { Table, type Column } from '../../components/ui/Table';
import { EmptyState } from '../../components/ui/EmptyState';
import { FileText } from 'lucide-react';
import { clientsStore, invoicesStore, ledgerStore } from '../../lib/entities';
import { formatCurrency, formatDate, invoiceTotal } from '../../lib/format';

interface StatementRow {
  id: string;
  date: string;
  description: string;
  debit: number;
  credit: number;
  balance: number;
}

export function AdminStatements() {
  const clients = clientsStore.useAll();
  const invoices = invoicesStore.useAll();
  const ledger = ledgerStore.useAll();
  const [clientId, setClientId] = useState(clients[0]?.id ?? '');

  const client = clients.find((c) => c.id === clientId);

  const rows = useMemo(() => {
    if (!client) return [];
    type RawRow = { date: string; description: string; debit: number; credit: number };
    const raw: RawRow[] = [];

    invoices
      .filter((i) => i.clientId === client.id)
      .forEach((i) => {
        raw.push({ date: i.issueDate, description: `Invoice ${i.invoiceNumber} issued`, debit: invoiceTotal(i.items), credit: 0 });
        if (i.amountPaid > 0) {
          raw.push({ date: i.issueDate, description: `Payment received for ${i.invoiceNumber}`, debit: 0, credit: i.amountPaid });
        }
      });

    ledger
      .filter((l) => l.clientId === client.id)
      .forEach((l) => {
        raw.push({
          date: l.date,
          description: l.particulars,
          debit: l.type === 'Debit' ? l.amount : 0,
          credit: l.type === 'Credit' ? l.amount : 0,
        });
      });

    raw.sort((a, b) => (a.date < b.date ? -1 : 1));

    let balance = 0;
    const result: StatementRow[] = raw.map((r, idx) => {
      balance += r.debit - r.credit;
      return { id: `${idx}`, ...r, balance };
    });
    return result;
  }, [client, invoices, ledger]);

  const columns: Column<StatementRow>[] = [
    { header: 'Date', render: (r) => formatDate(r.date) },
    { header: 'Description', render: (r) => r.description },
    { header: 'Debit', render: (r) => (r.debit ? formatCurrency(r.debit) : '-') },
    { header: 'Credit', render: (r) => (r.credit ? formatCurrency(r.credit) : '-') },
    { header: 'Balance', render: (r) => <span className="font-medium">{formatCurrency(r.balance)}</span> },
  ];

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader
          title="Client Statement"
          subtitle="Combined invoice and ledger activity"
          action={
            <div className="w-56">
              <Select value={clientId} onChange={(e) => setClientId(e.target.value)}>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.businessName}
                  </option>
                ))}
              </Select>
            </div>
          }
        />
        {rows.length === 0 ? (
          <EmptyState icon={<FileText className="size-5" />} title="No transactions" message="This client has no invoices or ledger entries yet." />
        ) : (
          <Table columns={columns} rows={rows} keyFor={(r) => r.id} />
        )}
      </Card>
    </div>
  );
}
