import { useMemo, useState } from 'react';
import { History } from 'lucide-react';
import { Card } from '../../components/ui/Card';
import { Table, type Column } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { auditStore } from '../../lib/entities';
import { formatDateTime } from '../../lib/format';
import type { AuditLogEntry, Role } from '../../types';

export function AdminAuditTrail() {
  const entries = auditStore.useAll();
  const [roleFilter, setRoleFilter] = useState<'All' | Role>('All');

  const filtered = useMemo(() => (roleFilter === 'All' ? entries : entries.filter((e) => e.actorRole === roleFilter)), [entries, roleFilter]);
  const sorted = useMemo(() => [...filtered].sort((a, b) => (a.timestamp < b.timestamp ? 1 : -1)), [filtered]);

  const columns: Column<AuditLogEntry>[] = [
    { header: 'Timestamp', render: (e) => formatDateTime(e.timestamp) },
    { header: 'Actor', render: (e) => e.actor },
    { header: 'Role', render: (e) => <Badge tone={e.actorRole === 'admin' ? 'purple' : 'blue'}>{e.actorRole}</Badge> },
    { header: 'Action', render: (e) => e.action },
    { header: 'Entity', render: (e) => e.entity },
    { header: 'Details', render: (e) => <span className="text-slate-500">{e.details}</span> },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">{sorted.length} audit log entries</p>
        <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value as 'All' | Role)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="All">All actors</option>
          <option value="admin">Admin</option>
          <option value="client">Client</option>
        </select>
      </div>
      <Card>
        {sorted.length === 0 ? (
          <EmptyState icon={<History className="size-5" />} title="No audit entries" message="Actions across the firm will be logged here." />
        ) : (
          <Table columns={columns} rows={sorted} keyFor={(e) => e.id} />
        )}
      </Card>
    </div>
  );
}
