// Settings management related types

export interface SystemSetting {
  key: string;
  name: string;
  description?: string;
  value: string;
  defaultValue: string;
  category: string;
  type: 'string' | 'number' | 'boolean' | 'email' | 'url' | 'password';
  required: boolean;
  readOnly: boolean;
  restartRequired: boolean;
  validationPattern?: string;
  options?: string[]; // For select/enum types
  minValue?: number;
  maxValue?: number;
  updatedAt?: string;
  updatedBy?: string;
}

export interface SettingsCategory {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  order: number;
  settingsCount: number;
}

export interface SettingUpdateRequest {
  value: string;
}

export interface SettingsStats {
  totalSettings: number;
  modifiedSettings: number;
  categoriesCount: number;
  restartRequiredCount: number;
}
