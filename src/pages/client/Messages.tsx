import { useMemo, useState } from 'react';
import { MessageSquare, Send } from 'lucide-react';
import clsx from 'clsx';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Field';
import { useAuth } from '../../context/AuthContext';
import { messagesStore, clientsStore, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { formatDateTime } from '../../lib/format';

export function ClientMessages() {
  const { user } = useAuth();
  const threads = messagesStore.useAll();
  const clients = clientsStore.useAll();
  const client = clients.find((c) => c.id === user?.clientId);

  const myThreads = useMemo(() => threads.filter((t) => t.clientId === user?.clientId).sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1)), [threads, user]);
  const [activeId, setActiveId] = useState<string | null>(myThreads[0]?.id ?? null);
  const [draft, setDraft] = useState('');
  const [newSubject, setNewSubject] = useState('');

  const active = myThreads.find((t) => t.id === activeId) ?? myThreads[0];

  const openThread = (id: string) => {
    setActiveId(id);
    const thread = myThreads.find((t) => t.id === id);
    if (thread?.unreadForClient) messagesStore.update(id, { unreadForClient: false });
  };

  const startThread = () => {
    if (!client || !newSubject.trim()) return;
    const id = genId('thr');
    messagesStore.add({
      id,
      clientId: client.id,
      clientName: client.businessName,
      subject: newSubject.trim(),
      updatedAt: new Date().toISOString(),
      unreadForAdmin: true,
      unreadForClient: false,
      messages: [],
    });
    pushNotification('admin', 'New conversation', `${client.businessName} started a conversation: "${newSubject.trim()}"`, 'info', client.id, true);
    setNewSubject('');
    setActiveId(id);
  };

  const send = () => {
    if (!active || !draft.trim()) return;
    messagesStore.update(active.id, {
      updatedAt: new Date().toISOString(),
      unreadForAdmin: true,
      messages: [...active.messages, { id: genId('msg'), sender: user?.name ?? 'Client', senderRole: 'client', text: draft.trim(), timestamp: new Date().toISOString() }],
    });
    pushNotification('admin', 'New message', `${client?.businessName ?? 'A client'} replied: "${draft.trim().slice(0, 60)}"`, 'info', client?.id, true);
    setDraft('');
  };

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3" style={{ height: 'calc(100vh - 140px)' }}>
      <Card className="flex flex-col overflow-hidden lg:col-span-1">
        <div className="space-y-2 border-b border-slate-100 p-3">
          <p className="text-sm font-semibold text-slate-900">New Conversation</p>
          <div className="flex gap-2">
            <Input placeholder="Subject" value={newSubject} onChange={(e) => setNewSubject(e.target.value)} />
            <Button size="sm" onClick={startThread}>
              Start
            </Button>
          </div>
        </div>
        <div className="flex-1 overflow-y-auto">
          {myThreads.map((t) => (
            <button
              key={t.id}
              onClick={() => openThread(t.id)}
              className={clsx('block w-full border-b border-slate-50 p-3 text-left hover:bg-slate-50', active?.id === t.id && 'bg-indigo-50')}
            >
              <div className="flex items-center justify-between">
                <p className="truncate text-sm font-medium text-slate-900">{t.subject}</p>
                {t.unreadForClient && <span className="size-2 rounded-full bg-indigo-500" />}
              </div>
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
            </div>
            <div className="flex-1 space-y-3 overflow-y-auto p-4">
              {active.messages.length === 0 && <p className="text-center text-xs text-slate-400">Send a message to your consultant.</p>}
              {active.messages.map((m) => (
                <div key={m.id} className={clsx('flex', m.senderRole === 'client' ? 'justify-end' : 'justify-start')}>
                  <div className={clsx('max-w-xs rounded-xl px-3 py-2 text-sm', m.senderRole === 'client' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-700')}>
                    <p>{m.text}</p>
                    <p className={clsx('mt-1 text-[10px]', m.senderRole === 'client' ? 'text-indigo-100' : 'text-slate-400')}>{formatDateTime(m.timestamp)}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="flex gap-2 border-t border-slate-100 p-3">
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && send()}
                placeholder="Type a message..."
                className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <button onClick={send} className="flex items-center justify-center rounded-lg bg-indigo-600 px-3 text-white hover:bg-indigo-700">
                <Send className="size-4" />
              </button>
            </div>
          </>
        ) : (
          <div className="flex flex-1 items-center justify-center">
            <div className="text-center">
              <MessageSquare className="mx-auto size-8 text-slate-300" />
              <p className="mt-2 text-sm text-slate-500">Start a new conversation to message your consultant.</p>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
