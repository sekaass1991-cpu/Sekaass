import { ChatPanel } from '../../components/ai/ChatPanel';
import { useAuth } from '../../context/AuthContext';
import { invoicesStore, complianceStore } from '../../lib/entities';
import { formatCurrency, formatDate, invoiceTotal } from '../../lib/format';

export function ClientAiAssistant() {
  const { user } = useAuth();

  const buildContext = () => {
    const invoices = invoicesStore.getAll().filter((i) => i.clientId === user?.clientId);
    const compliance = complianceStore.getAll().filter((c) => c.clientId === user?.clientId);
    const outstanding = invoices.reduce((s, i) => (i.status === 'Paid' ? s : s + (invoiceTotal(i.items) - i.amountPaid)), 0);
    const pendingCompliance = compliance.filter((c) => c.status !== 'Completed');

    return [
      `Outstanding amount owed: ${formatCurrency(outstanding)}`,
      `Total invoices: ${invoices.length}`,
      `Pending compliance items: ${pendingCompliance.map((c) => `${c.title} (due ${formatDate(c.dueDate)})`).join(', ') || 'none'}`,
    ].join('\n');
  };

  return <ChatPanel storageKey={`ai_client_messages_${user?.clientId}`} contextLabel="Personal assistant for your account" buildContext={buildContext} />;
}
