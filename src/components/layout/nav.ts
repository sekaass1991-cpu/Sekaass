import type { ComponentType } from 'react';
import {
  LayoutDashboard,
  Users,
  FileText,
  ShieldCheck,
  BookOpen,
  PieChart,
  FolderOpen,
  MessageSquare,
  Bell,
  History,
  AlarmClock,
  Sparkles,
  Briefcase,
  ClipboardList,
  ListChecks,
  KeyRound,
  Settings,
  UserCircle,
} from 'lucide-react';

export interface NavItem {
  to: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  end?: boolean;
}

export const ADMIN_NAV: NavItem[] = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/clients', label: 'Clients', icon: Users },
  { to: '/admin/invoices', label: 'Invoices', icon: FileText },
  { to: '/admin/invoices/advanced', label: 'Advanced Invoices', icon: ClipboardList },
  { to: '/admin/compliance', label: 'Compliance', icon: ShieldCheck },
  { to: '/admin/ledger', label: 'Ledger', icon: BookOpen },
  { to: '/admin/financials', label: 'Financials', icon: PieChart },
  { to: '/admin/statements', label: 'Statements', icon: FileText },
  { to: '/admin/documents', label: 'Documents', icon: FolderOpen },
  { to: '/admin/messages', label: 'Messages', icon: MessageSquare },
  { to: '/admin/notifications', label: 'Notifications', icon: Bell },
  { to: '/admin/audit-trail', label: 'Audit Trail', icon: History },
  { to: '/admin/reminders', label: 'Reminders', icon: AlarmClock },
  { to: '/admin/ai', label: 'AI Assistant', icon: Sparkles },
  { to: '/admin/services', label: 'Services', icon: Briefcase },
  { to: '/admin/worklist', label: 'Worklist', icon: ListChecks },
  { to: '/admin/credentials', label: 'Credentials', icon: KeyRound },
  { to: '/admin/settings', label: 'Settings', icon: Settings },
  { to: '/admin/profile', label: 'Profile', icon: UserCircle },
];

export const CLIENT_NAV: NavItem[] = [
  { to: '/client', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/client/documents', label: 'Documents', icon: FolderOpen },
  { to: '/client/messages', label: 'Messages', icon: MessageSquare },
  { to: '/client/notifications', label: 'Notifications', icon: Bell },
  { to: '/client/services', label: 'Services', icon: Briefcase },
  { to: '/client/ai', label: 'AI Assistant', icon: Sparkles },
  { to: '/client/settings', label: 'Settings', icon: Settings },
  { to: '/client/profile', label: 'Profile', icon: UserCircle },
];
