import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { CLIENT_NAV } from './nav';

export function ClientLayout() {
  const location = useLocation();
  const current = CLIENT_NAV.find((i) => (i.end ? location.pathname === i.to : location.pathname.startsWith(i.to)));

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <Sidebar items={CLIENT_NAV} portalLabel="Client Portal" />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar title={current?.label ?? 'Client Portal'} items={CLIENT_NAV} portalLabel="Client Portal" />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
