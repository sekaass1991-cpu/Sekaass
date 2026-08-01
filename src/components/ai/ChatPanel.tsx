import { useState } from 'react';
import { Sparkles, Send, Loader2 } from 'lucide-react';
import clsx from 'clsx';
import { Card } from '../ui/Card';
import { getAssistantReply, type AiChatMessage } from '../../lib/ai';
import { useSettings } from '../../lib/settings';
import { createCollection, genId } from '../../lib/storage';

interface StoredMessage extends AiChatMessage {
  id: string;
  timestamp: string;
}

export function ChatPanel({ storageKey, contextLabel, buildContext }: { storageKey: string; contextLabel: string; buildContext: () => string }) {
  const store = createCollection<StoredMessage>(storageKey);
  const messages = store.useAll();
  const ordered = [...messages].sort((a, b) => (a.timestamp < b.timestamp ? -1 : 1));
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(false);
  const settings = useSettings();

  const send = async () => {
    const text = draft.trim();
    if (!text || loading) return;
    setDraft('');
    const userMsg: StoredMessage = { id: genId('msg'), role: 'user', content: text, timestamp: new Date().toISOString() };
    store.add(userMsg);
    setLoading(true);
    try {
      const context = buildContext();
      const reply = await getAssistantReply(
        [...ordered, userMsg].map((m) => ({ role: m.role, content: m.content })),
        context,
        settings,
      );
      store.add({ id: genId('msg'), role: 'assistant', content: reply, timestamp: new Date().toISOString() });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="flex flex-col overflow-hidden" style={{ height: 'calc(100vh - 140px)' }}>
      <div className="flex items-center gap-2 border-b border-slate-100 p-4">
        <Sparkles className="size-4 text-indigo-600" />
        <div>
          <p className="text-sm font-semibold text-slate-900">AI Assistant</p>
          <p className="text-xs text-slate-500">{contextLabel}</p>
        </div>
      </div>
      <div className="flex-1 space-y-3 overflow-y-auto p-4">
        {ordered.length === 0 && (
          <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-500">
            Ask me things like "what's overdue" or "summarize my clients". Configure an API key in Settings for richer, model-generated answers.
          </p>
        )}
        {ordered.map((m) => (
          <div key={m.id} className={clsx('flex', m.role === 'user' ? 'justify-end' : 'justify-start')}>
            <div className={clsx('max-w-lg whitespace-pre-wrap rounded-xl px-3 py-2 text-sm', m.role === 'user' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-700')}>
              {m.content}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <Loader2 className="size-3.5 animate-spin" /> Thinking...
          </div>
        )}
      </div>
      <div className="flex gap-2 border-t border-slate-100 p-3">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
          placeholder="Ask the assistant..."
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <button onClick={send} disabled={loading} className="flex items-center justify-center rounded-lg bg-indigo-600 px-3 text-white hover:bg-indigo-700 disabled:bg-indigo-300">
          <Send className="size-4" />
        </button>
      </div>
    </Card>
  );
}
