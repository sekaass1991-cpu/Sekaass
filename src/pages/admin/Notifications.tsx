import { useMemo } from 'react';
import { Bell, Trash2, AlertTriangle, Info, CheckCircle2, XCircle } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { EmptyState } from '../../components/ui/EmptyState';
import { Button } from '../../components/ui/Button';
import { notificationsStore } from '../../lib/entities';
import { formatDateTime } from '../../lib/format';
import type { AppNotification } from '../../types';

const ICONS: Record<AppNotification['type'], typeof Info> = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  error: XCircle,
};

const ICON_COLORS: Record<AppNotification['type'], string> = {
  info: 'text-sky-500 bg-sky-50',
  success: 'text-emerald-500 bg-emerald-50',
  warning: 'text-amber-500 bg-amber-50',
  error: 'text-rose-500 bg-rose-50',
};

export function AdminNotifications() {
  const notifications = notificationsStore.useAll();
  const mine = useMemo(
    () => notifications.filter((n) => n.audience === 'admin' || n.audience === 'all').sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)),
    [notifications],
  );

  const markAllRead = () => mine.forEach((n) => !n.read && notificationsStore.update(n.id, { read: true }));

  return (
    <Card>
      <div className="flex items-center justify-between border-b border-slate-100 p-4">
        <p className="text-sm font-semibold text-slate-900">All Notifications</p>
        <Button variant="secondary" size="sm" onClick={markAllRead}>
          Mark all read
        </Button>
      </div>
      {mine.length === 0 ? (
        <EmptyState icon={<Bell className="size-5" />} title="No notifications" message="You're all caught up." />
      ) : (
        <ul className="divide-y divide-slate-50">
          {mine.map((n) => {
            const Icon = ICONS[n.type];
            return (
              <li key={n.id} className={`flex items-start gap-3 p-4 ${n.read ? '' : 'bg-indigo-50/40'}`}>
                <div className={`flex size-8 shrink-0 items-center justify-center rounded-full ${ICON_COLORS[n.type]}`}>
                  <Icon className="size-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-900">{n.title}</p>
                  <p className="text-sm text-slate-500">{n.message}</p>
                  <p className="mt-1 text-xs text-slate-400">{formatDateTime(n.createdAt)}</p>
                </div>
                <div className="flex shrink-0 gap-1">
                  {!n.read && (
                    <button onClick={() => notificationsStore.update(n.id, { read: true })} className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50" title="Mark read">
                      <CheckCircle2 className="size-4" />
                    </button>
                  )}
                  <button onClick={() => notificationsStore.remove(n.id)} className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50" title="Delete">
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
