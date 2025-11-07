// JWT token utilities
export interface DecodedToken {
  sub: string; // username
  roles: string[];
  iat: number;
  exp: number;
}

export const decodeToken = (token: string): DecodedToken | null => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Error decoding token:', error);
    return null;
  }
};

export const getUserRoles = (): string[] => {
  const token = localStorage.getItem('authToken');
  if (!token) return [];
  
  const decoded = decodeToken(token);
  return decoded?.roles || [];
};

export const hasRole = (role: string): boolean => {
  const roles = getUserRoles();
  return roles.includes(role);
};

export const hasAnyRole = (requiredRoles: string[]): boolean => {
  const roles = getUserRoles();
  return requiredRoles.some(role => roles.includes(role));
};

export const isAdmin = (): boolean => {
  return hasRole('ROLE_ADMIN');
};

export const isManager = (): boolean => {
  return hasAnyRole(['ROLE_MANAGER', 'ROLE_CUSTOMER_MANAGER']);
};

export const isCustomer = (): boolean => {
  return hasAnyRole(['ROLE_CUSTOMER', 'ROLE_USER']);
};

export const isManagerOrAdmin = (): boolean => {
  return hasAnyRole(['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CUSTOMER_MANAGER']);
};

export const getUsername = (): string | null => {
  const token = localStorage.getItem('authToken');
  if (!token) return null;
  
  const decoded = decodeToken(token);
  return decoded?.sub || null;
};

export const getTokenExpiration = (): number | null => {
  const token = localStorage.getItem('authToken');
  if (!token) return null;
  
  const decoded = decodeToken(token);
  return decoded?.exp || null;
};

export const isTokenExpired = (): boolean => {
  const exp = getTokenExpiration();
  if (!exp) return true;
  
  return Date.now() >= exp * 1000;
};
