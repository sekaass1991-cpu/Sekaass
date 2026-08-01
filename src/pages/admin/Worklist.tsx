import { useMemo } from 'react';
import { ListChecks } from 'lucide-react';
import { Card, CardHeader } from '../../components/ui/Card';
import { StatusBadge, Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { complianceStore, documentsStore, invoicesStore } from '../../lib/entities';
import { formatDate, invoiceTotal } from '../../lib/format';

interface WorkItem {
  id: string;
  type: 'Compliance' | 'Document Review' | 'Invoice Follow-up';
  title: string;
  clientName: string;
  dueDate?: string;
  priority: 'Low' | 'Medium' | 'High';
}

export function AdminWorklist() {
  const compliance = complianceStore.useAll();
  const documents = documentsStore.useAll();
  const invoices = invoicesStore.useAll();

  const items: WorkItem[] = useMemo(() => {
    const complianceItems: WorkItem[] = compliance
      .filter((c) => c.status !== 'Completed')
      .map((c) => ({ id: c.id, type: 'Compliance', title: c.title, clientName: c.clientName, dueDate: c.dueDate, priority: c.priority }));

    const docItems: WorkItem[] = documents
      .filter((d) => d.status === 'Awaiting Review')
      .map((d) => ({ id: d.id, type: 'Document Review', title: `Review ${d.name}`, clientName: d.clientName, dueDate: d.uploadedDate, priority: 'Medium' }));

    const invoiceItems: WorkItem[] = invoices
      .filter((i) => i.status === 'Overdue')
      .map((i) => ({ id: i.id, type: 'Invoice Follow-up', title: `Follow up on ${i.invoiceNumber} (${invoiceTotal(i.items)})`, clientName: i.clientName, dueDate: i.dueDate, priority: 'High' }));

    return [...complianceItems, ...invoiceItems, ...docItems].sort((a, b) => ((a.dueDate ?? '') < (b.dueDate ?? '') ? -1 : 1));
  }, [compliance, documents, invoices]);

  const groups: Record<WorkItem['type'], WorkItem[]> = {
    Compliance: items.filter((i) => i.type === 'Compliance'),
    'Document Review': items.filter((i) => i.type === 'Document Review'),
    'Invoice Follow-up': items.filter((i) => i.type === 'Invoice Follow-up'),
  };

  if (items.length === 0) {
    return (
      <Card>
        <EmptyState icon={<ListChecks className="size-5" />} title="Worklist is clear" message="No pending compliance, invoice, or document tasks right now." />
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      {(Object.keys(groups) as WorkItem['type'][]).map((type) => (
        <Card key={type}>
          <CardHeader title={type} subtitle={`${groups[type].length} item(s)`} />
          {groups[type].length === 0 ? (
            <p className="p-6 text-center text-xs text-slate-400">Nothing here.</p>
          ) : (
            <ul className="divide-y divide-slate-50">
              {groups[type].map((item) => (
                <li key={item.id} className="p-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-sm font-medium text-slate-800">{item.title}</p>
                    <StatusBadge status={item.priority} />
                  </div>
                  <p className="mt-0.5 text-xs text-slate-500">{item.clientName}</p>
                  {item.dueDate && (
                    <p className="mt-1">
                      <Badge tone="slate">Due {formatDate(item.dueDate)}</Badge>
                    </p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      ))}
    </div>
  );
}
