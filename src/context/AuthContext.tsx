import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { writeRaw, useRaw, genId } from '../lib/storage';
import { usersStore, clientsStore, logAudit, pushNotification } from '../lib/entities';
import type { User, Role } from '../types';

interface AuthContextValue {
  user: User | null;
  login(email: string, password: string, role: Role): { ok: boolean; error?: string };
  registerClient(input: { name: string; businessName: string; email: string; phone: string; password: string }): { ok: boolean; error?: string };
  logout(): void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const SESSION_KEY = 'session_user_id';

export function AuthProvider({ children }: { children: ReactNode }) {
  const userId = useRaw<string | null>(SESSION_KEY, null);
  const users = usersStore.useAll();

  const user = useMemo(() => users.find((u) => u.id === userId) ?? null, [users, userId]);

  const value: AuthContextValue = {
    user,
    login(email, password, role) {
      const found = usersStore.getAll().find((u) => u.email.toLowerCase() === email.toLowerCase() && u.role === role);
      if (!found || found.password !== password) {
        return { ok: false, error: 'Invalid email or password.' };
      }
      writeRaw(SESSION_KEY, found.id);
      logAudit(found.name, found.role, 'Logged in', 'Auth', found.id, `${found.name} logged in`);
      return { ok: true };
    },
    registerClient({ name, businessName, email, phone, password }) {
      const existing = usersStore.getAll().find((u) => u.email.toLowerCase() === email.toLowerCase());
      if (existing) {
        return { ok: false, error: 'An account with this email already exists.' };
      }
      const clientId = genId('client');
      clientsStore.add({
        id: clientId,
        name,
        businessName,
        email,
        phone,
        entityType: 'Individual',
        category: 'General',
        status: 'Pending',
        assignedConsultant: 'Unassigned',
        onboardedDate: new Date().toISOString(),
      });
      const newUser: User = {
        id: genId('user'),
        role: 'client',
        name,
        email,
        password,
        phone,
        clientId,
        createdAt: new Date().toISOString(),
      };
      usersStore.add(newUser);
      writeRaw(SESSION_KEY, newUser.id);
      logAudit(name, 'client', 'Registered account', 'Client', clientId, `${name} registered a new client account`);
      pushNotification('admin', 'New client registration', `${businessName} (${name}) has registered and is pending review.`, 'info', clientId, true);
      pushNotification(
        'client',
        'Welcome to TaxTitan Consultancy',
        `Hi ${name}, your account for ${businessName} has been created and is pending review by our team. We'll be in touch shortly.`,
        'success',
        clientId,
      );
      return { ok: true };
    },
    logout() {
      writeRaw(SESSION_KEY, null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
