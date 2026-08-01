import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, LogOut, Menu, UserCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { notificationsStore } from '../../lib/entities';
import { formatDateTime } from '../../lib/format';
import type { NavItem } from './nav';
import { Sidebar } from './Sidebar';

export function Topbar({ title, items, portalLabel }: { title: string; items: NavItem[]; portalLabel: string }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const notifications = notificationsStore.useAll();
  const [showNotifs, setShowNotifs] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [showMobileNav, setShowMobileNav] = useState(false);

  const myNotifications = useMemo(() => {
    if (!user) return [];
    return notifications
      .filter((n) => n.audience === 'all' || n.audience === user.role || (user.role === 'client' && n.clientId === user.clientId))
      .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1));
  }, [notifications, user]);

  const unreadCount = myNotifications.filter((n) => !n.read).length;

  const markAllRead = () => {
    myNotifications.forEach((n) => {
      if (!n.read) notificationsStore.update(n.id, { read: true });
    });
  };

  return (
    <header
      className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur lg:px-6"
      style={{ paddingTop: 'max(0.75rem, calc(0.75rem + env(safe-area-inset-top)))' }}
    >
      <div className="flex items-center gap-3">
        <button className="text-slate-500 lg:hidden" onClick={() => setShowMobileNav(true)}>
          <Menu className="size-5" />
        </button>
        <h1 className="text-base font-semibold text-slate-900">{title}</h1>
      </div>
      <div className="flex items-center gap-2">
        <div className="relative">
          <button
            className="relative flex size-9 items-center justify-center rounded-full text-slate-500 hover:bg-slate-100"
            onClick={() => {
              setShowNotifs((v) => !v);
              setShowMenu(false);
            }}
          >
            <Bell className="size-5" />
            {unreadCount > 0 && (
              <span className="absolute right-1.5 top-1.5 flex size-4 items-center justify-center rounded-full bg-rose-500 text-[10px] font-semibold text-white">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
          {showNotifs && (
            <div className="absolute right-0 mt-2 w-80 rounded-xl border border-slate-200 bg-white shadow-lg">
              <div className="flex items-center justify-between border-b border-slate-100 p-3">
                <p className="text-sm font-semibold text-slate-900">Notifications</p>
                <button onClick={markAllRead} className="text-xs font-medium text-indigo-600 hover:underline">
                  Mark all read
                </button>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {myNotifications.length === 0 && <p className="p-4 text-center text-xs text-slate-400">You're all caught up.</p>}
                {myNotifications.slice(0, 8).map((n) => (
                  <div key={n.id} className={`border-b border-slate-50 p-3 ${n.read ? '' : 'bg-indigo-50/50'}`}>
                    <p className="text-xs font-medium text-slate-900">{n.title}</p>
                    <p className="mt-0.5 text-xs text-slate-500">{n.message}</p>
                    <p className="mt-1 text-[10px] text-slate-400">{formatDateTime(n.createdAt)}</p>
                  </div>
                ))}
              </div>
              <button
                onClick={() => {
                  setShowNotifs(false);
                  navigate(user?.role === 'admin' ? '/admin/notifications' : '/client/notifications');
                }}
                className="w-full rounded-b-xl border-t border-slate-100 p-2 text-center text-xs font-medium text-indigo-600 hover:bg-slate-50"
              >
                View all
              </button>
            </div>
          )}
        </div>
        <div className="relative">
          <button
            className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-slate-100"
            onClick={() => {
              setShowMenu((v) => !v);
              setShowNotifs(false);
            }}
          >
            <UserCircle className="size-7 text-slate-400" />
            <span className="hidden text-sm font-medium text-slate-700 sm:inline">{user?.name}</span>
          </button>
          {showMenu && (
            <div className="absolute right-0 mt-2 w-48 rounded-xl border border-slate-200 bg-white p-1 shadow-lg">
              <button
                onClick={() => {
                  setShowMenu(false);
                  navigate(user?.role === 'admin' ? '/admin/profile' : '/client/profile');
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-600 hover:bg-slate-50"
              >
                <UserCircle className="size-4" /> Profile
              </button>
              <button
                onClick={() => {
                  logout();
                  navigate('/login');
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-rose-600 hover:bg-rose-50"
              >
                <LogOut className="size-4" /> Log out
              </button>
            </div>
          )}
        </div>
      </div>

      {showMobileNav && (
        <div className="fixed inset-0 z-30 flex lg:hidden">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setShowMobileNav(false)} />
          <div className="relative flex">
            <Sidebar items={items} portalLabel={portalLabel} mobile />
          </div>
        </div>
      )}
    </header>
  );
}
