import { useState } from 'react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { readRaw, writeRaw } from '../../lib/storage';

interface ClientPrefs {
  emailNotifications: boolean;
  smsNotifications: boolean;
}

export function ClientSettings() {
  const { user } = useAuth();
  const { show } = useToast();
  const key = `client_prefs_${user?.id}`;
  const [prefs, setPrefs] = useState<ClientPrefs>(() => readRaw<ClientPrefs>(key, { emailNotifications: true, smsNotifications: false }));

  const save = () => {
    writeRaw(key, prefs);
    show('Preferences saved.', 'success');
  };

  return (
    <div className="max-w-xl space-y-4">
      <Card>
        <CardHeader title="Notification Preferences" subtitle="Choose how you'd like to be notified" />
        <div className="space-y-3 p-4">
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={prefs.emailNotifications}
              onChange={(e) => setPrefs({ ...prefs, emailNotifications: e.target.checked })}
              className="size-4 rounded border-slate-300"
            />
            Email notifications for compliance deadlines and invoices
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={prefs.smsNotifications}
              onChange={(e) => setPrefs({ ...prefs, smsNotifications: e.target.checked })}
              className="size-4 rounded border-slate-300"
            />
            SMS reminders for urgent items
          </label>
        </div>
      </Card>
      <div className="flex justify-end">
        <Button onClick={save}>Save Preferences</Button>
      </div>
    </div>
  );
}
