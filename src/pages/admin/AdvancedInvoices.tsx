import { useMemo } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardHeader } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { invoicesStore, clientsStore } from '../../lib/entities';
import { formatCurrency, invoiceTotal, daysUntil } from '../../lib/format';
import type { Invoice } from '../../types';

const COLORS = ['#4f46e5', '#f59e0b', '#10b981', '#f43f5e'];

interface ClientSummary {
  clientId: string;
  clientName: string;
  invoiceCount: number;
  totalBilled: number;
  totalPaid: number;
  outstanding: number;
}

function agingBucket(inv: Invoice): '0-30' | '31-60' | '61-90' | '90+' {
  if (inv.status === 'Paid') return '0-30';
  const overdueDays = Math.max(0, -daysUntil(inv.dueDate));
  if (overdueDays <= 30) return '0-30';
  if (overdueDays <= 60) return '31-60';
  if (overdueDays <= 90) return '61-90';
  return '90+';
}

export function AdminAdvancedInvoices() {
  const invoices = invoicesStore.useAll();
  const clients = clientsStore.useAll();

  const statusBreakdown = useMemo(() => {
    const counts: Record<string, number> = {};
    invoices.forEach((i) => {
      counts[i.status] = (counts[i.status] ?? 0) + invoiceTotal(i.items);
    });
    return Object.entries(counts).map(([name, value]) => ({ name, value }));
  }, [invoices]);

  const agingBuckets = useMemo(() => {
    const buckets: Record<string, number> = { '0-30': 0, '31-60': 0, '61-90': 0, '90+': 0 };
    invoices
      .filter((i) => i.status !== 'Paid')
      .forEach((i) => {
        buckets[agingBucket(i)] += invoiceTotal(i.items) - i.amountPaid;
      });
    return buckets;
  }, [invoices]);

  const clientSummaries: ClientSummary[] = useMemo(() => {
    return clients
      .map((c) => {
        const clientInvoices = invoices.filter((i) => i.clientId === c.id);
        const totalBilled = clientInvoices.reduce((s, i) => s + invoiceTotal(i.items), 0);
        const totalPaid = clientInvoices.reduce((s, i) => s + i.amountPaid, 0);
        return {
          clientId: c.id,
          clientName: c.businessName,
          invoiceCount: clientInvoices.length,
          totalBilled,
          totalPaid,
          outstanding: totalBilled - totalPaid,
        };
      })
      .filter((s) => s.invoiceCount > 0)
      .sort((a, b) => b.outstanding - a.outstanding);
  }, [clients, invoices]);

  const columns: Column<ClientSummary>[] = [
    { header: 'Client', render: (s) => <span className="font-medium text-slate-900">{s.clientName}</span> },
    { header: 'Invoices', render: (s) => s.invoiceCount },
    { header: 'Total Billed', render: (s) => formatCurrency(s.totalBilled) },
    { header: 'Total Paid', render: (s) => formatCurrency(s.totalPaid) },
    {
      header: 'Outstanding',
      render: (s) => <span className={s.outstanding > 0 ? 'font-medium text-rose-600' : 'text-slate-500'}>{formatCurrency(s.outstanding)}</span>,
    },
  ];

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader title="Invoice Status Split" subtitle="By billed amount" />
          <div className="h-64 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={statusBreakdown} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                  {statusBreakdown.map((_, idx) => (
                    <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Aging Report" subtitle="Outstanding balance by days overdue" />
          <div className="grid grid-cols-2 gap-3 p-4 sm:grid-cols-4">
            {Object.entries(agingBuckets).map(([bucket, amount]) => (
              <div key={bucket} className="rounded-lg bg-slate-50 p-3 text-center">
                <p className="text-xs text-slate-500">{bucket} days</p>
                <p className="mt-1 text-lg font-semibold text-slate-900">{formatCurrency(amount)}</p>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card>
        <CardHeader title="Client Billing Summary" subtitle="Advanced invoice management by client" />
        <Table columns={columns} rows={clientSummaries} keyFor={(s) => s.clientId} emptyMessage="No billing data yet." />
      </Card>
    </div>
  );
}
