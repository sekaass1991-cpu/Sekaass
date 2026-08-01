import { useState } from 'react';
import { UserCircle } from 'lucide-react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Field';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { usersStore } from '../../lib/entities';

export function AdminProfile() {
  const { user } = useAuth();
  const { show } = useToast();
  const [name, setName] = useState(user?.name ?? '');
  const [phone, setPhone] = useState(user?.phone ?? '');
  const [password, setPassword] = useState('');

  if (!user) return null;

  const save = () => {
    usersStore.update(user.id, { name, phone, ...(password ? { password } : {}) });
    setPassword('');
    show('Profile updated.', 'success');
  };

  return (
    <div className="max-w-xl space-y-4">
      <Card>
        <CardHeader title="Admin Profile" subtitle="Your account details" />
        <div className="flex items-center gap-3 p-4">
          <UserCircle className="size-14 text-slate-300" />
          <div>
            <p className="text-sm font-semibold text-slate-900">{user.name}</p>
            <p className="text-xs text-slate-500">{user.email}</p>
          </div>
        </div>
        <div className="space-y-3 p-4 pt-0">
          <Input label="Full Name" value={name} onChange={(e) => setName(e.target.value)} />
          <Input label="Email" value={user.email} disabled />
          <Input label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Input label="New Password" type="password" placeholder="Leave blank to keep current password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
      </Card>
      <div className="flex justify-end">
        <Button onClick={save}>Save Changes</Button>
      </div>
    </div>
  );
}
