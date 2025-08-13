import React, { createContext, useContext, useReducer, useEffect, useCallback } from 'react';
import { AuthState, AuthContextType, LoginRequest, User } from '../types/auth';
import { authService } from '../services/authApi';

// Auth action types
type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; accessToken: string; refreshToken: string } }
  | { type: 'LOGIN_FAILURE'; payload: string }
  | { type: 'LOGOUT' }
  | { type: 'REFRESH_START' }
  | { type: 'REFRESH_SUCCESS'; payload: { user: User; accessToken: string; refreshToken: string } }
  | { type: 'REFRESH_FAILURE' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'SET_LOADING'; payload: boolean };

// Initial auth state
const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: true, // Start with loading true to check stored auth
  error: null,
};

// Auth reducer
const authReducer = (state: AuthState, action: AuthAction): AuthState => {
  switch (action.type) {
    case 'LOGIN_START':
      return {
        ...state,
        isLoading: true,
        error: null,
      };
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        accessToken: action.payload.accessToken,
        refreshToken: action.payload.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      };
    case 'LOGIN_FAILURE':
      return {
        ...state,
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        error: action.payload,
      };
    case 'LOGOUT':
      return {
        ...state,
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      };
    case 'REFRESH_START':
      return {
        ...state,
        isLoading: true,
        error: null,
      };
    case 'REFRESH_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        accessToken: action.payload.accessToken,
        refreshToken: action.payload.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      };
    case 'REFRESH_FAILURE':
      return {
        ...state,
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      };
    case 'CLEAR_ERROR':
      return {
        ...state,
        error: null,
      };
    case 'SET_LOADING':
      return {
        ...state,
        isLoading: action.payload,
      };
    default:
      return state;
  }
};

// Create context
const AuthContext = createContext<AuthContextType | null>(null);

// Auth provider component
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);
  const lastRefreshTimeRef = React.useRef<number>(0);

  // Helper functions for role checking
  const hasRole = useCallback((role: string): boolean => {
    // Backend sends single role, so check direct equality
    return state.user?.role === role || false;
  }, [state.user?.role]);

  const hasAnyRole = useCallback((roles: string[]): boolean => {
    return roles.some(role => hasRole(role));
  }, [hasRole]);

  // Store tokens in localStorage
  const storeTokens = useCallback((user: User, accessToken: string, refreshToken: string) => {
    console.log('💾 AuthContext: Storing tokens for user:', user.username);
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    console.log('✅ AuthContext: Tokens stored successfully');
  }, []);

  // Clear stored tokens
  const clearTokens = useCallback(() => {
    console.log('🗑️ AuthContext: Clearing stored tokens');
    localStorage.removeItem('user');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    console.log('✅ AuthContext: Tokens cleared');
  }, []);

  // Login function
  const login = useCallback(async (credentials: LoginRequest): Promise<void> => {
    console.log('🔐 AuthContext: Starting login for:', credentials.usernameOrEmail);
    dispatch({ type: 'LOGIN_START' });
    
    try {
      const response = await authService.login(credentials);
      console.log('📡 AuthContext: Login API response received');
      
      if (response.accessToken && response.user) {
        const { user, accessToken, refreshToken } = response;
        console.log('✅ AuthContext: Valid login response for user:', user.username);
        
        // Store tokens
        storeTokens(user, accessToken, refreshToken);
        
        dispatch({
          type: 'LOGIN_SUCCESS',
          payload: { user, accessToken, refreshToken }
        });
        console.log('🎉 AuthContext: Login successful!');
      } else {
        throw new Error('Login failed - invalid response');
      }
    } catch (error: any) {
      console.error('❌ AuthContext: Login failed:', error.message);
      dispatch({
        type: 'LOGIN_FAILURE',
        payload: error.message || 'Login failed'
      });
      throw error;
    }
  }, [storeTokens]);

  // Logout function
  const logout = useCallback(async (): Promise<void> => {
    try {
      await authService.logout();
    } catch (error) {
      console.warn('Logout API call failed:', error);
    } finally {
      clearTokens();
      dispatch({ type: 'LOGOUT' });
    }
  }, [clearTokens]);

  // Refresh auth function
  const refreshAuth = useCallback(async (): Promise<void> => {
    const storedRefreshToken = localStorage.getItem('refreshToken');
    
    if (!storedRefreshToken) {
      dispatch({ type: 'REFRESH_FAILURE' });
      return;
    }

    dispatch({ type: 'REFRESH_START' });
    
    try {
      const response = await authService.refreshToken({ refreshToken: storedRefreshToken });
      
      if (response.accessToken && response.user) {
        const { user, accessToken, refreshToken } = response;
        
        // Store new tokens
        storeTokens(user, accessToken, refreshToken);
        
        dispatch({
          type: 'REFRESH_SUCCESS',
          payload: { user, accessToken, refreshToken }
        });
      } else {
        throw new Error('Token refresh failed');
      }
    } catch (error) {
      clearTokens();
      dispatch({ type: 'REFRESH_FAILURE' });
    }
  }, [storeTokens, clearTokens]);

  // Clear error function
  const clearError = useCallback(() => {
    dispatch({ type: 'CLEAR_ERROR' });
  }, []);

  // Initialize auth on app start
  useEffect(() => {
    const initializeAuth = async () => {
      console.log('🔄 AuthContext: Initializing authentication...');
      
      const storedUser = localStorage.getItem('user');
      const storedAccessToken = localStorage.getItem('accessToken');
      const storedRefreshToken = localStorage.getItem('refreshToken');

      console.log('🔍 AuthContext: Stored tokens check:', {
        hasUser: !!storedUser,
        hasAccessToken: !!storedAccessToken,
        hasRefreshToken: !!storedRefreshToken
      });

      if (storedUser && storedAccessToken && storedRefreshToken) {
        try {
          const user = JSON.parse(storedUser);
          console.log('👤 AuthContext: Found stored user:', user.username);
          
          // Basic JWT validation - check if token is properly formatted
          const tokenParts = storedAccessToken.split('.');
          if (tokenParts.length !== 3) {
            throw new Error('Invalid token format');
          }
          
          // Try to get current user info to validate token
          console.log('🔍 AuthContext: Validating token with server...');
          const currentUser = await authService.getCurrentUser();
          
          console.log('✅ AuthContext: Token validation successful');
          dispatch({
            type: 'LOGIN_SUCCESS',
            payload: {
              user: currentUser,
              accessToken: storedAccessToken,
              refreshToken: storedRefreshToken
            }
          });
        } catch (error: any) {
          // Prevent infinite refresh loops - only try to refresh if we haven't done so recently
          const now = Date.now();
          const refreshCooldown = 5000; // 5 seconds
          
          if (now - lastRefreshTimeRef.current < refreshCooldown) {
            console.warn('⚠️ AuthContext: Refresh attempted too recently, skipping to prevent loop');
            clearTokens();
            dispatch({ type: 'LOGOUT' });
            dispatch({ type: 'SET_LOADING', payload: false });
            return;
          }
          
          console.warn('⚠️ AuthContext: Token validation failed, attempting refresh...', error);
          // If validation fails, try to refresh
          try {
            lastRefreshTimeRef.current = now;
            await refreshAuth();
          } catch (refreshError: any) {
            console.error('❌ AuthContext: Refresh failed, clearing tokens', refreshError);
            clearTokens();
            dispatch({ type: 'LOGOUT' });
            dispatch({ type: 'SET_LOADING', payload: false });
          }
        }
      } else {
        console.log('❌ AuthContext: No stored authentication found');
        dispatch({ type: 'SET_LOADING', payload: false });
      }
    };

    initializeAuth();
  }, []); // Only run on component mount

  // Separate effect for checking token changes
  useEffect(() => {
    // Check for token changes periodically (since storage event doesn't fire on same tab)
    const checkTokens = () => {
      const hasTokens = !!localStorage.getItem('accessToken');
      if (!hasTokens && state.isAuthenticated) {
        console.log('🔄 AuthContext: Detected token removal, logging out...');
        dispatch({ type: 'LOGOUT' });
      }
    };

    const interval = setInterval(checkTokens, 1000);
    
    // Cleanup interval
    return () => {
      clearInterval(interval);
    };
  }, [state.isAuthenticated]); // Only run when auth state changes

  const contextValue: AuthContextType = {
    ...state,
    login,
    logout,
    refreshAuth,
    clearError,
    hasRole,
    hasAnyRole,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

// Custom hook to use auth context
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
