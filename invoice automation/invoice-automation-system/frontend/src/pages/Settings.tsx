import React from 'react';
import DynamicSettingsLayout from '../components/settings/DynamicSettingsLayout';

/**
 * Settings page component following React UI Cursor Rules
 * - Main page for dynamic system settings management
 * - Protected route requiring ADMIN role
 * - Uses the DynamicSettingsLayout component for real-time settings
 */
const Settings: React.FC = () => {
  return (
    <div className="space-y-6">
      <DynamicSettingsLayout />
    </div>
  );
};

export default Settings;
