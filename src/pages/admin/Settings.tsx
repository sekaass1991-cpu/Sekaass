import { useState } from 'react';
import { TriangleAlert } from 'lucide-react';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Select } from '../../components/ui/Field';
import { useSettings, updateSettings } from '../../lib/settings';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { isEmailConfigured, sendEmail } from '../../lib/email';
import type { ClaudeModel, FirmSettings } from '../../types';

const CLAUDE_MODELS: { id: ClaudeModel; label: string }[] = [
  { id: 'claude-sonnet-5', label: 'Claude Sonnet 5' },
  { id: 'claude-opus-5', label: 'Claude Opus 5' },
  { id: 'claude-fable-5', label: 'Claude Fable 5' },
  { id: 'claude-haiku-4-5-20251001', label: 'Claude Haiku 4.5' },
];

export function AdminSettings() {
  const settings = useSettings();
  const { show } = useToast();
  const { user } = useAuth();
  const [form, setForm] = useState<FirmSettings>(settings);
  const [sendingTest, setSendingTest] = useState(false);

  const save = () => {
    updateSettings(form);
    show('Settings saved.', 'success');
  };

  const sendTestEmail = async () => {
    if (!user?.email) return;
    setSendingTest(true);
    const result = await sendEmail(form, {
      toEmail: user.email,
      toName: user.name,
      subject: 'TaxTitan Consultancy — test email',
      message: 'This is a test email confirming your EmailJS configuration is working.',
    });
    setSendingTest(false);
    show(result.ok ? `Test email sent to ${user.email}.` : `Failed to send: ${result.error}`, result.ok ? 'success' : 'error');
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
        <CardHeader title="Email (EmailJS)" subtitle="Used for client notifications and welcome emails" />
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          <Input label="EmailJS Service ID" value={form.emailjsServiceId ?? ''} onChange={(e) => setForm({ ...form, emailjsServiceId: e.target.value })} />
          <Input label="EmailJS Template ID" value={form.emailjsTemplateId ?? ''} onChange={(e) => setForm({ ...form, emailjsTemplateId: e.target.value })} />
          <Input label="EmailJS Public Key" value={form.emailjsPublicKey ?? ''} onChange={(e) => setForm({ ...form, emailjsPublicKey: e.target.value })} />
        </div>
        <div className="mx-4 mb-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          Your EmailJS template should use these variables:{' '}
          <code className="text-slate-700">to_email</code>, <code className="text-slate-700">to_name</code>,{' '}
          <code className="text-slate-700">from_name</code>, <code className="text-slate-700">subject</code>, and{' '}
          <code className="text-slate-700">message</code>. Once set up, invoices, compliance deadlines, document
          reviews, messages, and new client welcome emails are all sent automatically.
        </div>
        <div className="mx-4 mb-4 flex justify-end">
          <Button variant="secondary" size="sm" disabled={!isEmailConfigured(form) || sendingTest} onClick={sendTestEmail}>
            {sendingTest ? 'Sending…' : 'Send Test Email'}
          </Button>
        </div>
      </Card>

      <Card>
        <CardHeader title="AI Assistant (Claude)" subtitle="Connect the AI Assistant to Claude" />
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
          <Select label="Model" value={form.claudeModel} onChange={(e) => setForm({ ...form, claudeModel: e.target.value as ClaudeModel })}>
            {CLAUDE_MODELS.map((m) => (
              <option key={m.id} value={m.id}>
                {m.label}
              </option>
            ))}
          </Select>
          <Input label="Claude API Key" type="password" placeholder="sk-ant-..." value={form.claudeApiKey ?? ''} onChange={(e) => setForm({ ...form, claudeApiKey: e.target.value })} />
        </div>
        <div className="mx-4 mb-4 flex gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
          <TriangleAlert className="size-4 shrink-0" />
          <p>
            This app has no backend, so the key is used directly from your browser and stored in this browser&apos;s
            local storage. It will be visible in network requests to anyone with access to this device — fine for a
            personal or demo setup, not recommended for a shared or public deployment.
          </p>
        </div>
      </Card>

      <div className="flex justify-end">
        <Button onClick={save}>Save Settings</Button>
      </div>
    </div>
  );
}
