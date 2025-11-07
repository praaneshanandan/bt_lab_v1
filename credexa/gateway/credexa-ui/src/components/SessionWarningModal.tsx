import { AlertTriangle, LogOut, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface SessionWarningModalProps {
  open: boolean;
  secondsRemaining: number;
  onContinue: () => void;
  onLogout: () => void;
}

export function SessionWarningModal({
  open,
  secondsRemaining,
  onContinue,
  onLogout,
}: SessionWarningModalProps) {
  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-md" onPointerDownOutside={(e) => e.preventDefault()}>
        <DialogHeader>
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-6 w-6 text-orange-500" />
            <DialogTitle>Session Expiring Soon</DialogTitle>
          </div>
          <DialogDescription className="pt-2">
            You've been inactive for a while. For your security, you'll be automatically logged out in:
          </DialogDescription>
        </DialogHeader>
        
        <div className="flex justify-center py-6">
          <div className="text-center">
            <div className="text-6xl font-bold text-orange-500 mb-2">
              {secondsRemaining}
            </div>
            <div className="text-sm text-gray-600">seconds remaining</div>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            variant="outline"
            onClick={onLogout}
            className="flex items-center gap-2"
          >
            <LogOut size={16} />
            Logout Now
          </Button>
          <Button
            onClick={onContinue}
            className="flex items-center gap-2"
          >
            <RefreshCw size={16} />
            Continue Session
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
