import Anthropic from '@anthropic-ai/sdk';
import type { FirmSettings } from '../types';

export interface AiChatMessage {
  role: 'user' | 'assistant';
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
    return "Hi! I'm your TaxTitan AI Assistant. Ask me about outstanding invoices, upcoming compliance deadlines, or client status, and I'll summarize what's in your workspace. Add a Claude API key in Settings to talk to me directly.";
  }
  return `I can only give simple summaries right now since no Claude API key is configured (see Settings > AI Assistant). Here's a quick snapshot of your data:\n\n${context}`;
}

export async function getAssistantReply(
  messages: AiChatMessage[],
  context: string,
  settings: FirmSettings,
): Promise<string> {
  const lastUser = [...messages].reverse().find((m) => m.role === 'user')?.content ?? '';

  if (!settings.claudeApiKey) {
    return localReply(lastUser, context);
  }

  try {
    const client = new Anthropic({ apiKey: settings.claudeApiKey, dangerouslyAllowBrowser: true });
    const response = await client.messages.create({
      model: settings.claudeModel,
      max_tokens: 1024,
      system: `You are a helpful assistant for a Chartered Accountant firm. Use the workspace context below to answer questions about clients, invoices, and compliance.\n\n${context}`,
      messages,
    });
    const textBlock = response.content.find((block) => block.type === 'text');
    if (textBlock && textBlock.text.trim()) return textBlock.text;
    throw new Error('Claude returned an empty response');
  } catch (err) {
    const message = err instanceof Anthropic.APIError ? [err.status, err.message].filter(Boolean).join(' ') : (err as Error).message;
    return `I couldn't reach Claude (${message}). Falling back to a local summary:\n\n${localReply(lastUser, context)}`;
  }
}
