import { useState, useEffect } from 'react';
import type { Customer } from '@/types';

interface DuplicateWarnings {
  email?: string;
  mobile?: string;
  pan?: string;
  aadhar?: string;
}

interface UseDuplicateDetectionProps {
  customers: Customer[];
  email: string;
  mobileNumber: string;
  panNumber?: string;
  aadharNumber?: string;
  excludeCustomerId?: number; // For editing existing customers
}

export function useDuplicateDetection({
  customers,
  email,
  mobileNumber,
  panNumber,
  aadharNumber,
  excludeCustomerId,
}: UseDuplicateDetectionProps) {
  const [warnings, setWarnings] = useState<DuplicateWarnings>({});
  const [hasDuplicates, setHasDuplicates] = useState(false);

  useEffect(() => {
    const newWarnings: DuplicateWarnings = {};

    // Check email duplicates
    if (email && email.trim()) {
      const emailDuplicate = customers.find(
        (c) => c.email.toLowerCase() === email.toLowerCase() && c.id !== excludeCustomerId
      );
      if (emailDuplicate) {
        newWarnings.email = `Email already registered to ${emailDuplicate.fullName}`;
      }
    }

    // Check mobile duplicates
    if (mobileNumber && mobileNumber.trim()) {
      const mobileDuplicate = customers.find(
        (c) => c.mobileNumber === mobileNumber && c.id !== excludeCustomerId
      );
      if (mobileDuplicate) {
        newWarnings.mobile = `Mobile number already registered to ${mobileDuplicate.fullName}`;
      }
    }

    // Check PAN duplicates
    if (panNumber && panNumber.trim()) {
      const panDuplicate = customers.find(
        (c) => c.panNumber?.toUpperCase() === panNumber.toUpperCase() && c.id !== excludeCustomerId
      );
      if (panDuplicate) {
        newWarnings.pan = `PAN number already registered to ${panDuplicate.fullName}`;
      }
    }

    // Check Aadhar duplicates
    if (aadharNumber && aadharNumber.trim()) {
      const aadharDuplicate = customers.find(
        (c) => c.aadharNumber === aadharNumber && c.id !== excludeCustomerId
      );
      if (aadharDuplicate) {
        newWarnings.aadhar = `Aadhar number already registered to ${aadharDuplicate.fullName}`;
      }
    }

    setWarnings(newWarnings);
    setHasDuplicates(Object.keys(newWarnings).length > 0);
  }, [customers, email, mobileNumber, panNumber, aadharNumber, excludeCustomerId]);

  return { warnings, hasDuplicates };
}
