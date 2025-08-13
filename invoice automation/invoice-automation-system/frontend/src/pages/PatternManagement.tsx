import React from 'react';
import PatternList from '../components/patterns/PatternList';

/**
 * PatternManagement page component following React UI Cursor Rules
 * - Main page for pattern management functionality
 * - Protected route requiring ADMIN role
 * - Uses the PatternList component for main functionality
 */
const PatternManagement: React.FC = () => {
  return (
    <div className="space-y-6">
      <PatternList />
    </div>
  );
};

export default PatternManagement;
