import { useState, type ChangeEvent } from 'react';
import { Upload, FolderOpen, Download } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { StatusBadge, Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { Select } from '../../components/ui/Field';
import { EmptyState } from '../../components/ui/EmptyState';
import { useAuth } from '../../context/AuthContext';
import { documentsStore, clientsStore, logAudit, pushNotification } from '../../lib/entities';
import { genId } from '../../lib/storage';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../lib/format';
import type { DocumentCategory, DocumentRecord } from '../../types';

const CATEGORIES: DocumentCategory[] = ['PAN', 'Aadhaar', 'GST Certificate', 'Bank Statement', 'Address Proof', 'Financial Statement', 'Other'];
const MAX_SIZE = 1_000_000;

export function ClientDocuments() {
  const { user } = useAuth();
  const { show } = useToast();
  const documents = documentsStore.useAll().filter((d) => d.clientId === user?.clientId);
  const clients = clientsStore.useAll();
  const client = clients.find((c) => c.id === user?.clientId);

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({ name: '', category: 'Other' as DocumentCategory, dataUrl: '', size: 0 });

  const onFile = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_SIZE) {
      show('File is too large for this demo (max 1MB).', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setForm((f) => ({ ...f, name: file.name, dataUrl: String(reader.result), size: file.size }));
    reader.readAsDataURL(file);
  };

  const save = () => {
    if (!client || !form.name) {
      show('Please choose a file to upload.', 'error');
      return;
    }
    const id = genId('doc');
    documentsStore.add({
      id,
      clientId: client.id,
      clientName: client.businessName,
      name: form.name,
      category: form.category,
      uploadedBy: user?.name ?? 'Client',
      uploadedDate: new Date().toISOString(),
      status: 'Awaiting Review',
      dataUrl: form.dataUrl,
      size: form.size,
    });
    logAudit(user?.name ?? 'Client', 'client', 'Uploaded document', 'Document', id, `Uploaded ${form.name}`);
    pushNotification('admin', 'New document uploaded', `${client.businessName} uploaded ${form.name}.`, 'info', client.id, true);
    show('Document uploaded for review.', 'success');
    setModalOpen(false);
    setForm({ name: '', category: 'Other', dataUrl: '', size: 0 });
  };

  const columns: Column<DocumentRecord>[] = [
    { header: 'Document', render: (d) => <span className="font-medium text-slate-900">{d.name}</span> },
    { header: 'Category', render: (d) => <Badge tone="blue">{d.category}</Badge> },
    { header: 'Uploaded', render: (d) => formatDate(d.uploadedDate) },
    { header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
    {
      header: '',
      render: (d) =>
        d.dataUrl ? (
          <a href={d.dataUrl} download={d.name} className="flex items-center gap-1 text-xs font-medium text-indigo-600 hover:underline">
            <Download className="size-3.5" /> Download
          </a>
        ) : null,
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={() => setModalOpen(true)}>
          <Upload className="size-4" /> Upload Document
        </Button>
      </div>
      <Card>
        {documents.length === 0 ? (
          <EmptyState icon={<FolderOpen className="size-5" />} title="No documents yet" message="Upload documents your consultant has requested." />
        ) : (
          <Table columns={columns} rows={documents} keyFor={(d) => d.id} />
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
          <Select label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as DocumentCategory })}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <input type="file" onChange={onFile} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          {form.name && <p className="text-xs text-slate-500">Selected: {form.name}</p>}
        </div>
      </Modal>
    </div>
  );
}
