import type { ReactNode } from 'react';
import clsx from 'clsx';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: ReactNode;
  tone?: 'indigo' | 'emerald' | 'amber' | 'rose' | 'sky';
  hint?: string;
}

const TONE_CLASSES = {
  indigo: 'bg-indigo-50 text-indigo-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  rose: 'bg-rose-50 text-rose-600',
  sky: 'bg-sky-50 text-sky-600',
};

export function StatCard({ label, value, icon, tone = 'indigo', hint }: StatCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-500">{label}</p>
        <div className={clsx('flex size-8 items-center justify-center rounded-lg', TONE_CLASSES[tone])}>{icon}</div>
      </div>
      <p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
    </div>
  );
}
