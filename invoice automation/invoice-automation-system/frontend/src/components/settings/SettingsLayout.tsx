import React, { useState, useEffect } from 'react';
import {
  Cog6ToothIcon,
  ShieldCheckIcon,
  EnvelopeIcon,
  CloudIcon,
  DocumentTextIcon,
  BellIcon,
} from '@heroicons/react/24/outline';
import { SystemSetting, SettingsCategory, settingsApi } from '../../services/settingsApi';
import { cn } from '../../utils/cn';
import { Button, Alert, Loading } from '../ui';

export interface SettingsLayoutProps {
  className?: string;
}

/**
 * SettingsLayout component following React UI Cursor Rules
 * - Tabbed settings interface with categories
 * - Form-based setting management
 * - Validation and error handling
 * - Responsive design with sidebar navigation
 * - Accessible with proper ARIA labels
 */
const SettingsLayout: React.FC<SettingsLayoutProps> = ({ className }) => {
  const [selectedCategory, setSelectedCategory] = useState('general');
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [categories, setCategories] = useState<SettingsCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [modifiedSettings, setModifiedSettings] = useState<Record<string, string>>({});

  // Mock data for demonstration (deprecated - using DynamicSettingsLayout instead)
  const mockCategories: any[] = [
    {
      id: 'general',
      name: 'General',
      description: 'Basic application settings',
      icon: 'cog',
      order: 1,
      settingsCount: 4,
    },
    {
      id: 'security',
      name: 'Security',
      description: 'Authentication and authorization settings',
      icon: 'shield',
      order: 2,
      settingsCount: 3,
    },
    {
      id: 'ocr',
      name: 'OCR Processing',
      description: 'Optical Character Recognition configuration',
      icon: 'document',
      order: 3,
      settingsCount: 5,
    },
    {
      id: 'email',
      name: 'Email',
      description: 'Email monitoring and notifications',
      icon: 'envelope',
      order: 4,
      settingsCount: 3,
    },
    {
      id: 'storage',
      name: 'Storage',
      description: 'File storage and backup settings',
      icon: 'cloud',
      order: 5,
      settingsCount: 2,
    },
    {
      id: 'notifications',
      name: 'Notifications',
      description: 'Alert and notification preferences',
      icon: 'bell',
      order: 6,
      settingsCount: 3,
    },
  ];

  const mockSettings: any[] = [
    // General Settings
    {
      key: 'app.name',
      name: 'Application Name',
      description: 'The display name of the application',
      currentValue: 'Invoice Automation System',
      defaultValue: 'Invoice Automation System',
      category: 'general',
      type: 'string',
      required: true,
      readOnly: false,
      restartRequired: false,
    },
    {
      key: 'app.version',
      name: 'Application Version',
      description: 'Current version of the application',
      currentValue: '1.0.0',
      defaultValue: '1.0.0',
      category: 'general',
      type: 'string',
      required: true,
      readOnly: true,
      restartRequired: false,
    },
    {
      key: 'app.maintenance_mode',
      name: 'Maintenance Mode',
      description: 'Enable maintenance mode to prevent new uploads',
      currentValue: 'false',
      defaultValue: 'false',
      category: 'general',
      type: 'boolean',
      required: false,
      readOnly: false,
      restartRequired: false,
    },
    {
      key: 'app.max_file_size',
      name: 'Maximum File Size (MB)',
      description: 'Maximum allowed file size for uploads',
      currentValue: '50',
      defaultValue: '50',
      category: 'general',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: true,
      minValue: 1,
      maxValue: 100,
    },
    
    // Security Settings
    {
      key: 'security.session_timeout',
      name: 'Session Timeout (minutes)',
      description: 'User session timeout in minutes',
      currentValue: '1440',
      defaultValue: '1440',
      category: 'security',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: false,
      minValue: 30,
      maxValue: 10080,
    },
    {
      key: 'security.password_min_length',
      name: 'Minimum Password Length',
      description: 'Minimum required password length',
      currentValue: '8',
      defaultValue: '8',
      category: 'security',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: false,
      minValue: 6,
      maxValue: 50,
    },
    {
      key: 'security.max_login_attempts',
      name: 'Maximum Login Attempts',
      description: 'Maximum failed login attempts before account lockout',
      currentValue: '5',
      defaultValue: '5',
      category: 'security',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: false,
      minValue: 3,
      maxValue: 10,
    },
    
    // OCR Settings
    {
      key: 'ocr.tesseract.language',
      name: 'OCR Language',
      description: 'Primary language for OCR processing',
      currentValue: 'eng',
      defaultValue: 'eng',
      category: 'ocr',
      type: 'string',
      required: true,
      readOnly: false,
      restartRequired: true,
      options: ['eng', 'fra', 'deu', 'spa', 'ita'],
    },
    {
      key: 'ocr.confidence_threshold',
      name: 'Confidence Threshold',
      description: 'Minimum confidence level for OCR results (0.0-1.0)',
      currentValue: '0.7',
      defaultValue: '0.7',
      category: 'ocr',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: false,
      minValue: 0.0,
      maxValue: 1.0,
    },
    
    // Email Settings
    {
      key: 'email.poll_interval',
      name: 'Email Poll Interval (seconds)',
      description: 'How often to check for new emails',
      currentValue: '120',
      defaultValue: '120',
      category: 'email',
      type: 'number',
      required: true,
      readOnly: false,
      restartRequired: false,
      minValue: 60,
      maxValue: 3600,
    },
    {
      key: 'email.enabled',
      name: 'Email Monitoring Enabled',
      description: 'Enable automatic email monitoring for invoices',
      currentValue: 'true',
      defaultValue: 'true',
      category: 'email',
      type: 'boolean',
      required: false,
      readOnly: false,
      restartRequired: false,
    },
  ];

  useEffect(() => {
    const loadSettings = async () => {
      try {
        setLoading(true);
        
        // Load settings and categories from API
        // Using mock data since this component is deprecated
        setSettings(mockSettings);
        setCategories(mockCategories);
      } catch (err: any) {
        setError(err.message || 'Failed to load settings');
        // Fallback to mock data if API fails
        setCategories(mockCategories);
        setSettings(mockSettings);
      } finally {
        setLoading(false);
      }
    };

    loadSettings();
  }, []);

  const getCategoryIcon = (iconName: string) => {
    const icons = {
      cog: <Cog6ToothIcon className="h-5 w-5" />,
      shield: <ShieldCheckIcon className="h-5 w-5" />,
      document: <DocumentTextIcon className="h-5 w-5" />,
      envelope: <EnvelopeIcon className="h-5 w-5" />,
      cloud: <CloudIcon className="h-5 w-5" />,
      bell: <BellIcon className="h-5 w-5" />,
    };
    return icons[iconName as keyof typeof icons] || <Cog6ToothIcon className="h-5 w-5" />;
  };

  const filteredSettings = settings.filter(setting => setting.category === selectedCategory);

  const handleSettingChange = (key: string, value: string) => {
    setModifiedSettings(prev => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleSave = async () => {
    if (Object.keys(modifiedSettings).length === 0) {
      return;
    }

    try {
      setSaving(true);
      setError(null);
      
      // Prepare settings for bulk update
      // Deprecated: This component is no longer used
      // Just update the modified settings locally for demo purposes
      setSettings(prev => prev.map(setting => 
        modifiedSettings[setting.key] 
          ? { ...setting, currentValue: modifiedSettings[setting.key] }
          : setting
      ));
      
      setModifiedSettings({});
      setSuccess('Settings saved successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    setModifiedSettings({});
  };

  const renderSettingInput = (setting: SystemSetting) => {
    const currentValue = modifiedSettings[setting.key] ?? setting.currentValue;
    
    if ((setting as any).readOnly) {
      return (
        <div className="mt-1 block w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-md text-gray-500">
          {currentValue}
        </div>
      );
    }

    switch (setting.type) {
      case 'boolean':
        return (
          <div className="mt-1">
            <label className="inline-flex items-center">
              <input
                type="checkbox"
                checked={currentValue === 'true'}
                onChange={(e) => handleSettingChange(setting.key, e.target.checked ? 'true' : 'false')}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <span className="ml-2 text-sm text-gray-600">
                {setting.description}
              </span>
            </label>
          </div>
        );
      
      case 'number':
        return (
          <input
            type="number"
            value={currentValue}
            onChange={(e) => handleSettingChange(setting.key, e.target.value)}
            min={(setting as any).minValue}
            max={(setting as any).maxValue}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        );
      
      default:
        if ((setting as any).options) {
          return (
            <select
              value={currentValue}
              onChange={(e) => handleSettingChange(setting.key, e.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              {(setting as any).options.map((option: string) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          );
        }
        
        return (
          <input
            type={(setting as any).type === 'password' ? 'password' : 'text'}
            value={currentValue}
            onChange={(e) => handleSettingChange(setting.key, e.target.value)}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        );
    }
  };

  const hasChanges = Object.keys(modifiedSettings).length > 0;
  const restartRequired = filteredSettings.some(setting => 
    (setting as any).restartRequired && modifiedSettings[setting.key] !== undefined
  );

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Loading size="lg" text="Loading settings..." />
      </div>
    );
  }

  return (
    <div className={cn('flex flex-col lg:flex-row gap-6', className)}>
      {/* Sidebar Navigation */}
      <div className="w-full lg:w-64 flex-shrink-0">
        <nav className="space-y-1">
          {categories.map((category) => (
            <button
              key={category.id}
              onClick={() => setSelectedCategory(category.id)}
              className={cn(
                'w-full flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors',
                selectedCategory === category.id
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-700 hover:bg-gray-100'
              )}
            >
              <span className="mr-3">
                {getCategoryIcon(category.icon || 'cog')}
              </span>
              <span className="flex-1 text-left">{category.name}</span>
              <span className="text-xs text-gray-500">
                {category.settingsCount}
              </span>
            </button>
          ))}
        </nav>
      </div>

      {/* Main Content */}
      <div className="flex-1 min-w-0">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
          <p className="mt-1 text-sm text-gray-600">
            Configure system settings and preferences
          </p>
        </div>

        {/* Alerts */}
        {error && (
          <Alert variant="error" dismissible onDismiss={() => setError(null)} className="mb-6">
            {error}
          </Alert>
        )}

        {success && (
          <Alert variant="success" dismissible onDismiss={() => setSuccess(null)} className="mb-6">
            {success}
          </Alert>
        )}

        {restartRequired && (
          <Alert variant="warning" className="mb-6">
            Some settings require an application restart to take effect.
          </Alert>
        )}

        {/* Settings Form */}
        <div className="bg-white shadow-sm rounded-lg border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">
              {categories.find(cat => cat.id === selectedCategory)?.name} Settings
            </h2>
            <p className="text-sm text-gray-600">
              {categories.find(cat => cat.id === selectedCategory)?.description}
            </p>
          </div>

          <div className="p-6 space-y-6">
            {filteredSettings.map((setting) => (
              <div key={setting.key}>
                <label className="block text-sm font-medium text-gray-700">
                  {setting.name}
                  {(setting as any).required && <span className="text-red-500 ml-1">*</span>}
                </label>
                
                {setting.type !== 'boolean' && setting.description && (
                  <p className="text-sm text-gray-500 mt-1">{setting.description}</p>
                )}
                
                {renderSettingInput(setting)}
                
                {(setting as any).restartRequired && modifiedSettings[setting.key] !== undefined && (
                  <p className="text-xs text-yellow-600 mt-1">
                    ⚠️ Restart required for this change to take effect
                  </p>
                )}
              </div>
            ))}
          </div>

          {/* Actions */}
          {hasChanges && (
            <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex items-center justify-between">
              <p className="text-sm text-gray-600">
                You have unsaved changes
              </p>
              <div className="flex space-x-3">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={handleReset}
                  disabled={saving}
                >
                  Reset
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleSave}
                  loading={saving}
                  disabled={saving}
                >
                  Save Changes
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SettingsLayout;
