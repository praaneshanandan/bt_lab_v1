import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function isManagerOrAdmin(): boolean {
  const rolesStr = localStorage.getItem('userRoles');
  if (!rolesStr) return false;
  
  try {
    const roles = JSON.parse(rolesStr);
    // roles is an array like ["ROLE_ADMIN"] or ["ROLE_MANAGER"]
    if (Array.isArray(roles)) {
      return roles.some(role => {
        const upperRole = role.toUpperCase();
        return upperRole === 'ADMIN' || upperRole === 'ROLE_ADMIN' || 
               upperRole === 'MANAGER' || upperRole === 'ROLE_MANAGER';
      });
    }
    return false;
  } catch {
    return false;
  }
}
