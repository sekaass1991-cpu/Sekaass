import { Briefcase } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { servicesStore, clientsStore, pushNotification, logAudit } from '../../lib/entities';
import { useToast } from '../../context/ToastContext';
import { formatCurrency } from '../../lib/format';

export function ClientServices() {
  const { user } = useAuth();
  const { show } = useToast();
  const services = servicesStore.useAll().filter((s) => s.active);
  const clients = clientsStore.useAll();
  const client = clients.find((c) => c.id === user?.clientId);

  const requestService = (name: string) => {
    if (!client) return;
    pushNotification('admin', 'Service request', `${client.businessName} requested "${name}".`, 'info', client.id, true);
    logAudit(user?.name ?? 'Client', 'client', 'Requested service', 'Service', undefined, `Requested "${name}"`);
    show('Request sent to your consultant.', 'success');
  };

  if (services.length === 0) {
    return (
      <Card>
        <EmptyState icon={<Briefcase className="size-5" />} title="No services available" message="Check back soon for available services." />
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {services.map((s) => (
        <Card key={s.id} className="flex flex-col justify-between p-4">
          <div>
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-900">{s.name}</p>
              <Badge tone="blue">{s.category}</Badge>
            </div>
            <p className="mt-2 text-xs text-slate-500">{s.description}</p>
          </div>
          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm font-semibold text-slate-900">{formatCurrency(s.price)}</p>
            <Button size="sm" onClick={() => requestService(s.name)}>
              Request
            </Button>
          </div>
        </Card>
      ))}
    </div>
  );
}
