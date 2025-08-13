import React, { useState, useEffect, useCallback } from 'react';
import { format } from 'date-fns';
import { 
  PlusIcon, 
  MagnifyingGlassIcon,
  UserIcon,
  PencilIcon,
  TrashIcon,
  LockClosedIcon,
  LockOpenIcon 
} from '@heroicons/react/24/outline';
import { UserAccount, userApi, UserSearchParams } from '../../services/userApi';
import { Button, Input, Alert, Loading } from '../ui';
import { cn } from '../../utils/cn';

export interface UserListProps {
  className?: string;
}

/**
 * UserList component following React UI Cursor Rules
 * - Displays users in a responsive table
 * - Search and filter functionality
 * - Role-based actions
 * - Accessible table structure
 * - Loading and error states
 */
const UserList: React.FC<UserListProps> = ({ className }) => {
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const params: UserSearchParams = {
        page: currentPage,
        size: 20,
        sortBy: 'createdAt',
        sortDir: 'desc',
      };
      
      if (searchTerm.trim()) {
        params.search = searchTerm.trim();
      }
      
      const response = await userApi.getAllUsers(params);
      
      setUsers(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
      
    } catch (err: any) {
      setError(err.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [currentPage, searchTerm]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleCreateUser = () => {
    alert('Create user functionality - Feature coming soon!');
  };

  const handleEditUser = (user: UserAccount) => {
    alert(`Edit user "${user.username}" - Feature coming soon!`);
  };

  const handleDeleteUser = async (user: UserAccount) => {
    if (!window.confirm(`Are you sure you want to deactivate user "${user.username}"? This will prevent them from logging in.`)) {
      return;
    }
    
    try {
      await userApi.deleteUser(user.id);
      await fetchUsers(); // Refresh the list
    } catch (err: any) {
      setError(err.message || 'Failed to deactivate user');
    }
  };

  const handleToggleLock = async (user: UserAccount) => {
    try {
      await userApi.toggleUserLock(user.id);
      await fetchUsers(); // Refresh the list
    } catch (err: any) {
      setError(err.message || 'Failed to toggle user lock status');
    }
  };

  const getRoleBadge = (role: string) => {
    const colors = {
      'ADMIN': 'bg-red-100 text-red-800',
      'APPROVER': 'bg-blue-100 text-blue-800',
      'USER': 'bg-green-100 text-green-800',
    };
    
    return (
      <span className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
        colors[role as keyof typeof colors] || 'bg-gray-100 text-gray-800'
      )}>
        {role}
      </span>
    );
  };

  const getStatusBadge = (user: UserAccount) => {
    if (user.isLocked) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
          Locked
        </span>
      );
    }
    
    return (
      <span className={cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
        user.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
      )}>
        {user.isActive ? 'Active' : 'Inactive'}
      </span>
    );
  };

  return (
    <div className={className}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
          <p className="mt-1 text-sm text-gray-600">
            Manage user accounts and permissions
          </p>
        </div>
        <div className="mt-4 sm:mt-0">
          <Button
            variant="primary"
            size="sm"
            onClick={handleCreateUser}
            className="w-full sm:w-auto"
          >
            <PlusIcon className="h-4 w-4 mr-2" />
            Create User
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="mb-6">
        <Input
          placeholder="Search users..."
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setCurrentPage(0); // Reset to first page when searching
          }}
          startIcon={<MagnifyingGlassIcon />}
          className="max-w-md"
        />
      </div>

      {/* Error State */}
      {error && (
        <Alert variant="error" dismissible onDismiss={() => setError(null)} className="mb-6">
          {error}
        </Alert>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex justify-center py-12">
          <Loading size="lg" text="Loading users..." />
        </div>
      )}

      {/* Users Table */}
      {!loading && (
        <div className="bg-white shadow-sm rounded-lg border border-gray-200 overflow-hidden">
          {users.length === 0 ? (
            <div className="text-center py-12">
              <UserIcon className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-4 text-lg font-medium text-gray-900">
                No users found
              </h3>
              <p className="mt-2 text-sm text-gray-600">
                {searchTerm ? 'Try adjusting your search term.' : 'Get started by creating a new user.'}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th 
                      scope="col" 
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      User
                    </th>
                    <th 
                      scope="col" 
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Role
                    </th>
                    <th 
                      scope="col" 
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Status
                    </th>
                    <th 
                      scope="col" 
                      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Last Login
                    </th>
                    <th 
                      scope="col" 
                      className="relative px-6 py-3"
                    >
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {users.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center">
                            <UserIcon className="h-6 w-6 text-gray-600" />
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">
                              {user.firstName} {user.lastName}
                            </div>
                            <div className="text-sm text-gray-500">
                              {user.email}
                            </div>
                            <div className="text-sm text-gray-500">
                              @{user.username}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getRoleBadge(user.role)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(user)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {user.lastLogin ? (
                          <div>
                            <div>{format(new Date(user.lastLogin), 'MMM dd, yyyy')}</div>
                            <div className="text-xs text-gray-400">
                              {format(new Date(user.lastLogin), 'HH:mm')}
                            </div>
                          </div>
                        ) : (
                          'Never'
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleToggleLock(user)}
                            aria-label={`${user.isLocked ? 'Unlock' : 'Lock'} user ${user.username}`}
                          >
                            {user.isLocked ? (
                              <LockOpenIcon className="h-4 w-4" />
                            ) : (
                              <LockClosedIcon className="h-4 w-4" />
                            )}
                          </Button>
                          
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleEditUser(user)}
                            aria-label={`Edit user ${user.username}`}
                          >
                            <PencilIcon className="h-4 w-4" />
                          </Button>
                          
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteUser(user)}
                            aria-label={`Delete user ${user.username}`}
                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          >
                            <TrashIcon className="h-4 w-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Pagination */}
      {!loading && users.length > 0 && totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 mt-6 rounded-b-lg">
          <div className="flex flex-1 justify-between sm:hidden">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
              disabled={currentPage === 0}
            >
              Previous
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
              disabled={currentPage >= totalPages - 1}
            >
              Next
            </Button>
          </div>
          <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">{currentPage * 20 + 1}</span> to{' '}
                <span className="font-medium">
                  {Math.min((currentPage + 1) * 20, totalElements)}
                </span>{' '}
                of <span className="font-medium">{totalElements}</span> users
              </p>
            </div>
            <div>
              <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                  disabled={currentPage === 0}
                  className="rounded-l-md"
                >
                  Previous
                </Button>
                
                {/* Page numbers */}
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let pageNum = currentPage - 2 + i;
                  if (pageNum < 0) pageNum = i;
                  if (pageNum >= totalPages) pageNum = totalPages - 5 + i;
                  if (pageNum < 0 || pageNum >= totalPages) return null;
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === currentPage ? "primary" : "ghost"}
                      size="sm"
                      onClick={() => setCurrentPage(pageNum)}
                      className="min-w-[40px]"
                    >
                      {pageNum + 1}
                    </Button>
                  );
                })}
                
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
                  disabled={currentPage >= totalPages - 1}
                  className="rounded-r-md"
                >
                  Next
                </Button>
              </nav>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserList;
