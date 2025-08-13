import { AxiosResponse } from 'axios';
import { api } from './api';

// Types for User Management
export interface UserAccount {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'USER' | 'APPROVER';
  isActive: boolean;
  isLocked: boolean;
  failedLoginAttempts: number;
  lastLogin?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  role: 'ADMIN' | 'USER' | 'APPROVER';
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password?: string;
  role: 'ADMIN' | 'USER' | 'APPROVER';
  isActive: boolean;
}

export interface UsersPageResponse {
  content: UserAccount[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface UserStatsResponse {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  lockedUsers: number;
  adminUsers: number;
  approverUsers: number;
  regularUsers: number;
}

export interface UserSearchParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  search?: string;
  role?: string;
  active?: boolean;
}

/**
 * User Management API Service
 * Provides CRUD operations for user accounts (Admin only)
 */
export const userApi = {
  /**
   * Get all users with pagination and filtering
   */
  getAllUsers: async (params: UserSearchParams = {}): Promise<UsersPageResponse> => {
    const queryParams = new URLSearchParams();
    
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortDir) queryParams.append('sortDir', params.sortDir);
    if (params.search) queryParams.append('search', params.search);
    if (params.role) queryParams.append('role', params.role);
    if (params.active !== undefined) queryParams.append('active', params.active.toString());
    
    const response: AxiosResponse<UsersPageResponse> = await api.get(
      `/api/admin/users?${queryParams.toString()}`
    );
    return response.data;
  },

  /**
   * Get user by ID
   */
  getUserById: async (id: string): Promise<UserAccount> => {
    const response: AxiosResponse<UserAccount> = await api.get(`/api/admin/users/${id}`);
    return response.data;
  },

  /**
   * Create new user
   */
  createUser: async (userData: CreateUserRequest): Promise<UserAccount> => {
    const response: AxiosResponse<UserAccount> = await api.post('/api/admin/users', userData);
    return response.data;
  },

  /**
   * Update existing user
   */
  updateUser: async (id: string, userData: UpdateUserRequest): Promise<UserAccount> => {
    const response: AxiosResponse<UserAccount> = await api.put(`/api/admin/users/${id}`, userData);
    return response.data;
  },

  /**
   * Delete user (soft delete - deactivates the account)
   */
  deleteUser: async (id: string): Promise<void> => {
    await api.delete(`/api/admin/users/${id}`);
  },

  /**
   * Toggle user lock status
   */
  toggleUserLock: async (id: string): Promise<UserAccount> => {
    const response: AxiosResponse<UserAccount> = await api.post(`/api/admin/users/${id}/toggle-lock`);
    return response.data;
  },

  /**
   * Reset user password
   */
  resetUserPassword: async (id: string, newPassword: string): Promise<void> => {
    const params = new URLSearchParams();
    params.append('newPassword', newPassword);
    await api.post(`/api/admin/users/${id}/reset-password?${params.toString()}`);
  },

  /**
   * Get user statistics
   */
  getUserStats: async (): Promise<UserStatsResponse> => {
    const response: AxiosResponse<UserStatsResponse> = await api.get('/api/admin/users/stats');
    return response.data;
  },
};

export default userApi;
