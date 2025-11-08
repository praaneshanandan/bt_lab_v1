import { AlertTriangle } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';

interface DuplicateWarningProps {
  message: string;
  field: 'email' | 'mobile' | 'pan' | 'aadhar';
}

export function DuplicateWarning({ message, field }: DuplicateWarningProps) {
  const fieldNames = {
    email: 'Email',
    mobile: 'Mobile Number',
    pan: 'PAN',
    aadhar: 'Aadhar'
  };

  const suggestions = {
    email: 'If this is your email, try logging in or use the "Forgot Password" feature.',
    mobile: 'If this is your mobile number, please contact support for assistance.',
    pan: 'Each PAN number can only be registered once. Please verify your details.',
    aadhar: 'Each Aadhar number can only be registered once. Please verify your details.'
  };

  return (
    <Alert variant="destructive" className="mt-2">
      <AlertTriangle className="h-4 w-4" />
      <AlertDescription>
        <strong>{fieldNames[field]} Duplicate:</strong> {message}
        <br />
        <span className="text-sm mt-1 block">{suggestions[field]}</span>
      </AlertDescription>
    </Alert>
  );
}

