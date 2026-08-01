import { useState } from 'react';
import { UserCircle } from 'lucide-react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Textarea } from '../../components/ui/Field';
import { StatusBadge } from '../../components/ui/Badge';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { clientsStore, usersStore } from '../../lib/entities';

export function ClientProfile() {
  const { user } = useAuth();
  const { show } = useToast();
  const client = clientsStore.useAll().find((c) => c.id === user?.clientId);

  const [phone, setPhone] = useState(client?.phone ?? '');
  const [address, setAddress] = useState(client?.address ?? '');
  const [password, setPassword] = useState('');

  if (!user || !client) return null;

  const save = () => {
    clientsStore.update(client.id, { phone, address });
    if (password) usersStore.update(user.id, { password });
    setPassword('');
    show('Profile updated.', 'success');
  };

  return (
    <div className="max-w-xl space-y-4">
      <Card>
        <CardHeader title="About Your Firm" subtitle="Business details on file with your consultant" />
        <div className="flex items-center gap-3 p-4">
          <UserCircle className="size-14 text-slate-300" />
          <div>
            <p className="text-sm font-semibold text-slate-900">{client.businessName}</p>
            <p className="text-xs text-slate-500">{client.name}</p>
          </div>
          <StatusBadge status={client.status} />
        </div>
        <div className="space-y-3 p-4 pt-0">
          <div className="grid grid-cols-2 gap-3">
            <Input label="Entity Type" value={client.entityType} disabled />
            <Input label="Category" value={client.category} disabled />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="PAN" value={client.pan ?? '-'} disabled />
            <Input label="GSTIN" value={client.gstin ?? '-'} disabled />
          </div>
          <Input label="Email" value={client.email} disabled />
          <Input label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Textarea label="Address" rows={2} value={address} onChange={(e) => setAddress(e.target.value)} />
          <Input label="New Password" type="password" placeholder="Leave blank to keep current password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
      </Card>
      <div className="flex justify-end">
        <Button onClick={save}>Save Changes</Button>
      </div>
    </div>
  );
}
