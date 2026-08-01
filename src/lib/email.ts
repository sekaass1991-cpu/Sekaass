import emailjs from '@emailjs/browser';
import type { FirmSettings } from '../types';

export function isEmailConfigured(settings: FirmSettings): boolean {
  return Boolean(settings.emailjsServiceId && settings.emailjsTemplateId && settings.emailjsPublicKey);
}

interface SendEmailInput {
  toEmail: string;
  toName: string;
  subject: string;
  message: string;
}

export async function sendEmail(settings: FirmSettings, input: SendEmailInput): Promise<{ ok: boolean; error?: string }> {
  if (!isEmailConfigured(settings)) {
    return { ok: false, error: 'EmailJS is not configured (see Settings > Email).' };
  }
  try {
    await emailjs.send(
      settings.emailjsServiceId!,
      settings.emailjsTemplateId!,
      {
        to_email: input.toEmail,
        to_name: input.toName,
        from_name: settings.firmName,
        subject: input.subject,
        message: input.message,
      },
      { publicKey: settings.emailjsPublicKey! },
    );
    return { ok: true };
  } catch (err) {
    const error = err as { text?: string; message?: string };
    return { ok: false, error: error.text ?? error.message ?? 'Failed to send email.' };
  }
}
