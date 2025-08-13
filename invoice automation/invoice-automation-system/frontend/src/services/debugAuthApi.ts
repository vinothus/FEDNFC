// Debug utility to check auth state
export const debugAuth = () => {
  console.log('🔍 Auth Debug Info:');
  console.log('Access Token:', localStorage.getItem('accessToken'));
  console.log('Refresh Token:', localStorage.getItem('refreshToken'));
  console.log('User:', localStorage.getItem('user'));
  
  const token = localStorage.getItem('accessToken');
  if (token) {
    try {
      // Parse JWT payload (without verification, just for debugging)
      const parts = token.split('.');
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1]));
        console.log('JWT Payload:', payload);
        console.log('JWT Expires:', new Date(payload.exp * 1000));
        console.log('JWT Valid:', payload.exp * 1000 > Date.now());
      }
    } catch (e) {
      console.log('Error parsing JWT:', e);
    }
  } else {
    console.log('❌ No access token found in localStorage');
  }
  
  return {
    hasToken: !!token,
    tokenLength: token?.length,
    isValid: token ? (JSON.parse(atob(token.split('.')[1]))?.exp * 1000 > Date.now()) : false
  };
};

// Add to window for easy debugging
(window as any).debugAuth = debugAuth;
