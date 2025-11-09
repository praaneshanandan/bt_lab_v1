import React, { useState, useEffect } from 'react';
import { 
  Clock, 
  Calendar, 
  Play, 
  RefreshCw, 
  AlertTriangle,
  CheckCircle,
  XCircle,
  TrendingUp,
  DollarSign
} from 'lucide-react';
import { accountServiceApi } from '../services/api';
import { isManagerOrAdmin } from '../utils/auth';
import type { BatchStatusResponse, TimeTravelStatusResponse } from '../types';

const BatchManagement: React.FC = () => {
  const [timeTravelStatus, setTimeTravelStatus] = useState<TimeTravelStatusResponse | null>(null);
  const [batchStatuses, setBatchStatuses] = useState<{
    maturity?: BatchStatusResponse;
    capitalization?: BatchStatusResponse;
    accrual?: BatchStatusResponse;
  }>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [processing, setProcessing] = useState<string | null>(null);
  
  // Time travel form
  const [targetDate, setTargetDate] = useState('');
  const [showTimeTravelModal, setShowTimeTravelModal] = useState(false);

  const hasAdminAccess = isManagerOrAdmin();

  // Redirect if not admin/manager
  useEffect(() => {
    if (!hasAdminAccess) {
      window.location.href = '/dashboard';
    }
  }, [hasAdminAccess]);

  // Fetch all statuses
  const fetchStatuses = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [ttStatus, mStatus] = await Promise.allSettled([
        accountServiceApi.getTimeTravelStatus(),
        accountServiceApi.getMaturityProcessingStatus(),
      ]);

      if (ttStatus.status === 'fulfilled') {
        setTimeTravelStatus(ttStatus.value.data);
      }

      setBatchStatuses({
        maturity: mStatus.status === 'fulfilled' ? mStatus.value.data : undefined,
        // Note: You may need separate endpoints for each batch type
        // For now using generic batch status
      });
    } catch (err: any) {
      console.error('Error fetching statuses:', err);
      setError('Failed to load batch statuses');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (hasAdminAccess) {
      fetchStatuses();
    }
  }, [hasAdminAccess]);

  // Set time travel
  const handleSetTimeTravel = async () => {
    if (!targetDate) return;
    
    try {
      setProcessing('time-travel');
      await accountServiceApi.setTimeTravel({ targetDate });
      setShowTimeTravelModal(false);
      setTargetDate('');
      await fetchStatuses();
    } catch (err: any) {
      console.error('Error setting time travel:', err);
      alert(err.response?.data?.message || 'Failed to set time travel');
    } finally {
      setProcessing(null);
    }
  };

  // Clear time travel
  const handleClearTimeTravel = async () => {
    if (!confirm('Are you sure you want to clear time travel and return to actual system date?')) {
      return;
    }
    
    try {
      setProcessing('clear-time-travel');
      await accountServiceApi.clearTimeTravel();
      await fetchStatuses();
    } catch (err: any) {
      console.error('Error clearing time travel:', err);
      alert(err.response?.data?.message || 'Failed to clear time travel');
    } finally {
      setProcessing(null);
    }
  };

  // Trigger batch
  const handleTriggerBatch = async (batchType: 'maturity' | 'capitalization' | 'accrual') => {
    const confirmMsg = `Are you sure you want to trigger ${batchType} processing? This will process all eligible accounts.`;
    if (!confirm(confirmMsg)) return;
    
    try {
      setProcessing(batchType);
      
      switch (batchType) {
        case 'maturity':
          await accountServiceApi.triggerMaturityProcessing();
          break;
        case 'capitalization':
          await accountServiceApi.triggerInterestCapitalization();
          break;
        case 'accrual':
          await accountServiceApi.triggerInterestAccrual();
          break;
      }
      
      // Wait a bit for processing to start
      setTimeout(() => {
        fetchStatuses();
      }, 2000);
    } catch (err: any) {
      console.error(`Error triggering ${batchType}:`, err);
      alert(err.response?.data?.message || `Failed to trigger ${batchType} processing`);
    } finally {
      setProcessing(null);
    }
  };

  // Format date
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Batch Management</h1>
          <p className="text-muted-foreground mt-1">
            Manage system batch processes and time travel
          </p>
        </div>
        
        <button
          onClick={fetchStatuses}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors disabled:opacity-50"
        >
          <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Time Travel Section */}
      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
              <Clock className="w-6 h-6 text-primary" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">Time Travel</h2>
              <p className="text-sm text-muted-foreground">
                Simulate system date for testing
              </p>
            </div>
          </div>
          
          {timeTravelStatus?.enabled ? (
            <div className="flex items-center gap-2 px-3 py-1 bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400 rounded-full text-sm font-semibold">
              <AlertTriangle className="w-4 h-4" />
              Active
            </div>
          ) : (
            <div className="flex items-center gap-2 px-3 py-1 bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 rounded-full text-sm font-semibold">
              <CheckCircle className="w-4 h-4" />
              Inactive
            </div>
          )}
        </div>

        {timeTravelStatus && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="bg-muted rounded-lg p-4">
              <p className="text-sm text-muted-foreground mb-1">Actual System Date</p>
              <p className="text-lg font-bold text-foreground">
                {formatDate(timeTravelStatus.actualSystemDate)}
              </p>
            </div>
            
            {timeTravelStatus.enabled && timeTravelStatus.currentSimulatedDate && (
              <div className="bg-yellow-100 dark:bg-yellow-900/30 rounded-lg p-4">
                <p className="text-sm text-yellow-800 dark:text-yellow-400 mb-1">
                  Simulated Date
                </p>
                <p className="text-lg font-bold text-yellow-900 dark:text-yellow-300">
                  {formatDate(timeTravelStatus.currentSimulatedDate)}
                </p>
              </div>
            )}
            
            <div className="bg-muted rounded-lg p-4">
              <p className="text-sm text-muted-foreground mb-1">Status</p>
              <p className="text-sm text-foreground">
                {timeTravelStatus.message}
              </p>
            </div>
          </div>
        )}

        <div className="flex gap-3">
          <button
            onClick={() => setShowTimeTravelModal(true)}
            disabled={processing !== null}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            <Calendar className="w-5 h-5" />
            {timeTravelStatus?.enabled ? 'Change Date' : 'Enable Time Travel'}
          </button>
          
          {timeTravelStatus?.enabled && (
            <button
              onClick={handleClearTimeTravel}
              disabled={processing !== null}
              className="flex items-center gap-2 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors disabled:opacity-50"
            >
              <XCircle className="w-5 h-5" />
              Clear Time Travel
            </button>
          )}
        </div>
      </div>

      {/* Batch Processes */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Maturity Processing */}
        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-blue-500/10 rounded-lg flex items-center justify-center">
              <Calendar className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <h3 className="font-bold text-foreground">Maturity Processing</h3>
          </div>
          
          <p className="text-sm text-muted-foreground mb-4">
            Process accounts that have reached maturity date
          </p>
          
          {batchStatuses.maturity && (
            <div className="space-y-2 mb-4 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Status:</span>
                <span className="font-semibold text-foreground">
                  {batchStatuses.maturity.status}
                </span>
              </div>
              {batchStatuses.maturity.lastRunTime && (
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Last Run:</span>
                  <span className="text-foreground">
                    {formatDate(batchStatuses.maturity.lastRunTime)}
                  </span>
                </div>
              )}
            </div>
          )}
          
          <button
            onClick={() => handleTriggerBatch('maturity')}
            disabled={processing !== null}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {processing === 'maturity' ? (
              <>
                <RefreshCw className="w-5 h-5 animate-spin" />
                Processing...
              </>
            ) : (
              <>
                <Play className="w-5 h-5" />
                Trigger Processing
              </>
            )}
          </button>
        </div>

        {/* Interest Capitalization */}
        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-green-500/10 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-5 h-5 text-green-600 dark:text-green-400" />
            </div>
            <h3 className="font-bold text-foreground">Interest Capitalization</h3>
          </div>
          
          <p className="text-sm text-muted-foreground mb-4">
            Add accumulated interest to principal
          </p>
          
          {batchStatuses.capitalization && (
            <div className="space-y-2 mb-4 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Status:</span>
                <span className="font-semibold text-foreground">
                  {batchStatuses.capitalization.status}
                </span>
              </div>
            </div>
          )}
          
          <button
            onClick={() => handleTriggerBatch('capitalization')}
            disabled={processing !== null}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
          >
            {processing === 'capitalization' ? (
              <>
                <RefreshCw className="w-5 h-5 animate-spin" />
                Processing...
              </>
            ) : (
              <>
                <Play className="w-5 h-5" />
                Trigger Processing
              </>
            )}
          </button>
        </div>

        {/* Interest Accrual */}
        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-purple-500/10 rounded-lg flex items-center justify-center">
              <DollarSign className="w-5 h-5 text-purple-600 dark:text-purple-400" />
            </div>
            <h3 className="font-bold text-foreground">Interest Accrual</h3>
          </div>
          
          <p className="text-sm text-muted-foreground mb-4">
            Calculate and accrue daily interest
          </p>
          
          {batchStatuses.accrual && (
            <div className="space-y-2 mb-4 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Status:</span>
                <span className="font-semibold text-foreground">
                  {batchStatuses.accrual.status}
                </span>
              </div>
            </div>
          )}
          
          <button
            onClick={() => handleTriggerBatch('accrual')}
            disabled={processing !== null}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
          >
            {processing === 'accrual' ? (
              <>
                <RefreshCw className="w-5 h-5 animate-spin" />
                Processing...
              </>
            ) : (
              <>
                <Play className="w-5 h-5" />
                Trigger Processing
              </>
            )}
          </button>
        </div>
      </div>

      {/* Warning Notice */}
      <div className="bg-yellow-100 dark:bg-yellow-900/30 border border-yellow-300 dark:border-yellow-800 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-yellow-800 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
          <div>
            <h4 className="font-semibold text-yellow-900 dark:text-yellow-300 mb-1">
              Batch Processing Warning
            </h4>
            <p className="text-sm text-yellow-800 dark:text-yellow-400">
              Batch processes can be resource-intensive and may affect system performance. 
              Ensure no other critical operations are running before triggering batch jobs.
              Time travel is for testing purposes only and should not be used in production.
            </p>
          </div>
        </div>
      </div>

      {/* Time Travel Modal */}
      {showTimeTravelModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-card border border-border rounded-lg max-w-md w-full">
            <div className="p-6 border-b border-border">
              <h2 className="text-xl font-bold text-foreground">Set Time Travel Date</h2>
              <p className="text-sm text-muted-foreground mt-1">
                Choose a date to simulate for testing purposes
              </p>
            </div>
            
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground mb-2">
                  Target Date
                </label>
                <input
                  type="datetime-local"
                  value={targetDate}
                  onChange={(e) => setTargetDate(e.target.value)}
                  className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              
              <div className="bg-yellow-100 dark:bg-yellow-900/30 border border-yellow-300 dark:border-yellow-800 rounded-lg p-3">
                <p className="text-sm text-yellow-800 dark:text-yellow-400">
                  <strong>Warning:</strong> This will affect all time-based calculations 
                  and batch processes. Use only for testing.
                </p>
              </div>
              
              <div className="flex gap-3 pt-4">
                <button
                  onClick={() => {
                    setShowTimeTravelModal(false);
                    setTargetDate('');
                  }}
                  className="flex-1 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSetTimeTravel}
                  disabled={!targetDate || processing !== null}
                  className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50"
                >
                  {processing === 'time-travel' ? 'Setting...' : 'Set Date'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BatchManagement;
