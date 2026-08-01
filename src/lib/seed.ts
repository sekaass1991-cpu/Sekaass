import { genId, readRaw, writeRaw } from './storage';
import {
  usersStore,
  clientsStore,
  invoicesStore,
  complianceStore,
  ledgerStore,
  documentsStore,
  messagesStore,
  notificationsStore,
  auditStore,
  remindersStore,
  servicesStore,
  credentialsStore,
} from './entities';
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
  FirmSettings,
} from '../types';

const SEED_FLAG = 'seeded_v1';

function isoDaysFromNow(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString();
}

export function ensureSeedData() {
  if (readRaw(SEED_FLAG, false)) return;

  const clients: Client[] = [
    {
      id: 'client_1',
      name: 'Arjun Kumar',
      businessName: 'Arjun Kumar Textiles',
      email: 'arjun.kumar@example.com',
      phone: '+91 98765 43210',
      pan: 'ABCDE1234F',
      gstin: '27ABCDE1234F1Z5',
      entityType: 'Proprietorship',
      category: 'Retail & Trading',
      status: 'Active',
      assignedConsultant: 'Priya Sharma',
      onboardedDate: isoDaysFromNow(-320),
      address: 'MG Road, Pune, Maharashtra',
    },
    {
      id: 'client_2',
      name: 'Meera Nair',
      businessName: 'Nair Design Studio LLP',
      email: 'meera.nair@example.com',
      phone: '+91 98220 11223',
      pan: 'BCDEF2345G',
      gstin: '32BCDEF2345G1Z8',
      entityType: 'LLP',
      category: 'Architectural Services',
      status: 'Active',
      assignedConsultant: 'Rohit Verma',
      onboardedDate: isoDaysFromNow(-210),
      address: 'Marine Drive, Kochi, Kerala',
    },
    {
      id: 'client_3',
      name: 'Sanjay Gupta',
      businessName: 'Gupta Infra Private Limited',
      email: 'sanjay.gupta@example.com',
      phone: '+91 90000 12345',
      pan: 'CDEFG3456H',
      gstin: '09CDEFG3456H1Z2',
      entityType: 'Private Limited',
      category: 'Construction',
      status: 'Pending',
      assignedConsultant: 'Priya Sharma',
      onboardedDate: isoDaysFromNow(-15),
      address: 'Sector 62, Noida, Uttar Pradesh',
    },
    {
      id: 'client_4',
      name: 'Fatima Sheikh',
      businessName: 'Sheikh Exports',
      email: 'fatima.sheikh@example.com',
      phone: '+91 91234 56789',
      pan: 'DEFGH4567I',
      gstin: '24DEFGH4567I1Z9',
      entityType: 'Partnership',
      category: 'Import / Export',
      status: 'Active',
      assignedConsultant: 'Rohit Verma',
      onboardedDate: isoDaysFromNow(-540),
      address: 'Ashram Road, Ahmedabad, Gujarat',
    },
    {
      id: 'client_5',
      name: 'Karan Malhotra',
      businessName: 'Malhotra & Associates',
      email: 'karan.malhotra@example.com',
      phone: '+91 99887 66554',
      pan: 'EFGHI5678J',
      gstin: '06EFGHI5678J1Z3',
      entityType: 'Individual',
      category: 'Consulting',
      status: 'Inactive',
      assignedConsultant: 'Priya Sharma',
      onboardedDate: isoDaysFromNow(-680),
      address: 'Cyber City, Gurugram, Haryana',
    },
  ];

  const users: User[] = [
    {
      id: 'user_admin',
      role: 'admin',
      name: 'Admin User',
      email: 'admin@sekaass.com',
      password: 'admin123',
      phone: '+91 90000 00001',
      createdAt: isoDaysFromNow(-700),
    },
    {
      id: 'user_client_1',
      role: 'client',
      name: 'Arjun Kumar',
      email: 'arjun.kumar@example.com',
      password: 'client123',
      clientId: 'client_1',
      createdAt: isoDaysFromNow(-320),
    },
  ];

  const services: ServiceItem[] = [
    { id: genId('svc'), name: 'GST Registration', category: 'Registration', description: 'End-to-end GST registration and compliance setup', price: 3999, active: true },
    { id: genId('svc'), name: 'PAN & TAN Application', category: 'Registration', description: 'Apply for PAN and TAN with documentation support', price: 1499, active: true },
    { id: genId('svc'), name: 'Business Registration', category: 'Registration', description: 'Business registration services for new entities', price: 7999, active: true },
    { id: genId('svc'), name: 'Monthly GST Filing', category: 'Compliance', description: 'Monthly GSTR-1 and GSTR-3B filing', price: 1999, active: true },
    { id: genId('svc'), name: 'TDS Return Filing', category: 'Compliance', description: 'Quarterly TDS return preparation and filing', price: 2499, active: true },
    { id: genId('svc'), name: 'Annual Financial Audit', category: 'Audit', description: 'Audit and assurance services for statutory compliance', price: 24999, active: true },
    { id: genId('svc'), name: 'ROC Annual Filing', category: 'Compliance', description: 'Annual ROC filings for companies and LLPs', price: 5999, active: true },
    { id: genId('svc'), name: 'Asset Advisory', category: 'Advisory', description: 'Asset management advisory services', price: 9999, active: true },
    { id: genId('svc'), name: 'Business Consultancy', category: 'Advisory', description: 'Business and management consultancy services', price: 6999, active: false },
    { id: genId('svc'), name: 'Bookkeeping', category: 'Accounting', description: 'Monthly bookkeeping and ledger maintenance', price: 2999, active: true },
  ];

  const invoices: Invoice[] = [
    {
      id: genId('inv'),
      invoiceNumber: 'INV-2026-001',
      clientId: 'client_1',
      clientName: 'Arjun Kumar Textiles',
      issueDate: isoDaysFromNow(-30),
      dueDate: isoDaysFromNow(-2),
      items: [
        { id: genId('item'), description: 'Monthly GST Filing', qty: 1, rate: 1999 },
        { id: genId('item'), description: 'Bookkeeping', qty: 1, rate: 2999 },
      ],
      amountPaid: 0,
      status: 'Overdue',
    },
    {
      id: genId('inv'),
      invoiceNumber: 'INV-2026-002',
      clientId: 'client_2',
      clientName: 'Nair Design Studio LLP',
      issueDate: isoDaysFromNow(-15),
      dueDate: isoDaysFromNow(15),
      items: [{ id: genId('item'), description: 'ROC Annual Filing', qty: 1, rate: 5999 }],
      amountPaid: 5999,
      status: 'Paid',
    },
    {
      id: genId('inv'),
      invoiceNumber: 'INV-2026-003',
      clientId: 'client_3',
      clientName: 'Gupta Infra Private Limited',
      issueDate: isoDaysFromNow(-5),
      dueDate: isoDaysFromNow(25),
      items: [{ id: genId('item'), description: 'Business Registration', qty: 1, rate: 7999 }],
      amountPaid: 3000,
      status: 'Pending',
    },
    {
      id: genId('inv'),
      invoiceNumber: 'INV-2026-004',
      clientId: 'client_4',
      clientName: 'Sheikh Exports',
      issueDate: isoDaysFromNow(-60),
      dueDate: isoDaysFromNow(-30),
      items: [{ id: genId('item'), description: 'Annual Financial Audit', qty: 1, rate: 24999 }],
      amountPaid: 24999,
      status: 'Paid',
    },
    {
      id: genId('inv'),
      invoiceNumber: 'INV-2026-005',
      clientId: 'client_1',
      clientName: 'Arjun Kumar Textiles',
      issueDate: isoDaysFromNow(-1),
      dueDate: isoDaysFromNow(29),
      items: [{ id: genId('item'), description: 'TDS Return Filing', qty: 1, rate: 2499 }],
      amountPaid: 0,
      status: 'Draft',
    },
  ];

  const compliance: ComplianceItem[] = [
    { id: genId('cmp'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', title: 'GSTR-3B Filing', category: 'GST', dueDate: isoDaysFromNow(5), status: 'Pending', assignedTo: 'Priya Sharma', priority: 'High' },
    { id: genId('cmp'), clientId: 'client_2', clientName: 'Nair Design Studio LLP', title: 'TDS Return Q1', category: 'TDS', dueDate: isoDaysFromNow(-3), status: 'Overdue', assignedTo: 'Rohit Verma', priority: 'High' },
    { id: genId('cmp'), clientId: 'client_3', clientName: 'Gupta Infra Private Limited', title: 'ROC Annual Return', category: 'ROC', dueDate: isoDaysFromNow(20), status: 'Pending', assignedTo: 'Priya Sharma', priority: 'Medium' },
    { id: genId('cmp'), clientId: 'client_4', clientName: 'Sheikh Exports', title: 'Annual Financial Audit', category: 'Audit', dueDate: isoDaysFromNow(-40), status: 'Completed', assignedTo: 'Rohit Verma', priority: 'Medium', completedDate: isoDaysFromNow(-38) },
    { id: genId('cmp'), clientId: 'client_5', clientName: 'Malhotra & Associates', title: 'Income Tax Return Filing', category: 'Income Tax', dueDate: isoDaysFromNow(45), status: 'Pending', assignedTo: 'Priya Sharma', priority: 'Low' },
    { id: genId('cmp'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', title: 'GST Annual Reconciliation', category: 'GST', dueDate: isoDaysFromNow(60), status: 'Pending', assignedTo: 'Rohit Verma', priority: 'Medium' },
  ];

  const ledger: LedgerEntry[] = [
    { id: genId('led'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', date: isoDaysFromNow(-30), particulars: 'Service invoice raised', account: 'Revenue', type: 'Credit', amount: 4998 },
    { id: genId('led'), clientId: 'client_2', clientName: 'Nair Design Studio LLP', date: isoDaysFromNow(-15), particulars: 'Payment received', account: 'Bank', type: 'Debit', amount: 5999 },
    { id: genId('led'), clientId: 'client_3', clientName: 'Gupta Infra Private Limited', date: isoDaysFromNow(-5), particulars: 'Partial payment received', account: 'Bank', type: 'Debit', amount: 3000 },
    { id: genId('led'), clientId: 'client_4', clientName: 'Sheikh Exports', date: isoDaysFromNow(-60), particulars: 'Audit fee invoiced', account: 'Revenue', type: 'Credit', amount: 24999 },
    { id: genId('led'), date: isoDaysFromNow(-10), particulars: 'Office rent', account: 'Administrative Expenses', type: 'Debit', amount: 45000 },
    { id: genId('led'), date: isoDaysFromNow(-8), particulars: 'Software subscriptions', account: 'Administrative Expenses', type: 'Debit', amount: 8200 },
    { id: genId('led'), date: isoDaysFromNow(-2), particulars: 'Staff salaries', account: 'Administrative Expenses', type: 'Debit', amount: 180000 },
  ];

  const documents: DocumentRecord[] = [
    { id: genId('doc'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', name: 'PAN Card.pdf', category: 'PAN', uploadedBy: 'Arjun Kumar', uploadedDate: isoDaysFromNow(-300), status: 'Approved' },
    { id: genId('doc'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', name: 'Aadhaar Copy.pdf', category: 'Aadhaar', uploadedBy: 'Arjun Kumar', uploadedDate: isoDaysFromNow(-300), status: 'Approved' },
    { id: genId('doc'), clientId: 'client_3', clientName: 'Gupta Infra Private Limited', name: 'Bank Statement - March.pdf', category: 'Bank Statement', uploadedBy: 'Sanjay Gupta', uploadedDate: isoDaysFromNow(-4), status: 'Awaiting Review' },
    { id: genId('doc'), clientId: 'client_2', clientName: 'Nair Design Studio LLP', name: 'GST Certificate.pdf', category: 'GST Certificate', uploadedBy: 'Meera Nair', uploadedDate: isoDaysFromNow(-200), status: 'Approved' },
    { id: genId('doc'), clientId: 'client_4', clientName: 'Sheikh Exports', name: 'FY25 Financial Statement.pdf', category: 'Financial Statement', uploadedBy: 'Fatima Sheikh', uploadedDate: isoDaysFromNow(-40), status: 'Approved' },
  ];

  const messages: MessageThread[] = [
    {
      id: genId('thr'),
      clientId: 'client_1',
      clientName: 'Arjun Kumar Textiles',
      subject: 'GSTR-3B due date reminder',
      updatedAt: isoDaysFromNow(-1),
      unreadForAdmin: true,
      unreadForClient: false,
      messages: [
        { id: genId('msg'), sender: 'Priya Sharma', senderRole: 'admin', text: 'Hi Arjun, your GSTR-3B filing is due in 5 days. Please share the latest sales register.', timestamp: isoDaysFromNow(-2) },
        { id: genId('msg'), sender: 'Arjun Kumar', senderRole: 'client', text: 'Sure, uploading it today.', timestamp: isoDaysFromNow(-1) },
      ],
    },
    {
      id: genId('thr'),
      clientId: 'client_3',
      clientName: 'Gupta Infra Private Limited',
      subject: 'Documents for business registration',
      updatedAt: isoDaysFromNow(-3),
      unreadForAdmin: false,
      unreadForClient: true,
      messages: [
        { id: genId('msg'), sender: 'Sanjay Gupta', senderRole: 'client', text: 'When will the registration be complete?', timestamp: isoDaysFromNow(-3) },
      ],
    },
  ];

  const notifications: AppNotification[] = [
    { id: genId('notif'), audience: 'admin', title: 'Invoice overdue', message: 'INV-2026-001 for Arjun Kumar Textiles is overdue.', type: 'warning', read: false, createdAt: isoDaysFromNow(-1), actionRequired: true },
    { id: genId('notif'), audience: 'admin', title: 'New document uploaded', message: 'Gupta Infra Private Limited uploaded a bank statement.', type: 'info', read: false, createdAt: isoDaysFromNow(-4) },
    { id: genId('notif'), audience: 'client', clientId: 'client_1', title: 'Compliance due soon', message: 'GSTR-3B filing is due in 5 days.', type: 'warning', read: false, createdAt: isoDaysFromNow(-2), actionRequired: true },
    { id: genId('notif'), audience: 'client', clientId: 'client_1', title: 'Invoice generated', message: 'A new invoice INV-2026-005 has been generated.', type: 'info', read: false, createdAt: isoDaysFromNow(-1) },
  ];

  const audit: AuditLogEntry[] = [
    { id: genId('audit'), actor: 'Admin User', actorRole: 'admin', action: 'Created client', entity: 'Client', entityId: 'client_3', details: 'Added Gupta Infra Private Limited as a new client', timestamp: isoDaysFromNow(-15) },
    { id: genId('audit'), actor: 'Admin User', actorRole: 'admin', action: 'Generated invoice', entity: 'Invoice', entityId: invoices[3].id, details: 'Created INV-2026-004 for Sheikh Exports', timestamp: isoDaysFromNow(-60) },
    { id: genId('audit'), actor: 'Arjun Kumar', actorRole: 'client', action: 'Uploaded document', entity: 'Document', entityId: documents[0].id, details: 'Uploaded PAN Card.pdf', timestamp: isoDaysFromNow(-300) },
  ];

  const reminders: Reminder[] = [
    { id: genId('rem'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', title: 'GSTR-3B filing reminder', dueDate: isoDaysFromNow(5), frequency: 'Once', channel: 'Email', status: 'Active' },
    { id: genId('rem'), clientId: 'client_2', clientName: 'Nair Design Studio LLP', title: 'TDS return overdue follow-up', dueDate: isoDaysFromNow(-3), frequency: 'Once', channel: 'Email', status: 'Active' },
    { id: genId('rem'), title: 'Monthly compliance digest', dueDate: isoDaysFromNow(2), frequency: 'Monthly', channel: 'In-App', status: 'Active' },
  ];

  const credentials: Credential[] = [
    { id: genId('cred'), clientId: 'client_1', clientName: 'Arjun Kumar Textiles', portalName: 'GST Portal', username: 'arjunk_gst', password: 'demo-password', updatedAt: isoDaysFromNow(-90) },
    { id: genId('cred'), clientId: 'client_2', clientName: 'Nair Design Studio LLP', portalName: 'Income Tax Portal', username: 'nair_llp_it', password: 'demo-password', updatedAt: isoDaysFromNow(-60) },
    { id: genId('cred'), portalName: 'MCA Portal (Firm)', username: 'sekaass_firm', password: 'demo-password', updatedAt: isoDaysFromNow(-200), notes: 'Shared firm-level MCA login' },
  ];

  const settings: FirmSettings = {
    firmName: 'Sekaass & Associates',
    autoBackupFrequency: 'Weekly',
    autoReminders: true,
  };

  usersStore.set(users);
  clientsStore.set(clients);
  invoicesStore.set(invoices);
  complianceStore.set(compliance);
  ledgerStore.set(ledger);
  documentsStore.set(documents);
  messagesStore.set(messages);
  notificationsStore.set(notifications);
  auditStore.set(audit);
  remindersStore.set(reminders);
  servicesStore.set(services);
  credentialsStore.set(credentials);
  writeRaw('settings', settings);

  writeRaw(SEED_FLAG, true);
}
