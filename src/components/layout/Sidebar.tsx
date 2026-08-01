import { NavLink } from 'react-router-dom';
import clsx from 'clsx';
import type { NavItem } from './nav';
import { useSettings } from '../../lib/settings';
import { BrandMark } from '../ui/BrandMark';

export function Sidebar({ items, portalLabel, mobile }: { items: NavItem[]; portalLabel: string; mobile?: boolean }) {
  const settings = useSettings();

  return (
    <aside className={clsx('w-64 shrink-0 flex-col border-r border-slate-200 bg-white', mobile ? 'flex' : 'hidden lg:flex')}>
      <div className="flex items-center gap-2 border-b border-slate-100 px-5 py-5">
        <BrandMark logoUrl={settings.logoUrl} size={36} />
        <div>
          <p className="text-sm font-semibold text-slate-900">{settings.firmName}</p>
          <p className="text-xs text-slate-400">{portalLabel}</p>
        </div>
      </div>
      <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-50',
              )
            }
          >
            <item.icon className="size-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
