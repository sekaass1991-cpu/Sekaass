import { readRaw, writeRaw, useRaw } from './storage';
import type { FirmSettings } from '../types';

const KEY = 'settings';

const DEFAULT_SETTINGS: FirmSettings = {
  firmName: 'TaxTitan Consultancy',
  logoUrl: '/brand/logo-icon.png',
  autoBackupFrequency: 'Weekly',
  autoReminders: true,
};

export function getSettings(): FirmSettings {
  return readRaw<FirmSettings>(KEY, DEFAULT_SETTINGS);
}

export function updateSettings(patch: Partial<FirmSettings>) {
  writeRaw(KEY, { ...getSettings(), ...patch });
}

export function useSettings(): FirmSettings {
  return useRaw<FirmSettings>(KEY, DEFAULT_SETTINGS);
}
