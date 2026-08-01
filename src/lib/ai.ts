import type { FirmSettings } from '../types';

export interface AiChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

function localReply(prompt: string, context: string): string {
  const lower = prompt.toLowerCase();
  if (lower.includes('overdue')) {
    return `Here's what I found in your data:\n\n${context}\n\nFocus on the overdue items first — they usually have the biggest compliance and cash-flow risk.`;
  }
  if (lower.includes('client')) {
    return `Based on your current records:\n\n${context}`;
  }
  if (lower.includes('help') || lower.includes('hi') || lower.includes('hello')) {
    return "Hi! I'm your Sekaass AI Assistant. Ask me about outstanding invoices, upcoming compliance deadlines, or client status, and I'll summarize what's in your workspace. Add an API key in Settings to connect a real language model for richer answers.";
  }
  return `I can only give simple summaries right now since no AI API key is configured (see Settings > API Settings). Here's a quick snapshot of your data:\n\n${context}`;
}

export async function getAssistantReply(
  messages: AiChatMessage[],
  context: string,
  settings: FirmSettings,
): Promise<string> {
  const lastUser = [...messages].reverse().find((m) => m.role === 'user')?.content ?? '';

  if (!settings.aiApiKey || !settings.aiApiEndpoint) {
    return localReply(lastUser, context);
  }

  try {
    const res = await fetch(settings.aiApiEndpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${settings.aiApiKey}`,
      },
      body: JSON.stringify({
        messages: [{ role: 'system', content: `You are a helpful assistant for a CA firm. Context:\n${context}` }, ...messages],
      }),
    });
    if (!res.ok) throw new Error(`Request failed with ${res.status}`);
    const data = await res.json();
    const content = data?.choices?.[0]?.message?.content ?? data?.content ?? data?.reply;
    if (typeof content === 'string' && content.trim()) return content;
    throw new Error('Unexpected response shape');
  } catch (err) {
    return `I couldn't reach the configured AI endpoint (${(err as Error).message}). Falling back to a local summary:\n\n${localReply(lastUser, context)}`;
  }
}
