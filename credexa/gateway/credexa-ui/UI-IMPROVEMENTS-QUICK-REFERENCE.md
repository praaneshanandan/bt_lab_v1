# UI Improvements Quick Reference Guide

## For Developers

### Session Management

**Hook Usage**:
```typescript
import { useSessionManager } from '@/hooks/useSessionManager';

// In your root component (App.tsx)
const { showWarning, remainingTime, continueSession, logout } = useSessionManager();

// Display the warning modal
<SessionWarningModal 
  open={showWarning} 
  remainingTime={remainingTime}
  onContinue={continueSession}
  onLogout={logout}
/>
```

**Configuration**:
- Timeout: 5 minutes (300,000ms) - Line 6 in `useSessionManager.ts`
- Warning Period: 30 seconds (30,000ms) - Line 7 in `useSessionManager.ts`
- Tracked Events: mousedown, keydown, scroll, touchstart, click

---

### Authentication Utils

**Import**:
```typescript
import { 
  hasRole, 
  hasAnyRole, 
  isAdmin, 
  isManager, 
  isManagerOrAdmin,
  getUsername,
  isTokenExpired 
} from '@/utils/auth';
```

**Common Usage**:
```typescript
// Check single role
if (hasRole('ROLE_ADMIN')) {
  // Admin-only code
}

// Check multiple roles
if (hasAnyRole(['ROLE_MANAGER', 'ROLE_ADMIN'])) {
  // Manager or Admin code
}

// Convenience methods
if (isManagerOrAdmin()) {
  // Show admin features
}

// Get user info
const username = getUsername();

// Check token validity
if (isTokenExpired()) {
  // Handle expired token
}
```

---

### Duplicate Detection

**Hook Usage**:
```typescript
import { useDuplicateDetection } from '@/hooks/useDuplicateDetection';

const { warnings, hasDuplicates } = useDuplicateDetection({
  customers,                    // Array of existing customers
  email: newCustomer.email,
  mobileNumber: newCustomer.mobileNumber,
  panNumber: newCustomer.panNumber,
  aadharNumber: newCustomer.aadharNumber,
  excludeCustomerId: customerId // Optional: for edit scenarios
});

// Check if specific field has duplicate
if (warnings.email) {
  // Show warning for email
}

// Check if any duplicates exist
if (hasDuplicates) {
  // Disable submit button
}
```

**Display Warnings**:
```typescript
import { DuplicateWarning } from '@/components/DuplicateWarning';

{warnings.email && (
  <DuplicateWarning message={warnings.email} field="email" />
)}
```

---

### Password Strength Indicator

**Component Usage**:
```typescript
import { PasswordStrength } from '@/components/PasswordStrength';

<PasswordStrength password={formData.password} />
```

**Validation Rules**:
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one number (0-9)
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

**Strength Levels**:
- Weak (0-1 checks): Red
- Medium (2-3 checks): Yellow
- Good (4 checks): Blue
- Strong (5 checks): Green

---

### Profile Completion

**Component Usage**:
```typescript
import { ProfileCompletion } from '@/components/ProfileCompletion';

<ProfileCompletion customer={customerData} />
```

**Tracked Sections**:
1. Basic Info: fullName, dateOfBirth, gender
2. Contact Info: email, mobileNumber
3. Identity Docs: panNumber, aadharNumber
4. Complete Address: addressLine1, city, state, pincode, country
5. Banking Details: accountNumber, ifscCode

---

### Role-Based Route Protection

**Page-Level Protection**:
```typescript
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isManagerOrAdmin } from '@/utils/auth';
import { toast } from 'sonner';

useEffect(() => {
  if (!isManagerOrAdmin()) {
    toast.info('Unauthorized access');
    navigate('/profile');
    return;
  }
}, [navigate]);
```

**Component-Level Protection**:
```typescript
import { isManagerOrAdmin } from '@/utils/auth';

{isManagerOrAdmin() && (
  <AdminOnlyComponent />
)}
```

---

### Customer 360° View

**Navigation**:
```typescript
import { useNavigate } from 'react-router-dom';

const navigate = useNavigate();

// Navigate to 360° view
navigate(\`/customers/\${customerId}/360-view\`);
```

**Route Configuration** (in App.tsx):
```typescript
<Route path="/customers/:id/360-view" element={<Customer360View />} />
```

---

### Search & Filters

**State Management**:
```typescript
const [searchTerm, setSearchTerm] = useState('');
const [classificationFilter, setClassificationFilter] = useState<string>('ALL');
const [kycStatusFilter, setKycStatusFilter] = useState<string>('ALL');
const [activeStatusFilter, setActiveStatusFilter] = useState<string>('ALL');
```

**Filter Logic**:
```typescript
const filteredCustomers = customers.filter((customer) => {
  // Search filter
  const matchesSearch = 
    !searchTerm ||
    customer.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.mobileNumber.includes(searchTerm) ||
    customer.panNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.aadharNumber?.includes(searchTerm) ||
    customer.username?.toLowerCase().includes(searchTerm.toLowerCase());

  // Classification filter
  const matchesClassification = 
    classificationFilter === 'ALL' || 
    customer.classification === classificationFilter;

  // KYC filter
  const matchesKYC = 
    kycStatusFilter === 'ALL' || 
    customer.kycStatus === kycStatusFilter;

  // Status filter
  const matchesStatus = 
    activeStatusFilter === 'ALL' ||
    (activeStatusFilter === 'ACTIVE' && customer.isActive) ||
    (activeStatusFilter === 'INACTIVE' && !customer.isActive);

  return matchesSearch && matchesClassification && matchesKYC && matchesStatus;
});
```

**Export to CSV**:
```typescript
const exportToCSV = () => {
  const headers = ['ID', 'Name', 'Email', 'Mobile', 'Classification', 'KYC Status', 'Status', 'DOB', 'City'];
  const csvData = filteredCustomers.map(customer => [
    customer.id,
    customer.fullName,
    customer.email,
    customer.mobileNumber,
    customer.classification,
    customer.kycStatus,
    customer.isActive ? 'Active' : 'Inactive',
    customer.dateOfBirth,
    customer.city
  ]);

  const csvContent = [
    headers.join(','),
    ...csvData.map(row => row.join(','))
  ].join('\\n');

  const blob = new Blob([csvContent], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = \`customers_\${new Date().toISOString().split('T')[0]}.csv\`;
  a.click();
  window.URL.revokeObjectURL(url);
  toast.success('Customer data exported successfully!');
};
```

---

## Common Patterns

### Toast Notifications
```typescript
import { toast } from 'sonner';

// Success
toast.success('Operation completed successfully!');

// Error
toast.error('Operation failed. Please try again.');

// Info
toast.info('Important information');

// Warning
toast.warning('Please review your input');
```

### Loading States
```typescript
const [loading, setLoading] = useState(false);

const handleSubmit = async () => {
  setLoading(true);
  try {
    await api.call();
    toast.success('Success!');
  } catch (error) {
    toast.error('Failed!');
  } finally {
    setLoading(false);
  }
};

<Button disabled={loading}>
  {loading ? 'Loading...' : 'Submit'}
</Button>
```

### Form Validation
```typescript
const [errors, setErrors] = useState<Record<string, string>>({});

const validate = () => {
  const newErrors: Record<string, string> = {};
  
  if (!formData.email) {
    newErrors.email = 'Email is required';
  } else if (!/^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/.test(formData.email)) {
    newErrors.email = 'Invalid email format';
  }
  
  if (!formData.mobileNumber) {
    newErrors.mobileNumber = 'Mobile number is required';
  } else if (!/^[0-9]{10}$/.test(formData.mobileNumber)) {
    newErrors.mobileNumber = 'Invalid mobile number (10 digits required)';
  }
  
  setErrors(newErrors);
  return Object.keys(newErrors).length === 0;
};

const handleSubmit = (e: React.FormEvent) => {
  e.preventDefault();
  if (validate()) {
    // Submit form
  }
};
```

---

## Environment Variables

Currently, no environment variables are required for the UI improvements. All configuration is hardcoded in the source files.

**Potential Future Environment Variables**:
```env
# Session timeout in milliseconds
VITE_SESSION_TIMEOUT=300000

# Session warning period in milliseconds
VITE_SESSION_WARNING=30000

# API base URL
VITE_API_BASE_URL=http://localhost:8080
```

---

## Debugging Tips

### Session Management Issues
1. Check browser console for activity event logs
2. Verify localStorage contains 'token' key
3. Check token expiration timestamp
4. Verify event listeners are attached

### Duplicate Detection Not Working
1. Ensure customers array is populated
2. Check customer data structure matches expected format
3. Verify field names match exactly
4. Check browser console for errors

### Role-Based Access Issues
1. Verify token exists in localStorage
2. Check token structure contains 'roles' array
3. Verify role names match exactly (case-sensitive)
4. Check browser console for JWT decode errors

### Filter/Search Not Working
1. Verify filter state values
2. Check if customer data is loaded
3. Verify field names in filter logic
4. Check for case sensitivity issues

---

## Browser Compatibility

All features use standard ES6+ JavaScript and React hooks. Compatible with:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

**Note**: JWT decoding uses atob() which is supported in all modern browsers.

---

## Performance Tips

1. **Duplicate Detection**: For large customer lists (1000+), consider:
   - Adding debouncing to reduce checks
   - Moving validation to backend
   - Using indexed data structures

2. **Search & Filters**: For large datasets:
   - Implement pagination
   - Add server-side filtering
   - Use virtual scrolling for tables

3. **Session Management**: 
   - Event listeners are throttled naturally by browser
   - Consider adding explicit throttling for very heavy pages

---

## Support & Questions

For questions or issues with the UI improvements:
1. Check this guide first
2. Review the implementation summary document
3. Check browser console for errors
4. Verify all dependencies are installed
5. Ensure backend services are running (for data-dependent features)

---

**Last Updated**: $(date)
**Version**: 1.0.0
**Maintainer**: Development Team
