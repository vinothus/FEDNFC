import React, { useState, useEffect, useCallback } from 'react';
import {
  Cog6ToothIcon,
  ShieldCheckIcon,
  EnvelopeIcon,
  CloudIcon,
  DocumentTextIcon,
  BellIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline';
import { SystemSetting, settingsApi } from '../../services/settingsApi';
import { cn } from '../../utils/cn';
import { Button, Alert, Loading } from '../ui';

export interface DynamicSettingsLayoutProps {
  className?: string;
}

/**
 * DynamicSettingsLayout component for managing application settings
 * - Loads real settings from backend
 * - Grouped by category with filtering
 * - Real-time updates and validation
 * - Shows restart requirements
 */
const DynamicSettingsLayout: React.FC<DynamicSettingsLayoutProps> = ({ className }) => {
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [groupedSettings, setGroupedSettings] = useState<Record<string, SystemSetting[]>>({});
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [unsavedChanges, setUnsavedChanges] = useState<Record<string, string>>({});
  const [searchTerm, setSearchTerm] = useState('');
  const [totalSettings, setTotalSettings] = useState(0);
  const [modifiableSettings, setModifiableSettings] = useState(0);
  const [restartRequired, setRestartRequired] = useState(false);

  // Load settings from backend
  const loadSettings = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('⚙️ DynamicSettings: Loading settings from API...');
      const response = await settingsApi.getAllSettings();
      console.log('✅ DynamicSettings: Settings loaded successfully:', response);
      
      setSettings(response.settings);
      setCategories(['all', ...response.categories]);
      setGroupedSettings(response.groupedSettings);
      setTotalSettings(response.totalSettings);
      setModifiableSettings(response.modifiableSettings);
      setRestartRequired(response.restartRequired);
      
    } catch (err: any) {
      console.error('❌ DynamicSettings: Failed to load settings:', err);
      setError(err.message || 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  }, []); // Empty dependency array since this function doesn't depend on any props or state

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  // Filter settings based on category and search
  const filteredSettings = selectedCategory === 'all' 
    ? (settings || []).filter(setting => 
        setting?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        setting?.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        setting?.key?.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : (groupedSettings[selectedCategory] || []).filter(setting =>
        setting?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        setting?.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        setting?.key?.toLowerCase().includes(searchTerm.toLowerCase())
      );

  // Handle setting value change
  const handleSettingChange = (key: string, value: string) => {
    setUnsavedChanges(prev => ({
      ...prev,
      [key]: value
    }));
  };

  // Handle individual setting save
  const handleSaveSetting = async (key: string) => {
    if (!unsavedChanges[key]) return;

    try {
      setSaving(true);
      await settingsApi.updateSetting(key, unsavedChanges[key]);
      
      // Update local state
      setSettings(prev => prev.map(setting => 
        setting.key === key 
          ? { ...setting, currentValue: unsavedChanges[key] }
          : setting
      ));
      
      // Remove from unsaved changes
      setUnsavedChanges(prev => {
        const updated = { ...prev };
        delete updated[key];
        return updated;
      });
      
      setSuccess(`Setting "${key}" updated successfully`);
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err: any) {
      setError(err.message || 'Failed to update setting');
    } finally {
      setSaving(false);
    }
  };

  // Handle bulk save
  const handleSaveAll = async () => {
    if (Object.keys(unsavedChanges).length === 0) return;

    try {
      setSaving(true);
      const result = await settingsApi.bulkUpdateSettings(unsavedChanges);
      
      if (result.hasErrors) {
        setError(`Some settings failed to update. ${result.totalErrors} errors occurred.`);
      } else {
        setSuccess(`Successfully updated ${result.totalUpdated} settings`);
        setTimeout(() => setSuccess(null), 3000);
        
        // Update local state
        setSettings(prev => prev.map(setting => 
          unsavedChanges[setting.key] 
            ? { ...setting, currentValue: unsavedChanges[setting.key] }
            : setting
        ));
        
        setUnsavedChanges({});
        
        if (result.restartRequired) {
          setRestartRequired(true);
        }
      }
      
    } catch (err: any) {
      setError(err.message || 'Failed to update settings');
    } finally {
      setSaving(false);
    }
  };

  // Handle reset setting
  const handleResetSetting = async (key: string) => {
    if (!window.confirm(`Reset "${key}" to default value?`)) return;

    try {
      setSaving(true);
      await settingsApi.resetSetting(key);
      
      // Update local state
      setSettings(prev => prev.map(setting => 
        setting.key === key 
          ? { ...setting, currentValue: setting.defaultValue }
          : setting
      ));
      
      // Remove from unsaved changes
      setUnsavedChanges(prev => {
        const updated = { ...prev };
        delete updated[key];
        return updated;
      });
      
      setSuccess(`Setting "${key}" reset to default`);
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err: any) {
      setError(err.message || 'Failed to reset setting');
    } finally {
      setSaving(false);
    }
  };

  // Get category icon
  const getCategoryIcon = (category: string) => {
    const icons: Record<string, any> = {
      'File Storage': CloudIcon,
      'OCR': DocumentTextIcon,
      'Email': EnvelopeIcon,
      'Security': ShieldCheckIcon,
      'Logging': BellIcon,
      'Scheduler': Cog6ToothIcon,
      'Management': Cog6ToothIcon,
    };
    return icons[category] || Cog6ToothIcon;
  };

  // Render setting input based on type
  const renderSettingInput = (setting: SystemSetting) => {
    const currentValue = unsavedChanges[setting.key] ?? setting.currentValue;
    // const hasUnsavedChanges = unsavedChanges[setting.key] !== undefined;

    switch (setting.type) {
      case 'boolean':
        return (
          <div className="flex items-center space-x-3">
            <input
              type="checkbox"
              id={setting.key}
              checked={currentValue === 'true'}
              onChange={(e) => handleSettingChange(setting.key, e.target.checked ? 'true' : 'false')}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor={setting.key} className="text-sm text-gray-900">
              {setting.description}
            </label>
          </div>
        );

      case 'enum':
        return (
          <select
            value={currentValue}
            onChange={(e) => handleSettingChange(setting.key, e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          >
            {setting.allowedValues?.map(option => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        );

      case 'number':
        return (
          <input
            type="number"
            value={currentValue}
            onChange={(e) => handleSettingChange(setting.key, e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        );

      default: // string
        return (
          <input
            type="text"
            value={currentValue}
            onChange={(e) => handleSettingChange(setting.key, e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
        );
    }
  };

  if (loading) {
    return <Loading size="lg" className="py-12" />;
  }

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <div className="bg-white shadow-sm rounded-lg p-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dynamic Settings</h1>
            <p className="text-gray-600 mt-1">
              Configure application settings that can be modified at runtime
            </p>
          </div>
          <div className="flex items-center space-x-3">
            <Button
              variant="secondary"
              onClick={loadSettings}
              disabled={loading}
            >
              <ArrowPathIcon className="h-4 w-4 mr-2" />
              Refresh
            </Button>
            {Object.keys(unsavedChanges).length > 0 && (
              <Button
                variant="primary"
                onClick={handleSaveAll}
                loading={saving}
              >
                Save All Changes ({Object.keys(unsavedChanges).length})
              </Button>
            )}
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-4 mt-6">
          <div className="bg-blue-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-blue-600">{totalSettings}</div>
            <div className="text-sm text-blue-600">Total Settings</div>
          </div>
          <div className="bg-green-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-green-600">{modifiableSettings}</div>
            <div className="text-sm text-green-600">Modifiable</div>
          </div>
          <div className="bg-yellow-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-yellow-600">{Object.keys(unsavedChanges).length}</div>
            <div className="text-sm text-yellow-600">Unsaved Changes</div>
          </div>
          <div className="bg-red-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-red-600">{restartRequired ? 'Yes' : 'No'}</div>
            <div className="text-sm text-red-600">Restart Required</div>
          </div>
        </div>
      </div>

      {/* Alerts */}
      {error && (
        <Alert variant="error" dismissible onDismiss={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert variant="success" dismissible onDismiss={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {restartRequired && (
        <Alert variant="warning">
          <ExclamationTriangleIcon className="h-5 w-5" />
          <span>Some settings require a server restart to take effect.</span>
        </Alert>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white shadow-sm rounded-lg p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Categories</h3>
            
            {/* Search */}
            <div className="mb-4">
              <input
                type="text"
                placeholder="Search settings..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
              />
            </div>

            {/* Category List */}
            <nav className="space-y-1">
              {categories.map((category) => {
                const Icon = category === 'all' ? Cog6ToothIcon : getCategoryIcon(category);
                const isActive = selectedCategory === category;
                const count = category === 'all' 
                  ? totalSettings 
                  : (groupedSettings[category] || []).length;

                return (
                  <button
                    key={category}
                    onClick={() => setSelectedCategory(category)}
                    className={cn(
                      'w-full flex items-center justify-between px-3 py-2 text-sm font-medium rounded-md transition-colors',
                      isActive
                        ? 'bg-blue-100 text-blue-700'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    )}
                  >
                    <div className="flex items-center">
                      <Icon className="h-4 w-4 mr-3" />
                      <span>{category === 'all' ? 'All Settings' : category}</span>
                    </div>
                    <span className={cn(
                      'px-2 py-1 text-xs rounded-full',
                      isActive ? 'bg-blue-200 text-blue-800' : 'bg-gray-200 text-gray-600'
                    )}>
                      {count}
                    </span>
                  </button>
                );
              })}
            </nav>
          </div>
        </div>

        {/* Main Content */}
        <div className="lg:col-span-3">
          <div className="bg-white shadow-sm rounded-lg p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-medium text-gray-900">
                {selectedCategory === 'all' ? 'All Settings' : `${selectedCategory} Settings`}
              </h3>
              <span className="text-sm text-gray-500">
                {filteredSettings.length} settings
              </span>
            </div>

            {/* Settings List */}
            <div className="space-y-6">
              {filteredSettings.map((setting) => {
                const hasUnsavedChanges = unsavedChanges[setting.key] !== undefined;
                
                return (
                  <div key={setting.key} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center space-x-2 mb-2">
                          <h4 className="text-sm font-medium text-gray-900">
                            {setting.name}
                          </h4>
                          {setting.requiresRestart && (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                              Restart Required
                            </span>
                          )}
                          {hasUnsavedChanges && (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                              Modified
                            </span>
                          )}
                        </div>
                        
                        <p className="text-sm text-gray-600 mb-3">
                          {setting.description}
                        </p>
                        
                        <div className="mb-3">
                          {renderSettingInput(setting)}
                        </div>
                        
                        <div className="flex items-center justify-between text-xs text-gray-500">
                          <span>Key: <code className="bg-gray-100 px-1 rounded">{setting.key}</code></span>
                          <span>Default: <code className="bg-gray-100 px-1 rounded">{setting.defaultValue}</code></span>
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-2 ml-4">
                        {hasUnsavedChanges && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleSaveSetting(setting.key)}
                            loading={saving}
                          >
                            <CheckCircleIcon className="h-4 w-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleResetSetting(setting.key)}
                          disabled={saving}
                        >
                          Reset
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
              
              {filteredSettings.length === 0 && (
                <div className="text-center py-12">
                  <Cog6ToothIcon className="mx-auto h-12 w-12 text-gray-400" />
                  <h3 className="mt-4 text-lg font-medium text-gray-900">No settings found</h3>
                  <p className="mt-2 text-sm text-gray-600">
                    {searchTerm 
                      ? 'Try adjusting your search terms.' 
                      : 'No settings available in this category.'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DynamicSettingsLayout;