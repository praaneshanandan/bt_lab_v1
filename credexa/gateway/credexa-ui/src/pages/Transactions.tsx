import React, { useState, useEffect } from 'react';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  Search, 
  Filter, 
  Calendar,
  Download,
  RefreshCw,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle
} from 'lucide-react';
import { accountServiceApi } from '../services/api';
import { isManagerOrAdmin } from '../utils/auth';
import type { FDTransaction } from '../types';

const Transactions: React.FC = () => {
  const [transactions, setTransactions] = useState<FDTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Search and filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedTransaction, setSelectedTransaction] = useState<FDTransaction | null>(null);
  
  const hasAdminAccess = isManagerOrAdmin();

  // Fetch transactions
  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // First, get all FD accounts
      const accountsResponse = await accountServiceApi.getAllAccounts();
      const accounts = accountsResponse.data?.data?.content || accountsResponse.data?.content || [];
      
      if (accounts.length === 0) {
        setTransactions([]);
        setLoading(false);
        return;
      }
      
      // Fetch transactions for each account and combine them
      const allTransactions: FDTransaction[] = [];
      
      for (const account of accounts) {
        try {
          const txnResponse = await accountServiceApi.getTransactionsByAccount(
            account.accountNumber, 
            0, 
            100
          );
          const txns = txnResponse.data?.data?.content || txnResponse.data?.content || [];
          allTransactions.push(...txns);
        } catch (err) {
          console.warn(`Failed to fetch transactions for account ${account.accountNumber}:`, err);
          // Continue with other accounts
        }
      }
      
      setTransactions(allTransactions);
    } catch (err: unknown) {
      console.error('Error fetching transactions:', err);
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [hasAdminAccess]);

  // Filter transactions
  const filteredTransactions = transactions.filter(txn => {
    const matchesSearch = 
      txn.transactionId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      txn.accountNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (txn.referenceNumber && txn.referenceNumber.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesType = typeFilter === 'ALL' || txn.transactionType === typeFilter;
    const matchesStatus = statusFilter === 'ALL' || txn.status === statusFilter;
    
    return matchesSearch && matchesType && matchesStatus;
  });

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
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Get transaction type icon
  const getTransactionIcon = (type: string) => {
    switch (type) {
      case 'DEPOSIT':
      case 'INTEREST_CREDIT':
      case 'MATURITY_CREDIT':
        return <ArrowDownLeft className="w-5 h-5 text-green-600 dark:text-green-400" />;
      case 'TDS_DEDUCTION':
      case 'WITHDRAWAL':
      case 'CLOSURE':
        return <ArrowUpRight className="w-5 h-5 text-red-600 dark:text-red-400" />;
      case 'REVERSAL':
      case 'ADJUSTMENT':
        return <RefreshCw className="w-5 h-5 text-blue-600 dark:text-blue-400" />;
      default:
        return <RefreshCw className="w-5 h-5 text-muted-foreground" />;
    }
  };

  // Get status badge
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
            <CheckCircle className="w-3 h-3" />
            Completed
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
            <Clock className="w-3 h-3" />
            Pending
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400">
            <XCircle className="w-3 h-3" />
            Failed
          </span>
        );
      case 'REVERSED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400">
            <RefreshCw className="w-3 h-3" />
            Reversed
          </span>
        );
      default:
        return <span className="text-xs text-muted-foreground">{status}</span>;
    }
  };

  // Get transaction type label
  const getTypeLabel = (type: string) => {
    return type.split('_').map(word => 
      word.charAt(0) + word.slice(1).toLowerCase()
    ).join(' ');
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
          <h1 className="text-3xl font-bold text-foreground">Transactions</h1>
          <p className="text-muted-foreground mt-1">
            View all FD account transactions
          </p>
        </div>
        
        <button
          onClick={fetchTransactions}
          className="flex items-center gap-2 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors"
        >
          <RefreshCw className="w-5 h-5" />
          Refresh
        </button>
      </div>

      {/* Search and Filters */}
      <div className="bg-card border border-border rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search by reference or account..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          {/* Type Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring appearance-none"
            >
              <option value="ALL">All Types</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="INTEREST_CREDIT">Interest Credit</option>
              <option value="TDS_DEDUCTION">TDS Deduction</option>
              <option value="WITHDRAWAL">Withdrawal</option>
              <option value="MATURITY_CREDIT">Maturity Credit</option>
              <option value="CLOSURE">Closure</option>
              <option value="REVERSAL">Reversal</option>
              <option value="ADJUSTMENT">Adjustment</option>
            </select>
          </div>

          {/* Status Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring appearance-none"
            >
              <option value="ALL">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="REJECTED">Rejected</option>
              <option value="REVERSED">Reversed</option>
            </select>
          </div>

          {/* Export Button */}
          <button className="flex items-center justify-center gap-2 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors">
            <Download className="w-5 h-5" />
            Export
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Total Transactions</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredTransactions.length}
              </p>
            </div>
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
              <RefreshCw className="w-6 h-6 text-primary" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Completed</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredTransactions.filter(t => t.status === 'COMPLETED').length}
              </p>
            </div>
            <div className="w-12 h-12 bg-green-500/10 rounded-lg flex items-center justify-center">
              <CheckCircle className="w-6 h-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Pending</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredTransactions.filter(t => t.status === 'PENDING').length}
              </p>
            </div>
            <div className="w-12 h-12 bg-yellow-500/10 rounded-lg flex items-center justify-center">
              <Clock className="w-6 h-6 text-yellow-600 dark:text-yellow-400" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Failed</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredTransactions.filter(t => t.status === 'FAILED').length}
              </p>
            </div>
            <div className="w-12 h-12 bg-red-500/10 rounded-lg flex items-center justify-center">
              <XCircle className="w-6 h-6 text-red-600 dark:text-red-400" />
            </div>
          </div>
        </div>
      </div>

      {/* Transactions List */}
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted border-b border-border">
              <tr>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Type
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Reference
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Account Number
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Amount
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Date
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Status
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-muted-foreground">
                    <Calendar className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>No transactions found</p>
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((txn) => (
                  <tr 
                    key={txn.id} 
                    className="hover:bg-muted/50 transition-colors"
                  >
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        {getTransactionIcon(txn.transactionType)}
                        <span className="text-sm text-foreground">
                          {getTypeLabel(txn.transactionType)}
                        </span>
                      </div>
                    </td>
                    <td className="p-4">
                      <span className="font-mono text-xs text-foreground">
                        {txn.transactionId}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="font-mono text-sm text-foreground">
                        {txn.accountNumber}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm font-semibold text-foreground">
                        {formatCurrency(txn.amount)}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm text-foreground">
                        {formatDate(txn.transactionDate)}
                      </span>
                    </td>
                    <td className="p-4">
                      {getStatusBadge(txn.status)}
                    </td>
                    <td className="p-4">
                      <button
                        onClick={() => setSelectedTransaction(txn)}
                        className="text-sm text-primary hover:text-primary/80 transition-colors"
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Transaction Details Modal */}
      {selectedTransaction && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-card border border-border rounded-lg max-w-lg w-full">
            <div className="p-6 border-b border-border flex items-center justify-between">
              <h2 className="text-xl font-bold text-foreground">Transaction Details</h2>
              <button
                onClick={() => setSelectedTransaction(null)}
                className="text-muted-foreground hover:text-foreground"
              >
                ✕
              </button>
            </div>
            
            <div className="p-6 space-y-4">
              <div className="flex items-center gap-3 pb-4 border-b border-border">
                {getTransactionIcon(selectedTransaction.transactionType)}
                <div>
                  <p className="font-semibold text-foreground">
                    {getTypeLabel(selectedTransaction.transactionType)}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {formatDate(selectedTransaction.transactionDate)}
                  </p>
                </div>
                <div className="ml-auto">
                  {getStatusBadge(selectedTransaction.status)}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">Transaction ID</p>
                  <p className="font-mono text-xs text-foreground font-semibold mt-1">
                    {selectedTransaction.transactionId}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Reference</p>
                  <p className="font-mono text-xs text-foreground font-semibold mt-1">
                    {selectedTransaction.referenceNumber || 'N/A'}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Account Number</p>
                  <p className="font-mono text-sm text-foreground font-semibold mt-1">
                    {selectedTransaction.accountNumber}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Amount</p>
                  <p className="text-xl font-bold text-primary mt-1">
                    {formatCurrency(selectedTransaction.amount)}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">Balance Before</p>
                  <p className="text-foreground font-semibold mt-1">
                    {formatCurrency(selectedTransaction.balanceBefore)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Balance After</p>
                  <p className="text-foreground font-semibold mt-1">
                    {formatCurrency(selectedTransaction.balanceAfter)}
                  </p>
                </div>
              </div>

              {selectedTransaction.description && (
                <div>
                  <p className="text-sm text-muted-foreground">Description</p>
                  <p className="text-foreground mt-1">{selectedTransaction.description}</p>
                </div>
              )}

              {selectedTransaction.remarks && (
                <div>
                  <p className="text-sm text-muted-foreground">Remarks</p>
                  <p className="text-foreground mt-1">{selectedTransaction.remarks}</p>
                </div>
              )}

              {(selectedTransaction.initiatedBy || selectedTransaction.approvedBy) && (
                <div className="grid grid-cols-2 gap-4">
                  {selectedTransaction.initiatedBy && (
                    <div>
                      <p className="text-sm text-muted-foreground">Initiated By</p>
                      <p className="text-foreground mt-1">{selectedTransaction.initiatedBy}</p>
                    </div>
                  )}
                  {selectedTransaction.approvedBy && (
                    <div>
                      <p className="text-sm text-muted-foreground">Approved By</p>
                      <p className="text-foreground mt-1">{selectedTransaction.approvedBy}</p>
                    </div>
                  )}
                </div>
              )}

              <div className="grid grid-cols-2 gap-4 text-xs text-muted-foreground pt-4 border-t border-border">
                {selectedTransaction.valueDate && (
                  <div>
                    <p>Value Date</p>
                    <p className="text-foreground mt-1">
                      {formatDate(selectedTransaction.valueDate)}
                    </p>
                  </div>
                )}
                {selectedTransaction.approvalDate && (
                  <div>
                    <p>Approval Date</p>
                    <p className="text-foreground mt-1">
                      {formatDate(selectedTransaction.approvalDate)}
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Transactions;
