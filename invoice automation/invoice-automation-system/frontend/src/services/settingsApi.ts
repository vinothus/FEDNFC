import { AxiosResponse } from 'axios';
import { api } from './api';

// Types for Settings Management
export interface SystemSetting {
  key: string;
  name: string;
  description: string;
  currentValue: string;
  defaultValue: string;
  category: string;
  type: 'string' | 'number' | 'boolean' | 'enum';
  requiresRestart: boolean;
  allowedValues?: string[];
  lastModified?: number;
  modifiedBy?: string;
}

export interface SettingsCategory {
  id: string;
  name: string;
  description: string;
  icon: string;
  order: number;
  settingsCount: number;
}

export interface SettingUpdateRequest {
  key: string;
  value: string;
}

export interface BulkSettingsUpdateRequest {
  settings: SettingUpdateRequest[];
}

export interface SettingsBackupResponse {
  backupId: string;
  filename: string;
  createdAt: string;
  size: number;
}

export interface SettingsStatsResponse {
  totalSettings: number;
  categoriesCount: number;
  modifiedSettings: number;
  requiresRestart: number;
}

/**
 * Settings Management API Service
 * Provides CRUD operations for system settings (Admin only)
 */
export const settingsApi = {
  /**
   * Get all settings grouped by category
   */
  getAllSettings: async (): Promise<{
    settings: SystemSetting[];
    categories: string[];
    groupedSettings: Record<string, SystemSetting[]>;
    totalSettings: number;
    modifiableSettings: number;
    restartRequired: boolean;
  }> => {
    const response: AxiosResponse = await api.get('/admin/dynamic-settings');
    return response.data;
  },

  /**
   * Get all settings categories
   */
  getCategories: async (): Promise<Array<{name: string; count: number}>> => {
    const response: AxiosResponse = await api.get('/admin/dynamic-settings/categories');
    return response.data;
  },

  /**
   * Get specific setting
   */
  getSettingByKey: async (key: string): Promise<SystemSetting> => {
    const response: AxiosResponse<SystemSetting> = await api.get(`/admin/dynamic-settings/${key}`);
    return response.data;
  },

  /**
   * Update a single setting
   */
  updateSetting: async (key: string, value: string): Promise<any> => {
    const response: AxiosResponse = await api.put(`/admin/dynamic-settings/${key}`, { value });
    return response.data;
  },

  /**
   * Bulk update multiple settings
   */
  bulkUpdateSettings: async (settings: Record<string, string>): Promise<any> => {
    const response: AxiosResponse = await api.post('/admin/dynamic-settings/bulk-update', settings);
    return response.data;
  },

  /**
   * Reset setting to default value
   */
  resetSetting: async (key: string): Promise<any> => {
    const response: AxiosResponse = await api.post(`/admin/dynamic-settings/reset/${key}`);
    return response.data;
  },
};

export default settingsApi;
