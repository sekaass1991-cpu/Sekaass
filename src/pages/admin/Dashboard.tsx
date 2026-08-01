import { useMemo } from 'react';
import { Users, FileText, ShieldAlert, Wallet } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { StatCard } from '../../components/ui/StatCard';
import { Card, CardHeader } from '../../components/ui/Card';
import { StatusBadge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { clientsStore, invoicesStore, complianceStore, auditStore } from '../../lib/entities';
import { formatCurrency, formatDateTime, invoiceTotal, daysUntil } from '../../lib/format';
import { History } from 'lucide-react';

export function AdminDashboard() {
  const clients = clientsStore.useAll();
  const invoices = invoicesStore.useAll();
  const compliance = complianceStore.useAll();
  const audit = auditStore.useAll();

  const activeClients = clients.filter((c) => c.status === 'Active').length;
  const outstanding = invoices.reduce((sum, inv) => {
    const total = invoiceTotal(inv.items);
    return inv.status === 'Paid' ? sum : sum + (total - inv.amountPaid);
  }, 0);
  const overdueInvoices = invoices.filter((i) => i.status === 'Overdue').length;
  const pendingCompliance = compliance.filter((c) => c.status !== 'Completed').length;

  const chartData = useMemo(() => {
    const months: { month: string; revenue: number }[] = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const label = d.toLocaleDateString('en-IN', { month: 'short' });
      const monthRevenue = invoices
        .filter((inv) => {
          const issue = new Date(inv.issueDate);
          return issue.getFullYear() === d.getFullYear() && issue.getMonth() === d.getMonth();
        })
        .reduce((sum, inv) => sum + invoiceTotal(inv.items), 0);
      months.push({ month: label, revenue: monthRevenue });
    }
    return months;
  }, [invoices]);

  const upcomingCompliance = compliance
    .filter((c) => c.status !== 'Completed')
    .sort((a, b) => (a.dueDate < b.dueDate ? -1 : 1))
    .slice(0, 5);

  const recentActivity = [...audit].sort((a, b) => (a.timestamp < b.timestamp ? 1 : -1)).slice(0, 6);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Active Clients" value={activeClients} icon={<Users className="size-4" />} tone="indigo" hint={`${clients.length} total clients`} />
        <StatCard label="Outstanding Amount" value={formatCurrency(outstanding)} icon={<Wallet className="size-4" />} tone="amber" hint="Across pending & overdue invoices" />
        <StatCard label="Overdue Invoices" value={overdueInvoices} icon={<FileText className="size-4" />} tone="rose" hint="Need follow-up" />
        <StatCard label="Active Compliance Cases" value={pendingCompliance} icon={<ShieldAlert className="size-4" />} tone="sky" hint="Pending or overdue" />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Revenue (last 6 months)" subtitle="Based on invoices issued" />
          <div className="h-64 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="month" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v / 1000}k`} />
                <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                <Bar dataKey="revenue" fill="#4f46e5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <CardHeader title="Upcoming Compliance" subtitle="Next deadlines" />
          {upcomingCompliance.length === 0 ? (
            <EmptyState icon={<ShieldAlert className="size-5" />} title="Nothing due" message="All compliance items are completed." />
          ) : (
            <ul className="divide-y divide-slate-50 p-2">
              {upcomingCompliance.map((c) => {
                const days = daysUntil(c.dueDate);
                return (
                  <li key={c.id} className="flex items-center justify-between gap-2 px-2 py-2.5">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-slate-800">{c.title}</p>
                      <p className="truncate text-xs text-slate-400">{c.clientName}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <StatusBadge status={c.status} />
                      <p className="mt-1 text-[11px] text-slate-400">
                        {days < 0 ? `${Math.abs(days)}d overdue` : days === 0 ? 'Due today' : `in ${days}d`}
                      </p>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      </div>

      <Card>
        <CardHeader title="Recent Activity" subtitle="Latest actions across the firm" />
        {recentActivity.length === 0 ? (
          <EmptyState icon={<History className="size-5" />} title="No activity yet" message="Actions will appear here as they happen." />
        ) : (
          <ul className="divide-y divide-slate-50 p-2">
            {recentActivity.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-2 px-2 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-sm text-slate-700">
                    <span className="font-medium text-slate-900">{a.actor}</span> {a.action.toLowerCase()}
                  </p>
                  <p className="truncate text-xs text-slate-400">{a.details}</p>
                </div>
                <p className="shrink-0 text-xs text-slate-400">{formatDateTime(a.timestamp)}</p>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
