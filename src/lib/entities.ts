import { createCollection, genId } from './storage';
import type {
  User,
  Client,
  Invoice,
  ComplianceItem,
  LedgerEntry,
  DocumentRecord,
  MessageThread,
  AppNotification,
  AuditLogEntry,
  Reminder,
  ServiceItem,
  Credential,
  Role,
} from '../types';

export const usersStore = createCollection<User>('users');
export const clientsStore = createCollection<Client>('clients');
export const invoicesStore = createCollection<Invoice>('invoices');
export const complianceStore = createCollection<ComplianceItem>('compliance');
export const ledgerStore = createCollection<LedgerEntry>('ledger');
export const documentsStore = createCollection<DocumentRecord>('documents');
export const messagesStore = createCollection<MessageThread>('messages');
export const notificationsStore = createCollection<AppNotification>('notifications');
export const auditStore = createCollection<AuditLogEntry>('audit');
export const remindersStore = createCollection<Reminder>('reminders');
export const servicesStore = createCollection<ServiceItem>('services');
export const credentialsStore = createCollection<Credential>('credentials');

export function logAudit(actor: string, actorRole: Role, action: string, entity: string, entityId: string | undefined, details: string) {
  auditStore.add({
    id: genId('audit'),
    actor,
    actorRole,
    action,
    entity,
    entityId,
    details,
    timestamp: new Date().toISOString(),
  });
}

export function pushNotification(
  audience: Role | 'all',
  title: string,
  message: string,
  type: AppNotification['type'] = 'info',
  clientId?: string,
  actionRequired = false,
) {
  notificationsStore.add({
    id: genId('notif'),
    audience,
    clientId,
    title,
    message,
    type,
    read: false,
    createdAt: new Date().toISOString(),
    actionRequired,
  });
}
