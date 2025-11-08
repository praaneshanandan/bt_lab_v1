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
    <Card className={`${isComplete ? 'border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950' : 'border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950'}`}>
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center gap-2 text-foreground">
          {isComplete ? (
            <Check className="h-5 w-5 text-green-600 dark:text-green-400" />
          ) : (
            <AlertCircle className="h-5 w-5 text-orange-600 dark:text-orange-400" />
          )}
          Profile Completion
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Progress Bar */}
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="font-medium text-foreground">
              {isComplete ? 'Profile Complete!' : `${completionPercentage}% Complete`}
            </span>
            <span className="text-muted-foreground">
              {completedCount}/{totalCount} sections
            </span>
          </div>
          <div className="h-3 bg-muted rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-500 ${
                isComplete ? 'bg-green-500 dark:bg-green-600' : completionPercentage >= 60 ? 'bg-blue-500 dark:bg-blue-600' : 'bg-orange-500 dark:bg-orange-600'
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
          <div className="pt-2 border-t border-border">
            <p className="text-sm text-foreground">
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
          <div className="h-5 w-5 rounded-full bg-green-500 dark:bg-green-600 flex items-center justify-center">
            <Check className="h-3 w-3 text-white" />
          </div>
        ) : (
          <div className="h-5 w-5 rounded-full border-2 border-muted-foreground flex items-center justify-center">
            <X className="h-3 w-3 text-muted-foreground" />
          </div>
        )}
      </div>
      <div className="flex-1">
        <div className={`text-sm font-medium ${completed ? 'text-green-700 dark:text-green-300' : 'text-foreground'}`}>
          {label}
        </div>
        <div className="text-xs text-muted-foreground">{description}</div>
      </div>
    </div>
  );
}

