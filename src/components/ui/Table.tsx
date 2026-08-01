import type { ReactNode } from 'react';

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

interface TableProps<T> {
  columns: Column<T>[];
  rows: T[];
  keyFor: (row: T) => string;
  emptyMessage?: string;
}

export function Table<T>({ columns, rows, keyFor, emptyMessage = 'No records found.' }: TableProps<T>) {
  if (rows.length === 0) {
    return <div className="p-10 text-center text-sm text-slate-400">{emptyMessage}</div>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-max text-left text-sm">
        <thead>
          <tr className="border-b border-slate-100 text-xs text-slate-500">
            {columns.map((c) => (
              <th key={c.header} className="whitespace-nowrap px-4 py-3 font-medium">
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={keyFor(row)} className="border-b border-slate-50 last:border-0 hover:bg-slate-50">
              {columns.map((c) => (
                <td key={c.header} className={`whitespace-nowrap px-4 py-3 text-slate-700 ${c.className ?? ''}`}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
