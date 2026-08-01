import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';

const baseClass =
  'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500';

function Wrapper({ label, required, children }: { label?: string; required?: boolean; children: ReactNode }) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1 block text-xs font-medium text-slate-600">
          {label}
          {required && <span className="text-rose-500"> *</span>}
        </span>
      )}
      {children}
    </label>
  );
}

export function Input({
  label,
  required,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label?: string }) {
  return (
    <Wrapper label={label} required={required}>
      <input className={`${baseClass} ${className ?? ''}`} required={required} {...props} />
    </Wrapper>
  );
}

export function Textarea({
  label,
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string }) {
  return (
    <Wrapper label={label} required={props.required}>
      <textarea className={`${baseClass} ${className ?? ''}`} {...props} />
    </Wrapper>
  );
}

export function Select({
  label,
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { label?: string }) {
  return (
    <Wrapper label={label} required={props.required}>
      <select className={`${baseClass} bg-white ${className ?? ''}`} {...props}>
        {children}
      </select>
    </Wrapper>
  );
}
