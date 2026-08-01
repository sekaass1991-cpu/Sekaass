import type { ReactNode } from 'react';
import clsx from 'clsx';

type Tone = 'slate' | 'green' | 'amber' | 'red' | 'blue' | 'purple';

const TONE_CLASSES: Record<Tone, string> = {
  slate: 'bg-slate-100 text-slate-700',
  green: 'bg-emerald-100 text-emerald-700',
  amber: 'bg-amber-100 text-amber-700',
  red: 'bg-rose-100 text-rose-700',
  blue: 'bg-sky-100 text-sky-700',
  purple: 'bg-violet-100 text-violet-700',
};

export function Badge({ children, tone = 'slate' }: { children: ReactNode; tone?: Tone }) {
  return (
    <span className={clsx('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap', TONE_CLASSES[tone])}>
      {children}
    </span>
  );
}

const STATUS_TONE: Record<string, Tone> = {
  Active: 'green',
  Completed: 'green',
  Paid: 'green',
  Approved: 'green',
  Pending: 'amber',
  'Awaiting Review': 'amber',
  Draft: 'slate',
  Inactive: 'slate',
  Cancelled: 'slate',
  Overdue: 'red',
  Rejected: 'red',
  High: 'red',
  Medium: 'amber',
  Low: 'blue',
};

export function StatusBadge({ status }: { status: string }) {
  return <Badge tone={STATUS_TONE[status] ?? 'slate'}>{status}</Badge>;
}
