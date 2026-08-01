import { Landmark } from 'lucide-react';

export function BrandMark({ logoUrl, size = 36 }: { logoUrl?: string; size?: number }) {
  if (logoUrl) {
    return (
      <img
        src={logoUrl}
        alt="Firm logo"
        className="shrink-0 rounded-lg object-cover"
        style={{ width: size, height: size }}
      />
    );
  }
  return (
    <div className="flex shrink-0 items-center justify-center rounded-lg bg-indigo-600 text-white" style={{ width: size, height: size }}>
      <Landmark className="size-1/2" />
    </div>
  );
}
