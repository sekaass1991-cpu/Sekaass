export type Role = 'admin' | 'client';

export interface User {
  id: string;
  role: Role;
  name: string;
  email: string;
  password: string;
  phone?: string;
  clientId?: string; // set for role === 'client'
  createdAt: string;
}

export type ClientStatus = 'Active' | 'Pending' | 'Inactive';
export type EntityType =
  | 'Individual'
  | 'Proprietorship'
  | 'Partnership'
  | 'LLP'
  | 'Private Limited'
  | 'Public Limited'
  | 'Artificial Juridical Person';

export interface Client {
  id: string;
  name: string;
  businessName: string;
  email: string;
  phone: string;
  pan?: string;
  gstin?: string;
  entityType: EntityType;
  category: string;
  status: ClientStatus;
  assignedConsultant: string;
  onboardedDate: string;
  address?: string;
  notes?: string;
}

export type InvoiceStatus = 'Draft' | 'Pending' | 'Paid' | 'Overdue';

export interface InvoiceItem {
  id: string;
  description: string;
  qty: number;
  rate: number;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  clientId: string;
  clientName: string;
  issueDate: string;
  dueDate: string;
  items: InvoiceItem[];
  amountPaid: number;
  status: InvoiceStatus;
  notes?: string;
}

export type ComplianceCategory = 'GST' | 'TDS' | 'ROC' | 'Income Tax' | 'Audit' | 'Other';
export type ComplianceStatus = 'Pending' | 'Completed' | 'Overdue';
export type Priority = 'Low' | 'Medium' | 'High';

export interface ComplianceItem {
  id: string;
  clientId: string;
  clientName: string;
  title: string;
  category: ComplianceCategory;
  dueDate: string;
  status: ComplianceStatus;
  assignedTo: string;
  priority: Priority;
  completedDate?: string;
}

export type LedgerAccount =
  | 'Revenue'
  | 'Accounts Receivable'
  | 'Accounts Payable'
  | 'Administrative Expenses'
  | 'Bank'
  | 'Cash'
  | 'Assets'
  | 'Liabilities';

export interface LedgerEntry {
  id: string;
  clientId?: string;
  clientName?: string;
  date: string;
  particulars: string;
  account: LedgerAccount;
  type: 'Debit' | 'Credit';
  amount: number;
}

export type DocumentCategory =
  | 'PAN'
  | 'Aadhaar'
  | 'GST Certificate'
  | 'Bank Statement'
  | 'Address Proof'
  | 'Financial Statement'
  | 'Other';

export interface DocumentRecord {
  id: string;
  clientId: string;
  clientName: string;
  name: string;
  category: DocumentCategory;
  uploadedBy: string;
  uploadedDate: string;
  status: 'Awaiting Review' | 'Approved' | 'Rejected';
  dataUrl?: string;
  size?: number;
}

export interface ChatMessage {
  id: string;
  sender: string;
  senderRole: Role;
  text: string;
  timestamp: string;
}

export interface MessageThread {
  id: string;
  clientId: string;
  clientName: string;
  subject: string;
  messages: ChatMessage[];
  updatedAt: string;
  unreadForAdmin: boolean;
  unreadForClient: boolean;
}

export type NotificationType = 'info' | 'success' | 'warning' | 'error';

export interface AppNotification {
  id: string;
  audience: Role | 'all';
  clientId?: string;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  createdAt: string;
  actionRequired?: boolean;
}

export interface AuditLogEntry {
  id: string;
  actor: string;
  actorRole: Role;
  action: string;
  entity: string;
  entityId?: string;
  details: string;
  timestamp: string;
}

export type ReminderFrequency = 'Once' | 'Daily' | 'Weekly' | 'Monthly';

export interface Reminder {
  id: string;
  clientId?: string;
  clientName?: string;
  title: string;
  dueDate: string;
  frequency: ReminderFrequency;
  channel: 'Email' | 'SMS' | 'In-App';
  status: 'Active' | 'Completed' | 'Cancelled';
}

export interface ServiceItem {
  id: string;
  name: string;
  category: string;
  description: string;
  price: number;
  active: boolean;
}

export interface Credential {
  id: string;
  clientId?: string;
  clientName?: string;
  portalName: string;
  username: string;
  password: string;
  notes?: string;
  updatedAt: string;
}

export interface FirmSettings {
  firmName: string;
  logoUrl?: string;
  autoBackupFrequency: 'Daily' | 'Weekly' | 'Monthly' | 'Off';
  autoReminders: boolean;
  emailjsServiceId?: string;
  emailjsTemplateId?: string;
  emailjsPublicKey?: string;
  aiApiKey?: string;
  aiApiEndpoint?: string;
}
