import { Check, X, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Customer } from '@/types';

interface ProfileCompletionProps {
  profile: Customer;
}

export function ProfileCompletion({ profile }: ProfileCompletionProps) {
  const checks = {
    basicInfo: !!(profile.fullName && profile.dateOfBirth && profile.gender),
    contactInfo: !!(profile.email && profile.mobileNumber),
    identityDocs: !!(profile.panNumber && profile.aadharNumber),
    addressComplete: !!(profile.addressLine1 && profile.city && profile.state && profile.pincode && profile.country),
    bankingDetails: !!(profile.accountNumber && profile.ifscCode),
  };

  const completedCount = Object.values(checks).filter(Boolean).length;
  const totalCount = Object.keys(checks).length;
  const completionPercentage = Math.round((completedCount / totalCount) * 100);

  const isComplete = completionPercentage === 100;

  return (
    <Card className={`${isComplete ? 'border-green-200 bg-green-50' : 'border-orange-200 bg-orange-50'}`}>
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center gap-2">
          {isComplete ? (
            <Check className="h-5 w-5 text-green-600" />
          ) : (
            <AlertCircle className="h-5 w-5 text-orange-600" />
          )}
          Profile Completion
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Progress Bar */}
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="font-medium">
              {isComplete ? 'Profile Complete!' : `${completionPercentage}% Complete`}
            </span>
            <span className="text-gray-600">
              {completedCount}/{totalCount} sections
            </span>
          </div>
          <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-500 ${
                isComplete ? 'bg-green-500' : completionPercentage >= 60 ? 'bg-blue-500' : 'bg-orange-500'
              }`}
              style={{ width: `${completionPercentage}%` }}
            />
          </div>
        </div>

        {/* Checklist */}
        <div className="space-y-2">
          <ChecklistItem
            completed={checks.basicInfo}
            label="Basic Information"
            description="Name, Date of Birth, Gender"
          />
          <ChecklistItem
            completed={checks.contactInfo}
            label="Contact Information"
            description="Email and Mobile Number"
          />
          <ChecklistItem
            completed={checks.identityDocs}
            label="Identity Documents"
            description="PAN and Aadhar Numbers"
          />
          <ChecklistItem
            completed={checks.addressComplete}
            label="Complete Address"
            description="Full address with pincode"
          />
          <ChecklistItem
            completed={checks.bankingDetails}
            label="Banking Details"
            description="Account Number and IFSC Code"
          />
        </div>

        {!isComplete && (
          <div className="pt-2 border-t">
            <p className="text-sm text-gray-700">
              <strong>Complete your profile</strong> to unlock all features and ensure smooth FD account processing!
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function ChecklistItem({
  completed,
  label,
  description,
}: {
  completed: boolean;
  label: string;
  description: string;
}) {
  return (
    <div className="flex items-start gap-2">
      <div className="mt-0.5">
        {completed ? (
          <div className="h-5 w-5 rounded-full bg-green-500 flex items-center justify-center">
            <Check className="h-3 w-3 text-white" />
          </div>
        ) : (
          <div className="h-5 w-5 rounded-full border-2 border-gray-300 flex items-center justify-center">
            <X className="h-3 w-3 text-gray-400" />
          </div>
        )}
      </div>
      <div className="flex-1">
        <div className={`text-sm font-medium ${completed ? 'text-green-700' : 'text-gray-700'}`}>
          {label}
        </div>
        <div className="text-xs text-gray-600">{description}</div>
      </div>
    </div>
  );
}
