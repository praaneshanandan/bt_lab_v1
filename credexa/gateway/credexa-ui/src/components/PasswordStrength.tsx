import { Check, X } from 'lucide-react';

interface PasswordStrengthProps {
  password: string;
}

export function PasswordStrength({ password }: PasswordStrengthProps) {
  const checks = {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>]/.test(password),
  };

  const passedChecks = Object.values(checks).filter(Boolean).length;
  const strength = 
    passedChecks <= 1 ? 'weak' :
    passedChecks <= 3 ? 'medium' :
    passedChecks <= 4 ? 'good' : 'strong';

  const strengthColors = {
    weak: 'bg-red-500',
    medium: 'bg-yellow-500',
    good: 'bg-blue-500',
    strong: 'bg-green-500',
  };

  const strengthLabels = {
    weak: 'Weak',
    medium: 'Medium',
    good: 'Good',
    strong: 'Strong',
  };

  if (!password) return null;

  return (
    <div className="space-y-2 mt-2">
      <div className="flex items-center gap-2">
        <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-300 ${strengthColors[strength]}`}
            style={{ width: `${(passedChecks / 5) * 100}%` }}
          />
        </div>
        <span className={`text-sm font-medium ${
          strength === 'weak' ? 'text-red-600' :
          strength === 'medium' ? 'text-yellow-600' :
          strength === 'good' ? 'text-blue-600' :
          'text-green-600'
        }`}>
          {strengthLabels[strength]}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-1 text-xs">
        <CheckItem passed={checks.length} text="At least 8 characters" />
        <CheckItem passed={checks.uppercase} text="One uppercase letter" />
        <CheckItem passed={checks.lowercase} text="One lowercase letter" />
        <CheckItem passed={checks.number} text="One number" />
        <CheckItem passed={checks.special} text="One special character (!@#$...)" />
      </div>
    </div>
  );
}

function CheckItem({ passed, text }: { passed: boolean; text: string }) {
  return (
    <div className="flex items-center gap-1.5">
      {passed ? (
        <Check className="h-3 w-3 text-green-600 flex-shrink-0" />
      ) : (
        <X className="h-3 w-3 text-gray-400 flex-shrink-0" />
      )}
      <span className={passed ? 'text-green-700' : 'text-muted-foreground'}>
        {text}
      </span>
    </div>
  );
}

