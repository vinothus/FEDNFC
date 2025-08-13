import React from 'react';
import UserList from '../components/users/UserList';

/**
 * UserManagement page component following React UI Cursor Rules
 * - Main page for user management functionality
 * - Protected route requiring ADMIN role
 * - Uses the UserList component for main functionality
 */
const UserManagement: React.FC = () => {
  return (
    <div className="space-y-6">
      <UserList />
    </div>
  );
};

export default UserManagement;
