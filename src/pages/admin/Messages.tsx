import { useMemo, useState } from 'react';
import { MessageSquare, Send } from 'lucide-react';
import clsx from 'clsx';
import { Card } from '../../components/ui/Card';
import { EmptyState } from '../../components/ui/EmptyState';
import { messagesStore, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { formatDateTime } from '../../lib/format';

export function AdminMessages() {
  const { user } = useAuth();
  const threads = messagesStore.useAll();
  const sorted = useMemo(() => [...threads].sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1)), [threads]);
  const [activeId, setActiveId] = useState<string | null>(sorted[0]?.id ?? null);
  const [draft, setDraft] = useState('');

  const active = threads.find((t) => t.id === activeId) ?? sorted[0];

  const openThread = (id: string) => {
    setActiveId(id);
    const thread = threads.find((t) => t.id === id);
    if (thread && thread.unreadForAdmin) {
      messagesStore.update(id, { unreadForAdmin: false });
    }
  };

  const send = () => {
    if (!active || !draft.trim()) return;
    const updated = {
      ...active,
      updatedAt: new Date().toISOString(),
      unreadForClient: true,
      messages: [
        ...active.messages,
        { id: genId('msg'), sender: user?.name ?? 'Admin', senderRole: 'admin' as const, text: draft.trim(), timestamp: new Date().toISOString() },
      ],
    };
    messagesStore.update(active.id, updated);
    pushNotification('client', 'New message', `${user?.name ?? 'Admin'} sent you a message: "${draft.trim().slice(0, 60)}"`, 'info', active.clientId);
    setDraft('');
  };

  if (sorted.length === 0) {
    return (
      <Card>
        <EmptyState icon={<MessageSquare className="size-5" />} title="No conversations" message="Client conversations will appear here." />
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3" style={{ height: 'calc(100vh - 140px)' }}>
      <Card className="flex flex-col overflow-hidden lg:col-span-1">
        <div className="border-b border-slate-100 p-3 text-sm font-semibold text-slate-900">Conversations</div>
        <div className="flex-1 overflow-y-auto">
          {sorted.map((t) => (
            <button
              key={t.id}
              onClick={() => openThread(t.id)}
              className={clsx('block w-full border-b border-slate-50 p-3 text-left hover:bg-slate-50', active?.id === t.id && 'bg-indigo-50')}
            >
              <div className="flex items-center justify-between">
                <p className="truncate text-sm font-medium text-slate-900">{t.clientName}</p>
                {t.unreadForAdmin && <span className="size-2 rounded-full bg-indigo-500" />}
              </div>
              <p className="truncate text-xs text-slate-500">{t.subject}</p>
              <p className="mt-1 text-[11px] text-slate-400">{formatDateTime(t.updatedAt)}</p>
            </button>
          ))}
        </div>
      </Card>

      <Card className="flex flex-col overflow-hidden lg:col-span-2">
        {active ? (
          <>
            <div className="border-b border-slate-100 p-3">
              <p className="text-sm font-semibold text-slate-900">{active.subject}</p>
              <p className="text-xs text-slate-500">{active.clientName}</p>
            </div>
            <div className="flex-1 space-y-3 overflow-y-auto p-4">
              {active.messages.map((m) => (
                <div key={m.id} className={clsx('flex', m.senderRole === 'admin' ? 'justify-end' : 'justify-start')}>
                  <div className={clsx('max-w-xs rounded-xl px-3 py-2 text-sm', m.senderRole === 'admin' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-700')}>
                    <p>{m.text}</p>
                    <p className={clsx('mt-1 text-[10px]', m.senderRole === 'admin' ? 'text-indigo-100' : 'text-slate-400')}>{formatDateTime(m.timestamp)}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex gap-2 border-t border-slate-100 p-3">
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && send()}
                placeholder="Type a reply..."
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <button onClick={send} className="flex items-center justify-center rounded-lg bg-indigo-600 px-3 text-white hover:bg-indigo-700">
                <Send className="size-4" />
              </button>
            </div>
          </>
        ) : (
          <EmptyState icon={<MessageSquare className="size-5" />} title="Select a conversation" message="Choose a thread from the list." />
        )}
      </Card>
    </div>
  );
}
