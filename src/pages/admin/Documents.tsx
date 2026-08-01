import { useMemo, useState, type ChangeEvent } from 'react';
import { Upload, Trash2, FolderOpen, CheckCircle2, XCircle, Download } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge, Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Input, Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { documentsStore, clientsStore, logAudit, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { DocumentCategory, DocumentRecord } from '../../types';

const CATEGORIES: DocumentCategory[] = ['PAN', 'Aadhaar', 'GST Certificate', 'Bank Statement', 'Address Proof', 'Financial Statement', 'Other'];
const MAX_SIZE = 1_000_000;

export function AdminDocuments() {
  const { user } = useAuth();
  const { show } = useToast();
  const documents = documentsStore.useAll();
  const clients = clientsStore.useAll();

  const [clientFilter, setClientFilter] = useState('All');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ clientId: '', name: '', category: 'Other' as DocumentCategory, dataUrl: '', size: 0 });

  const filtered = useMemo(() => (clientFilter === 'All' ? documents : documents.filter((d) => d.clientId === clientFilter)), [documents, clientFilter]);
  const sorted = useMemo(() => [...filtered].sort((a, b) => (a.uploadedDate < b.uploadedDate ? 1 : -1)), [filtered]);

  const openAdd = () => {
    setForm({ clientId: clients[0]?.id ?? '', name: '', category: 'Other', dataUrl: '', size: 0 });
    setModalOpen(true);
  };

  const onFile = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_SIZE) {
      show('File is too large for this demo (max 1MB).', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setForm((f) => ({ ...f, name: file.name, dataUrl: String(reader.result), size: file.size }));
    };
    reader.readAsDataURL(file);
  };

  const save = () => {
    const client = clients.find((c) => c.id === form.clientId);
    if (!client || !form.name) {
      show('Choose a client and a file.', 'error');
      return;
    }
    const id = genId('doc');
    const doc: DocumentRecord = {
      id,
      clientId: client.id,
      clientName: client.businessName,
      name: form.name,
      category: form.category,
      uploadedBy: user?.name ?? 'Admin',
      uploadedDate: new Date().toISOString(),
      status: 'Approved',
      dataUrl: form.dataUrl,
      size: form.size,
    };
    documentsStore.add(doc);
    logAudit(user?.name ?? 'Admin', 'admin', 'Uploaded document', 'Document', id, `Uploaded ${form.name} for ${client.businessName}`);
    show('Document uploaded.', 'success');
    setModalOpen(false);
  };

  const setStatus = (doc: DocumentRecord, status: DocumentRecord['status']) => {
    documentsStore.update(doc.id, { status });
    logAudit(user?.name ?? 'Admin', 'admin', `Document ${status.toLowerCase()}`, 'Document', doc.id, `${doc.name} marked ${status}`);
    pushNotification('client', `Document ${status}`, `${doc.name} was ${status.toLowerCase()}.`, status === 'Approved' ? 'success' : 'warning', doc.clientId);
    show(`Document ${status.toLowerCase()}.`, 'success');
  };

  const remove = (doc: DocumentRecord) => {
    if (!confirm(`Delete ${doc.name}?`)) return;
    documentsStore.remove(doc.id);
    show('Document deleted.', 'success');
  };

  const columns: Column<DocumentRecord>[] = [
    { header: 'Document', render: (d) => <span className="font-medium text-slate-900">{d.name}</span> },
    { header: 'Client', render: (d) => d.clientName },
    { header: 'Category', render: (d) => <Badge tone="blue">{d.category}</Badge> },
    { header: 'Uploaded By', render: (d) => d.uploadedBy },
    { header: 'Date', render: (d) => formatDate(d.uploadedDate) },
    { header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
    {
      header: 'Actions',
      render: (d) => (
        <div className="flex gap-1">
          {d.dataUrl && (
            <a href={d.dataUrl} download={d.name} title="Download" className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100">
              <Download className="size-4" />
            </a>
          )}
          {d.status !== 'Approved' && (
            <button onClick={() => setStatus(d, 'Approved')} title="Approve" className="rounded-md p-1.5 text-emerald-600 hover:bg-emerald-50">
              <CheckCircle2 className="size-4" />
            </button>
          )}
          {d.status !== 'Rejected' && (
            <button onClick={() => setStatus(d, 'Rejected')} title="Reject" className="rounded-md p-1.5 text-amber-600 hover:bg-amber-50">
              <XCircle className="size-4" />
            </button>
          )}
          <button onClick={() => remove(d)} title="Delete" className="rounded-md p-1.5 text-rose-500 hover:bg-rose-50">
            <Trash2 className="size-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <select value={clientFilter} onChange={(e) => setClientFilter(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="All">All clients</option>
          {clients.map((c) => (
            <option key={c.id} value={c.id}>
              {c.businessName}
            </option>
          ))}
        </select>
        <Button onClick={openAdd}>
          <Upload className="size-4" /> Upload Document
        </Button>
      </div>

      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<FolderOpen className="size-5" />} title="No documents" message="Upload a document to share it with a client." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(d) => d.id} />
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Upload Document"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Upload</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Select label="Client" required value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.businessName}
              </option>
            ))}
          </Select>
          <Select label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as DocumentCategory })}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <Input label="File" type="file" required onChange={onFile} />
          {form.name && <p className="text-xs text-slate-500">Selected: {form.name}</p>}
        </div>
      </Modal>
    </div>
  );
}
