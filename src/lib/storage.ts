import { useSyncExternalStore } from 'react';

const PREFIX = 'sekaass:';

function emit(key: string) {
  window.dispatchEvent(new CustomEvent(`storage-change:${key}`));
}

export function readRaw<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    if (raw === null) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function writeRaw<T>(key: string, value: T) {
  localStorage.setItem(PREFIX + key, JSON.stringify(value));
  emit(key);
}

// useSyncExternalStore requires getSnapshot to return a stable reference
// when the underlying data hasn't changed, so parsed values are cached per
// key and only reparsed when the raw JSON string actually changes.
const snapshotCache = new Map<string, { raw: string | null; value: unknown }>();

function getCachedSnapshot<T>(key: string, fallback: T): T {
  const raw = localStorage.getItem(PREFIX + key);
  const cached = snapshotCache.get(key);
  if (cached && cached.raw === raw) return cached.value as T;
  const value = raw === null ? fallback : (JSON.parse(raw) as T);
  snapshotCache.set(key, { raw, value });
  return value;
}

export function useRaw<T>(key: string, fallback: T): T {
  return useSyncExternalStore(
    (onChange) => {
      const handler = () => onChange();
      window.addEventListener(`storage-change:${key}`, handler);
      window.addEventListener('storage', handler);
      return () => {
        window.removeEventListener(`storage-change:${key}`, handler);
        window.removeEventListener('storage', handler);
      };
    },
    () => getCachedSnapshot(key, fallback),
    () => fallback,
  );
}

export interface Collection<T extends { id: string }> {
  key: string;
  getAll(): T[];
  getById(id: string): T | undefined;
  add(item: T): T;
  update(id: string, patch: Partial<T>): void;
  remove(id: string): void;
  set(items: T[]): void;
  useAll(): T[];
}

export function createCollection<T extends { id: string }>(key: string): Collection<T> {
  return {
    key,
    getAll: () => readRaw<T[]>(key, []),
    getById: (id) => readRaw<T[]>(key, []).find((i) => i.id === id),
    add: (item) => {
      const all = readRaw<T[]>(key, []);
      const next = [item, ...all];
      writeRaw(key, next);
      return item;
    },
    update: (id, patch) => {
      const all = readRaw<T[]>(key, []);
      writeRaw(
        key,
        all.map((i) => (i.id === id ? { ...i, ...patch } : i)),
      );
    },
    remove: (id) => {
      const all = readRaw<T[]>(key, []);
      writeRaw(
        key,
        all.filter((i) => i.id !== id),
      );
    },
    set: (items) => writeRaw(key, items),
    useAll: () => useRaw<T[]>(key, []),
  };
}

export function genId(prefix = 'id'): string {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}
