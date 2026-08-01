import { useMemo } from 'react';
import { FileText, ShieldAlert, Wallet, FolderOpen } from 'lucide-react';
import { StatCard } from '../../components/ui/StatCard';
import { Card, CardHeader } from '../../components/ui/Card';
import { StatusBadge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { invoicesStore, complianceStore, documentsStore } from '../../lib/entities';
import { formatCurrency, formatDate, invoiceTotal, daysUntil } from '../../lib/format';

export function ClientDashboard() {
  const { user } = useAuth();
  const invoices = invoicesStore.useAll().filter((i) => i.clientId === user?.clientId);
  const compliance = complianceStore.useAll().filter((c) => c.clientId === user?.clientId);
  const documents = documentsStore.useAll().filter((d) => d.clientId === user?.clientId);

  const outstanding = invoices.reduce((s, i) => (i.status === 'Paid' ? s : s + (invoiceTotal(i.items) - i.amountPaid)), 0);
  const pendingCompliance = compliance.filter((c) => c.status !== 'Completed');

  const upcoming = useMemo(() => [...pendingCompliance].sort((a, b) => (a.dueDate < b.dueDate ? -1 : 1)).slice(0, 5), [pendingCompliance]);
  const recentInvoices = useMemo(() => [...invoices].sort((a, b) => (a.issueDate < b.issueDate ? 1 : -1)).slice(0, 5), [invoices]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Outstanding Amount" value={formatCurrency(outstanding)} icon={<Wallet className="size-4" />} tone="amber" />
        <StatCard label="Total Invoices" value={invoices.length} icon={<FileText className="size-4" />} tone="indigo" />
        <StatCard label="Pending Compliance" value={pendingCompliance.length} icon={<ShieldAlert className="size-4" />} tone="rose" />
        <StatCard label="Documents Shared" value={documents.length} icon={<FolderOpen className="size-4" />} tone="sky" />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Upcoming Compliance" subtitle="Your deadlines" />
          {upcoming.length === 0 ? (
            <EmptyState icon={<ShieldAlert className="size-5" />} title="Nothing due" message="You're all caught up on compliance." />
          ) : (
            <ul className="divide-y divide-slate-50 p-2">
              {upcoming.map((c) => {
                const days = daysUntil(c.dueDate);
                return (
                  <li key={c.id} className="flex items-center justify-between gap-2 px-2 py-2.5">
                    <div>
                      <p className="text-sm font-medium text-slate-800">{c.title}</p>
                      <p className="text-xs text-slate-400">{c.category}</p>
                    </div>
                    <div className="text-right">
                      <StatusBadge status={c.status} />
                      <p className="mt-1 text-[11px] text-slate-400">{days < 0 ? `${Math.abs(days)}d overdue` : days === 0 ? 'Due today' : `in ${days}d`}</p>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader title="Recent Invoices" subtitle="Your billing activity" />
          {recentInvoices.length === 0 ? (
            <EmptyState icon={<FileText className="size-5" />} title="No invoices yet" message="Invoices from your consultant will appear here." />
          ) : (
            <ul className="divide-y divide-slate-50 p-2">
              {recentInvoices.map((i) => (
                <li key={i.id} className="flex items-center justify-between gap-2 px-2 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-slate-800">{i.invoiceNumber}</p>
                    <p className="text-xs text-slate-400">Due {formatDate(i.dueDate)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium text-slate-900">{formatCurrency(invoiceTotal(i.items))}</p>
                    <StatusBadge status={i.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
