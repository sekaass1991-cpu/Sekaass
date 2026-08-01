import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { ADMIN_NAV } from './nav';

export function AdminLayout() {
  const location = useLocation();
  const current = ADMIN_NAV.find((i) => (i.end ? location.pathname === i.to : location.pathname.startsWith(i.to)));

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <Sidebar items={ADMIN_NAV} portalLabel="Admin Portal" />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar title={current?.label ?? 'Admin Portal'} items={ADMIN_NAV} portalLabel="Admin Portal" />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
