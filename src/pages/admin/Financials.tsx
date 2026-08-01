import { useMemo } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardHeader } from '../../components/ui/Card';
import { ledgerStore, invoicesStore } from '../../lib/entities';
import { formatCurrency, invoiceTotal } from '../../lib/format';
import type { LedgerAccount } from '../../types';

const COLORS = ['#4f46e5', '#10b981', '#f59e0b', '#f43f5e', '#0ea5e9', '#8b5cf6'];

function sumByAccount(entries: { account: LedgerAccount; type: 'Debit' | 'Credit'; amount: number }[], account: LedgerAccount) {
  return entries
    .filter((e) => e.account === account)
    .reduce((sum, e) => sum + (e.type === 'Credit' ? e.amount : -e.amount), 0);
}

export function AdminFinancials() {
  const ledger = ledgerStore.useAll();
  const invoices = invoicesStore.useAll();

  const revenue = useMemo(() => sumByAccount(ledger, 'Revenue') + invoices.reduce((s, i) => s + (i.status === 'Paid' ? invoiceTotal(i.items) : 0), 0), [ledger, invoices]);
  const adminExpenses = useMemo(() => Math.abs(sumByAccount(ledger, 'Administrative Expenses')), [ledger]);
  const netAfterExpenses = revenue - adminExpenses;

  const currentAssets = useMemo(() => {
    const bank = sumByAccount(ledger, 'Bank');
    const cash = sumByAccount(ledger, 'Cash');
    const receivable = invoices.reduce((s, i) => s + (invoiceTotal(i.items) - i.amountPaid), 0);
    return { bank, cash, receivable, total: bank + cash + receivable };
  }, [ledger, invoices]);

  const liabilities = useMemo(() => Math.abs(sumByAccount(ledger, 'Accounts Payable')) + Math.abs(sumByAccount(ledger, 'Liabilities')), [ledger]);

  const expenseBreakdown = useMemo(() => {
    const byParticular = new Map<string, number>();
    ledger
      .filter((l) => l.account === 'Administrative Expenses')
      .forEach((l) => byParticular.set(l.particulars, (byParticular.get(l.particulars) ?? 0) + l.amount));
    return Array.from(byParticular.entries()).map(([name, value]) => ({ name, value }));
  }, [ledger]);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader title="Total Revenue" />
          <p className="p-4 text-2xl font-semibold text-emerald-600">{formatCurrency(revenue)}</p>
        </Card>
        <Card>
          <CardHeader title="Administrative Expenses" />
          <p className="p-4 text-2xl font-semibold text-rose-600">{formatCurrency(adminExpenses)}</p>
        </Card>
        <Card>
          <CardHeader title="Net After Expenses" />
          <p className={`p-4 text-2xl font-semibold ${netAfterExpenses >= 0 ? 'text-slate-900' : 'text-rose-600'}`}>{formatCurrency(netAfterExpenses)}</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Current Assets" subtitle="Bank, cash and receivables" />
          <div className="space-y-2 p-4">
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">Bank</span>
              <span className="font-medium text-slate-900">{formatCurrency(currentAssets.bank)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">Cash</span>
              <span className="font-medium text-slate-900">{formatCurrency(currentAssets.cash)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">Accounts Receivable</span>
              <span className="font-medium text-slate-900">{formatCurrency(currentAssets.receivable)}</span>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-sm font-semibold">
              <span>Total Current Assets</span>
              <span>{formatCurrency(currentAssets.total)}</span>
            </div>
          </div>
        </Card>

        <Card>
          <CardHeader title="Balance Status" subtitle="Assets vs liabilities" />
          <div className="space-y-2 p-4">
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">Current Assets</span>
              <span className="font-medium text-emerald-600">{formatCurrency(currentAssets.total)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">Liabilities</span>
              <span className="font-medium text-rose-600">{formatCurrency(liabilities)}</span>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-sm font-semibold">
              <span>Net Position</span>
              <span>{formatCurrency(currentAssets.total - liabilities)}</span>
            </div>
          </div>
        </Card>
      </div>

      <Card>
        <CardHeader title="Expense Breakdown" subtitle="Administrative expenses by type" />
        {expenseBreakdown.length === 0 ? (
          <p className="p-6 text-center text-sm text-slate-400">No expenses recorded yet.</p>
        ) : (
          <div className="h-64 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={expenseBreakdown} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                  {expenseBreakdown.map((_, idx) => (
                    <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>
    </div>
  );
}
