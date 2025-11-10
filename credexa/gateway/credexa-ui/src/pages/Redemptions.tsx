import React, { useState } from 'react';
import { 
  Calculator, 
  TrendingDown, 
  AlertTriangle,
  CheckCircle,
  Info,
  DollarSign
} from 'lucide-react';
import { accountServiceApi } from '../services/api';
import { isManagerOrAdmin } from '../utils/auth';
import type { RedemptionInquiryResponse, ProcessRedemptionResponse } from '../types';

const Redemptions: React.FC = () => {
  const [idValue, setAccountNumber] = useState('');
  const [redemptionDate, setRedemptionDate] = useState('');
  const [reason, setReason] = useState('');
  const [inquiryResult, setInquiryResult] = useState<RedemptionInquiryResponse | null>(null);
  const [processResult, setProcessResult] = useState<ProcessRedemptionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const canProcess = isManagerOrAdmin();

  // Inquire redemption
  const handleInquiry = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setInquiryResult(null);
    setProcessResult(null);
    
    try {
      // Convert date to LocalDateTime format (YYYY-MM-DDTHH:mm:ss)
      const formattedDate = redemptionDate ? `${redemptionDate}T00:00:00` : undefined;
      
      const response = await accountServiceApi.inquireRedemption({
        idValue,
        redemptionDate: formattedDate,
      });
      console.log(response.data.data)
      setInquiryResult(response.data.data);
    } catch (err: any) {
      console.error('Error inquiring redemption:', err);
      setError(err.response?.data?.message || 'Failed to inquire redemption');
    } finally {
      setLoading(false);
    }
  };

  // Process redemption
  const handleProcess = async () => {
    if (!inquiryResult) return;
    
    if (!confirm(`Are you sure you want to process redemption for account ${idValue}? This action cannot be undone.`)) {
      return;
    }
    
    setProcessing(true);
    setError(null);
    
    try {
      // Convert date to LocalDateTime format (YYYY-MM-DDTHH:mm:ss)
      const formattedDate = redemptionDate ? `${redemptionDate}T00:00:00` : undefined;
      
      const response = await accountServiceApi.processRedemption({
        idValue: idValue,
        redemptionType: "FULL",
        redemptionDate: formattedDate,
        remarks: reason || undefined,
      });
      console.log(response.data)
      setProcessResult(response.data.data);
      setInquiryResult(null);
    } catch (err: any) {
      console.error('Error processing redemption:', err);
      setError(err.response?.data?.message || 'Failed to process redemption');
    } finally {
      setProcessing(false);
    }
  };

  // Format currency
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(amount);
  };

  // Format date
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-foreground">FD Redemption</h1>
        <p className="text-muted-foreground mt-1">
          Inquire and process premature withdrawal of Fixed Deposits
        </p>
      </div>

      {/* Inquiry Form */}
      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <Calculator className="w-5 h-5 text-primary" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">Redemption Inquiry</h2>
            <p className="text-sm text-muted-foreground">
              Calculate redemption amount and penalties
            </p>
          </div>
        </div>

        <form onSubmit={handleInquiry} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-2">
              Account Number *
            </label>
            <input
              type="text"
              required
              value={idValue}
              onChange={(e) => setAccountNumber(e.target.value)}
              placeholder="Enter FD account number"
              className="w-full px-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">
              Redemption Date (Optional)
            </label>
            <input
              type="date"
              value={redemptionDate}
              onChange={(e) => setRedemptionDate(e.target.value)}
              className="w-full px-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Leave empty to use today's date
            </p>
          </div>

          {canProcess && (
            <div>
              <label className="block text-sm font-medium text-foreground mb-2">
                Reason for Redemption (Optional)
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Enter reason for premature withdrawal..."
                rows={3}
                className="w-full px-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50 font-semibold"
          >
            {loading ? (
              <>
                <div className="w-5 h-5 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                Calculating...
              </>
            ) : (
              <>
                <Calculator className="w-5 h-5" />
                Calculate Redemption
              </>
            )}
          </button>
        </form>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Inquiry Result */}
      {inquiryResult && (
        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 bg-blue-500/10 rounded-lg flex items-center justify-center">
              <Info className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">Redemption Details</h2>
              <p className="text-sm text-muted-foreground">
                Account: {inquiryResult.accountNumber}
              </p>
            </div>
          </div>

          <div className="space-y-6">
            {/* Warning if premature */}
            {inquiryResult.redemptionType === 'PREMATURE' && (
              <div className="bg-yellow-100 dark:bg-yellow-900/30 border border-yellow-300 dark:border-yellow-800 rounded-lg p-4">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-yellow-800 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
                  <div>
                    <h4 className="font-semibold text-yellow-900 dark:text-yellow-300 mb-1">
                      Premature Withdrawal
                    </h4>
                    <p className="text-sm text-yellow-800 dark:text-yellow-400">
                      {inquiryResult.penaltyDescription || 'This is a premature withdrawal. Penalty charges will apply.'}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Amount Breakdown */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-muted rounded-lg p-4">
                <p className="text-sm text-muted-foreground mb-1">Principal Amount</p>
                <p className="text-2xl font-bold text-foreground">
                  {formatCurrency(inquiryResult.principalAmount)}
                </p>
              </div>

              <div className="bg-muted rounded-lg p-4">
                <p className="text-sm text-muted-foreground mb-1">Accrued Interest</p>
                <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                  {formatCurrency(inquiryResult.interestEarned)}
                </p>
              </div>

              {inquiryResult.penaltyAmount > 0 && (
                <div className="bg-red-100 dark:bg-red-900/30 rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-1">
                    <TrendingDown className="w-4 h-4 text-red-600 dark:text-red-400" />
                    <p className="text-sm text-red-800 dark:text-red-400">Penalty Charges</p>
                  </div>
                  <p className="text-2xl font-bold text-red-700 dark:text-red-300">
                    - {formatCurrency(inquiryResult.penaltyAmount)}
                  </p>
                </div>
              )}

              <div className="bg-primary/10 border border-primary/30 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-1">
                  <DollarSign className="w-4 h-4 text-primary" />
                  <p className="text-sm text-muted-foreground">Net Redemption Amount</p>
                </div>
                <p className="text-3xl font-bold text-primary">
                  {formatCurrency(inquiryResult.netRedemptionAmount)}
                </p>
              </div>
            </div>

            {/* Dates */}
            <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border">
              <div>
                <p className="text-sm text-muted-foreground">Maturity Date</p>
                <p className="font-semibold text-foreground mt-1">
                  {formatDate(inquiryResult.maturityDate)}
                </p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Inquiry Date</p>
                <p className="font-semibold text-foreground mt-1">
                  {formatDate(inquiryResult.inquiryDate)}
                </p>
              </div>
            </div>

            {/* Additional Info */}
            <div className="grid grid-cols-3 gap-4 pt-4 border-t border-border">
              <div>
                <p className="text-sm text-muted-foreground">Days Elapsed</p>
                <p className="font-semibold text-foreground mt-1">{inquiryResult.daysElapsed}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Days Remaining</p>
                <p className="font-semibold text-foreground mt-1">{inquiryResult.daysRemaining}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Redemption Type</p>
                <p className="font-semibold text-foreground mt-1">{inquiryResult.redemptionType}</p>
              </div>
            </div>

            {/* Remarks */}
            {inquiryResult.remarks && (
              <div className="bg-muted rounded-lg p-4">
                <p className="text-sm text-foreground">{inquiryResult.remarks}</p>
              </div>
            )}

            {/* Process Button (Admin/Manager only) */}
            {canProcess && (
              <button
                onClick={handleProcess}
                disabled={processing}
                className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors disabled:opacity-50 font-semibold"
              >
                {processing ? (
                  <>
                    <div className="w-5 h-5 border-2 border-destructive-foreground/30 border-t-destructive-foreground rounded-full animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <TrendingDown className="w-5 h-5" />
                    Process Redemption
                  </>
                )}
              </button>
            )}

            {!canProcess && (
              <div className="bg-muted border border-border rounded-lg p-4 text-center">
                <Info className="w-8 h-8 mx-auto mb-2 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">
                  Please contact your bank branch or customer service to process this redemption.
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Process Result */}
      {processResult && (
        <div className={`border rounded-lg p-6 ${
          processResult.redemptionStatus === 'COMPLETED' 
            ? 'bg-green-50 dark:bg-green-900/20 border-green-300 dark:border-green-800' 
            : 'bg-red-50 dark:bg-red-900/20 border-red-300 dark:border-red-800'
        }`}>
          <div className="flex items-center gap-3 mb-4">
            {processResult.redemptionStatus === 'COMPLETED' ? (
              <CheckCircle className="w-10 h-10 text-green-600 dark:text-green-400" />
            ) : (
              <AlertTriangle className="w-10 h-10 text-red-600 dark:text-red-400" />
            )}
            <div>
              <h2 className={`text-xl font-bold ${
                processResult.redemptionStatus === 'COMPLETED' 
                  ? 'text-green-900 dark:text-green-300' 
                  : 'text-red-900 dark:text-red-300'
              }`}>
                Redemption {processResult.redemptionStatus === 'COMPLETED' ? 'Successful' : 'Failed'}
              </h2>
              <p className={`text-sm ${
                processResult.redemptionStatus === 'COMPLETED' 
                  ? 'text-green-700 dark:text-green-400' 
                  : 'text-red-700 dark:text-red-400'
              }`}>
                {processResult.message}
              </p>
            </div>
          </div>

          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Account Number</p>
                <p className="font-mono font-semibold text-foreground mt-1">
                  {processResult.accountNumber}
                </p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Transaction Reference</p>
                <p className="font-mono text-sm font-semibold text-foreground mt-1">
                  {processResult.redemptionTransactionId}
                </p>
              </div>
            </div>

            <div className="bg-white dark:bg-gray-950 rounded-lg p-4">
              <p className="text-sm text-muted-foreground mb-1">Redemption Amount</p>
              <p className="text-3xl font-bold text-primary">
                {formatCurrency(processResult.netRedemptionAmount)}
              </p>
              {processResult.penaltyAmount > 0 && (
                <p className="text-sm text-red-600 dark:text-red-400 mt-1">
                  (Penalty: {formatCurrency(processResult.penaltyAmount)})
                </p>
              )}
            </div>

            <div>
              <p className="text-sm text-muted-foreground">Processed At</p>
              <p className="font-semibold text-foreground mt-1">
                {formatDate(processResult.redemptionDate)}
              </p>
            </div>

            <button
              onClick={() => {
                setProcessResult(null);
                setAccountNumber('');
                setRedemptionDate('');
                setReason('');
              }}
              className="w-full px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors font-semibold"
            >
              Process Another Redemption
            </button>
          </div>
        </div>
      )}

      {/* Information Box */}
      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <Info className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-blue-900 dark:text-blue-300">
            <h4 className="font-semibold mb-2">About FD Redemption</h4>
            <ul className="space-y-1 list-disc list-inside">
              <li>Premature withdrawals may incur penalty charges as per bank policy</li>
              <li>Redemption amount includes principal and accrued interest minus penalties</li>
              <li>Funds will be credited to your registered bank account</li>
              <li>Tax Deducted at Source (TDS) may apply as per regulations</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Redemptions;