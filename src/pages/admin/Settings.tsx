import { useState } from 'react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Select } from '../../components/ui/Field';
import { useSettings, updateSettings } from '../../lib/settings';
import { useToast } from '../../context/ToastContext';
import type { FirmSettings } from '../../types';

export function AdminSettings() {
  const settings = useSettings();
  const { show } = useToast();
  const [form, setForm] = useState<FirmSettings>(settings);

  const save = () => {
    updateSettings(form);
    show('Settings saved.', 'success');
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader title="Firm Details" subtitle="About your firm" />
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          <Input label="Firm Name" value={form.firmName} onChange={(e) => setForm({ ...form, firmName: e.target.value })} />
          <Input label="Logo URL" placeholder="https://path/to/logo.png" value={form.logoUrl ?? ''} onChange={(e) => setForm({ ...form, logoUrl: e.target.value })} />
        </div>
      </Card>

      <Card>
        <CardHeader title="Automation" subtitle="Backups and reminders" />
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          <Select
            label="Auto Backup Frequency"
            value={form.autoBackupFrequency}
            onChange={(e) => setForm({ ...form, autoBackupFrequency: e.target.value as FirmSettings['autoBackupFrequency'] })}
          >
            <option value="Daily">Daily</option>
            <option value="Weekly">Weekly</option>
            <option value="Monthly">Monthly</option>
            <option value="Off">Off</option>
          </Select>
          <label className="flex items-center gap-2 self-end pb-2 text-sm text-slate-600">
            <input type="checkbox" checked={form.autoReminders} onChange={(e) => setForm({ ...form, autoReminders: e.target.checked })} className="size-4 rounded border-slate-300" />
            Auto Reminders enabled
          </label>
        </div>
      </Card>

      <Card>
        <CardHeader title="API Settings" subtitle="Connect EmailJS for notifications and an AI provider for the assistant" />
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          <Input label="EmailJS Service ID" value={form.emailjsServiceId ?? ''} onChange={(e) => setForm({ ...form, emailjsServiceId: e.target.value })} />
          <Input label="EmailJS Template ID" value={form.emailjsTemplateId ?? ''} onChange={(e) => setForm({ ...form, emailjsTemplateId: e.target.value })} />
          <Input label="EmailJS Public Key" value={form.emailjsPublicKey ?? ''} onChange={(e) => setForm({ ...form, emailjsPublicKey: e.target.value })} />
          <div />
          <Input label="AI API Endpoint" placeholder="https://api.example.com/v1/chat/completions" value={form.aiApiEndpoint ?? ''} onChange={(e) => setForm({ ...form, aiApiEndpoint: e.target.value })} />
          <Input label="AI API Key" type="password" value={form.aiApiKey ?? ''} onChange={(e) => setForm({ ...form, aiApiKey: e.target.value })} />
        </div>
      </Card>

      <div className="flex justify-end">
        <Button onClick={save}>Save Settings</Button>
      </div>
    </div>
  );
}
