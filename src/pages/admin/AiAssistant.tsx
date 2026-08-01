import { ChatPanel } from '../../components/ai/ChatPanel';
import { clientsStore, invoicesStore, complianceStore } from '../../lib/entities';
import { formatCurrency, invoiceTotal } from '../../lib/format';

export function AdminAiAssistant() {
  const buildContext = () => {
    const clients = clientsStore.getAll();
    const invoices = invoicesStore.getAll();
    const compliance = complianceStore.getAll();

    const overdueInvoices = invoices.filter((i) => i.status === 'Overdue');
    const overdueCompliance = compliance.filter((c) => c.status === 'Overdue');
    const outstanding = invoices.reduce((s, i) => (i.status === 'Paid' ? s : s + (invoiceTotal(i.items) - i.amountPaid)), 0);

    return [
      `Total clients: ${clients.length} (${clients.filter((c) => c.status === 'Active').length} active)`,
      `Outstanding invoice amount: ${formatCurrency(outstanding)}`,
      `Overdue invoices: ${overdueInvoices.map((i) => `${i.invoiceNumber} (${i.clientName})`).join(', ') || 'none'}`,
      `Overdue compliance items: ${overdueCompliance.map((c) => `${c.title} (${c.clientName})`).join(', ') || 'none'}`,
    ].join('\n');
  };

  return <ChatPanel storageKey="ai_admin_messages" contextLabel="Firm-wide assistant for admins" buildContext={buildContext} />;
}
