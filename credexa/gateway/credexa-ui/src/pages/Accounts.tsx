import React, { useState, useEffect } from 'react';
import { 
  User, 
  DollarSign, 
  Calendar, 
  TrendingUp, 
  Info, 
  Plus,
  Search,
  Filter,
  Download,
  Eye,
  AlertCircle
} from 'lucide-react';
import { accountServiceApi, customerApi } from '../services/api';
import { isManagerOrAdmin, getUserRoles, decodeToken } from '../utils/auth';
import type { FDAccount, AccountBalanceResponse, CreateDefaultAccountRequest, CreateCustomAccountRequest } from '../types';

const Accounts: React.FC = () => {
  // Debug: Log user roles
  React.useEffect(() => {
    const token = localStorage.getItem('authToken');
    const roles = getUserRoles();
    console.log('🔍 DEBUG - User Roles:', roles);
    console.log('🔍 DEBUG - Has Admin Access:', isManagerOrAdmin());
    if (token) {
      const decoded = decodeToken(token);
      console.log('🔍 DEBUG - Full JWT Token:', decoded);
    }
  }, []);
  const [accounts, setAccounts] = useState<FDAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAccount, setSelectedAccount] = useState<FDAccount | null>(null);
  const [accountBalance, setAccountBalance] = useState<AccountBalanceResponse | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createMode] = useState<'default' | 'custom'>('default');
  
  // Search and filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  
  const hasAdminAccess = isManagerOrAdmin();
  const customerId = localStorage.getItem('customerId');

  // Fetch accounts based on role
  const fetchAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      
      let response;
      if (hasAdminAccess) {
        // Admin/Manager can see all accounts
        response = await accountServiceApi.getAllAccounts();
      } else {
        // Customer can only see their own accounts
        // First, get customer's profile to get their customer ID
        let customerIdToUse = customerId;
        
        if (!customerIdToUse) {
          try {
            const profileResponse = await customerApi.getOwnProfile();
            console.log('🔍 DEBUG - Profile Response:', profileResponse);
            console.log('🔍 DEBUG - Profile Data:', profileResponse.data);
            console.log('🔍 DEBUG - Profile Data.data:', profileResponse.data?.data);
            
            // Try multiple possible paths for customer ID
            customerIdToUse = 
              profileResponse.data?.data?.id?.toString() ||
              profileResponse.data?.data?.customerId?.toString() ||
              profileResponse.data?.id?.toString() ||
              profileResponse.data?.customerId?.toString();
            
            console.log('🔍 DEBUG - Extracted Customer ID:', customerIdToUse);
            
            if (customerIdToUse) {
              // Save it to localStorage for future use
              localStorage.setItem('customerId', customerIdToUse);
            }
          } catch (profileErr) {
            console.error('Error fetching customer profile:', profileErr);
            throw new Error('Unable to fetch your profile. Please ensure you have a customer profile created.');
          }
        }
        
        if (!customerIdToUse) {
          throw new Error('Customer ID not found in your profile. Please contact support.');
        }
        
        response = await accountServiceApi.getAccountsByCustomerId(parseInt(customerIdToUse));
      }
      
      // Extract accounts array from paginated response: response.data.data.content
      const accountsData = response.data?.data?.content || response.data?.data || [];
      if (accountsData.length > 0) {
        console.log('🔍 DEBUG - Sample account object:', accountsData[0]);
        console.log('🔍 DEBUG - Account keys:', Object.keys(accountsData[0]));
      }
      setAccounts(Array.isArray(accountsData) ? accountsData : []);
    } catch (err: any) {
      console.error('Error fetching accounts:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, [hasAdminAccess, customerId]);

  // Fetch account balance
  const fetchAccountBalance = async (accountNumber: string) => {
    try {
      const response = await accountServiceApi.getAccountBalance(accountNumber);
      setAccountBalance(response.data);
    } catch (err: any) {
      console.error('Error fetching balance:', err);
    }
  };

  // View account details
  const viewAccountDetails = async (account: FDAccount) => {
    setSelectedAccount(account);
    await fetchAccountBalance(account.accountNumber);
  };

  // Filter accounts
  const filteredAccounts = accounts.filter(account => {
    const matchesSearch = 
      account.accountNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      account.productCode.toLowerCase().includes(searchTerm.toLowerCase());
    
    // Check both accountStatus and status fields, default to 'ACTIVE' if not set
    const accountStatusValue = account.accountStatus || (account as any).status || 'ACTIVE';
    const matchesStatus = statusFilter === 'ALL' || accountStatusValue === statusFilter;
    
    return matchesSearch && matchesStatus;
  });

  // Format currency
  const formatCurrency = (amount: number | null | undefined) => {
    if (amount === null || amount === undefined || isNaN(amount)) {
      return '₹0.00';
    }
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
    });
  };

  // Get status badge color
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case 'MATURED':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case 'CLOSED':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400';
      case 'SUSPENDED':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400';
    }
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
          <h1 className="text-3xl font-bold text-foreground">FD Accounts</h1>
          <p className="text-muted-foreground mt-1">
            Manage your Fixed Deposit accounts
          </p>
        </div>
        
        {/* Create Account Button - Only for MANAGER/ADMIN */}
        {hasAdminAccess && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
          >
            <Plus className="w-5 h-5" />
            Create Account
          </button>
        )}
      </div>

      {/* Search and Filters */}
      <div className="bg-card border border-border rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search by account number or product..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
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
              <option value="ACTIVE">Active</option>
              <option value="MATURED">Matured</option>
              <option value="CLOSED">Closed</option>
              <option value="SUSPENDED">Suspended</option>
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
              <p className="text-sm text-muted-foreground">Total Accounts</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredAccounts.length}
              </p>
            </div>
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
              <User className="w-6 h-6 text-primary" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Active Accounts</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredAccounts.filter(a => {
                  const status = a.accountStatus || (a as any).status || 'ACTIVE';
                  return status === 'ACTIVE';
                }).length}
              </p>
            </div>
            <div className="w-12 h-12 bg-green-500/10 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Total Principal</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {formatCurrency(
                  filteredAccounts.reduce((sum, acc) => sum + acc.principalAmount, 0)
                )}
              </p>
            </div>
            <div className="w-12 h-12 bg-blue-500/10 rounded-lg flex items-center justify-center">
              <DollarSign className="w-6 h-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </div>

        <div className="bg-card border border-border rounded-lg p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Matured</p>
              <p className="text-2xl font-bold text-foreground mt-1">
                {filteredAccounts.filter(a => a.accountStatus === 'MATURED').length}
              </p>
            </div>
            <div className="w-12 h-12 bg-yellow-500/10 rounded-lg flex items-center justify-center">
              <Calendar className="w-6 h-6 text-yellow-600 dark:text-yellow-400" />
            </div>
          </div>
        </div>
      </div>

      {/* Accounts Table */}
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted border-b border-border">
              <tr>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Account Number
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Customer ID
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Product
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Principal
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Interest Rate
                </th>
                <th className="text-left p-4 text-sm font-semibold text-muted-foreground">
                  Maturity Date
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
              {filteredAccounts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-muted-foreground">
                    <Info className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>No accounts found</p>
                  </td>
                </tr>
              ) : (
                filteredAccounts.map((account) => (
                  <tr 
                    key={account.accountNumber} 
                    className="hover:bg-muted/50 transition-colors"
                  >
                    <td className="p-4">
                      <span className="font-mono text-sm text-foreground">
                        {account.accountNumber}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm font-medium text-primary">
                        {account.customerId}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm text-foreground">{account.productCode}</span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm font-semibold text-foreground">
                        {formatCurrency(account.principalAmount)}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm text-foreground">
                        {account.interestRate}% p.a.
                      </span>
                    </td>
                    <td className="p-4">
                      <span className="text-sm text-foreground">
                        {formatDate(account.maturityDate)}
                      </span>
                    </td>
                    <td className="p-4">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(account.accountStatus || (account as any).status || 'ACTIVE')}`}>
                        {account.accountStatus || (account as any).status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="p-4">
                      <button
                        onClick={() => viewAccountDetails(account)}
                        className="inline-flex items-center gap-1 px-3 py-1 text-sm text-primary hover:text-primary/80 transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                        View
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Account Details Modal */}
      {selectedAccount && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-card border border-border rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-border flex items-center justify-between">
              <h2 className="text-xl font-bold text-foreground">Account Details</h2>
              <button
                onClick={() => {
                  setSelectedAccount(null);
                  setAccountBalance(null);
                }}
                className="text-muted-foreground hover:text-foreground"
              >
                ✕
              </button>
            </div>
            
            <div className="p-6 space-y-6">
              {/* Balance Summary */}
              {accountBalance && (
                <div className="bg-primary/10 border border-primary/30 rounded-lg p-6">
                  <h3 className="text-sm font-semibold text-muted-foreground mb-4">
                    CURRENT BALANCE
                  </h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-muted-foreground">Principal</p>
                      <p className="text-xl font-bold text-foreground mt-1">
                        {formatCurrency(accountBalance.principalAmount)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Accrued Interest</p>
                      <p className="text-xl font-bold text-green-600 dark:text-green-400 mt-1">
                        {formatCurrency(accountBalance.accruedInterest)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Total Balance</p>
                      <p className="text-2xl font-bold text-primary mt-1">
                        {formatCurrency(accountBalance.totalBalance)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Maturity Amount</p>
                      <p className="text-xl font-bold text-foreground mt-1">
                        {formatCurrency(accountBalance.maturityAmount)}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Account Information */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">Account Number</p>
                  <p className="font-mono text-foreground font-semibold mt-1">
                    {selectedAccount.accountNumber}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Product Code</p>
                  <p className="text-foreground font-semibold mt-1">
                    {selectedAccount.productCode}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Interest Rate</p>
                  <p className="text-foreground font-semibold mt-1">
                    {selectedAccount.interestRate}% per annum
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Tenure</p>
                  <p className="text-foreground font-semibold mt-1">
                    {selectedAccount.tenure} {selectedAccount.tenureUnit}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Opening Date</p>
                  <p className="text-foreground font-semibold mt-1">
                    {formatDate(selectedAccount.openingDate)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Maturity Date</p>
                  <p className="text-foreground font-semibold mt-1">
                    {formatDate(selectedAccount.maturityDate)}
                  </p>
                </div>
                {selectedAccount.lastInterestCreditDate && (
                  <div>
                    <p className="text-sm text-muted-foreground">Last Interest Credit</p>
                    <p className="text-foreground font-semibold mt-1">
                      {formatDate(selectedAccount.lastInterestCreditDate)}
                    </p>
                  </div>
                )}
                <div>
                  <p className="text-sm text-muted-foreground">Status</p>
                  <span className={`inline-flex px-3 py-1 text-sm font-semibold rounded-full mt-1 ${getStatusColor(selectedAccount.accountStatus || (selectedAccount as any).status || 'ACTIVE')}`}>
                    {selectedAccount.accountStatus || (selectedAccount as any).status || 'ACTIVE'}
                  </span>
                </div>
              </div>

              {/* Nominee Details */}
              {selectedAccount.nomineeDetails && (
                <div>
                  <p className="text-sm text-muted-foreground">Nominee Details</p>
                  <p className="text-foreground mt-1">{selectedAccount.nomineeDetails}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Create Account Modal */}
      {showCreateModal && (
        <CreateAccountModal
          mode={createMode}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            fetchAccounts();
          }}
        />
      )}
    </div>
  );
};

// Create Account Modal Component
const CreateAccountModal: React.FC<{
  mode: 'default' | 'custom';
  onClose: () => void;
  onSuccess: () => void;
}> = ({ mode, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Get customer ID from localStorage, default to empty string for user to fill
  const storedCustomerId = localStorage.getItem('customerId');
  const customerIdValue = storedCustomerId && storedCustomerId !== '0' ? storedCustomerId : '';
  
  const [formData, setFormData] = useState<any>({
    accountName: '',
    customerId: customerIdValue,
    productCode: '',
    principalAmount: '',
    termMonths: '',
    effectiveDate: new Date().toISOString().split('T')[0], // Today's date in YYYY-MM-DD format
    interestRate: '',
    branchCode: '',
    branchName: '',
    remarks: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    console.log('🚀 Form submitted, starting account creation...');
    setLoading(true);
    setError(null);

    try {
      if (mode === 'default') {
        const data: CreateDefaultAccountRequest = {
          accountName: formData.accountName,
          customerId: parseInt(formData.customerId) || 0,
          productCode: formData.productCode,
          principalAmount: parseFloat(formData.principalAmount),
          termMonths: parseInt(formData.termMonths),
          effectiveDate: formData.effectiveDate,
          branchCode: formData.branchCode || undefined,
          branchName: formData.branchName || undefined,
          remarks: formData.remarks || undefined,
        };
        console.log('📤 Sending DEFAULT account creation request:', data);
        const response = await accountServiceApi.createDefaultAccount(data);
        console.log('✅ DEFAULT Account created successfully!', response);
        console.log('✅ Response data:', response.data);
      } else {
        const data: CreateCustomAccountRequest = {
          accountName: formData.accountName,
          customerId: parseInt(formData.customerId) || 0,
          productCode: formData.productCode,
          principalAmount: parseFloat(formData.principalAmount),
          termMonths: parseInt(formData.termMonths),
          effectiveDate: formData.effectiveDate,
          interestRate: parseFloat(formData.interestRate),
          branchCode: formData.branchCode || undefined,
          branchName: formData.branchName || undefined,
          remarks: formData.remarks || undefined,
        };
        console.log('📤 Sending account creation request:', data);
        const response = await accountServiceApi.createCustomAccount(data);
        console.log('✅ Account created successfully!', response.data);
      }

      console.log('🎉 Closing modal and refreshing list...');
      onSuccess();
    } catch (err: any) {
      console.error('Error creating account:', err);
      console.error('📛 Full error response:', err.response);
      const errorMessage = err.response?.data?.message || err.response?.data?.error || 'Failed to create account';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-card border border-border rounded-lg max-w-md w-full">
        <div className="p-6 border-b border-border">
          <h2 className="text-xl font-bold text-foreground">
            Create {mode === 'default' ? 'Default' : 'Custom'} Account
          </h2>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-lg p-3 text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Account Name *
            </label>
            <input
              type="text"
              required
              value={formData.accountName}
              onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
              placeholder="e.g., John Doe FD Account"
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Customer ID *
            </label>
            <input
              type="number"
              required
              min="1"
              value={formData.customerId}
              onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
              placeholder="Enter customer ID"
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Product Code *
            </label>
            <input
              type="text"
              required
              value={formData.productCode}
              onChange={(e) => setFormData({ ...formData, productCode: e.target.value })}
              placeholder="e.g., FD-STD-001"
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Principal Amount * (min ₹1000)
            </label>
            <input
              type="number"
              required
              min="1000"
              step="0.01"
              value={formData.principalAmount}
              onChange={(e) => setFormData({ ...formData, principalAmount: e.target.value })}
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Term (Months) *
            </label>
            <input
              type="number"
              required
              min="1"
              value={formData.termMonths}
              onChange={(e) => setFormData({ ...formData, termMonths: e.target.value })}
              placeholder="e.g., 12"
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Effective Date *
            </label>
            <input
              type="date"
              required
              value={formData.effectiveDate}
              onChange={(e) => setFormData({ ...formData, effectiveDate: e.target.value })}
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          {mode === 'custom' && (
            <>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1">
                  Interest Rate (%)
                </label>
                <input
                  type="number"
                  required
                  min="0.1"
                  step="0.1"
                  value={formData.interestRate}
                  onChange={(e) => setFormData({ ...formData, interestRate: e.target.value })}
                  className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1">
                    Tenure
                  </label>
                  <input
                    type="number"
                    required
                    min="1"
                    value={formData.tenure}
                    onChange={(e) => setFormData({ ...formData, tenure: e.target.value })}
                    className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1">
                    Unit
                  </label>
                  <select
                    value={formData.tenureUnit}
                    onChange={(e) => setFormData({ ...formData, tenureUnit: e.target.value })}
                    className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
                  >
                    <option value="DAYS">Days</option>
                    <option value="MONTHS">Months</option>
                    <option value="YEARS">Years</option>
                  </select>
                </div>
              </div>
            </>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1">
                Branch Code (Optional)
              </label>
              <input
                type="text"
                value={formData.branchCode}
                onChange={(e) => setFormData({ ...formData, branchCode: e.target.value })}
                placeholder="e.g., BR001"
                className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1">
                Branch Name (Optional)
              </label>
              <input
                type="text"
                value={formData.branchName}
                onChange={(e) => setFormData({ ...formData, branchName: e.target.value })}
                placeholder="e.g., Main Branch"
                className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Remarks (Optional)
            </label>
            <textarea
              value={formData.remarks}
              onChange={(e) => setFormData({ ...formData, remarks: e.target.value })}
              placeholder="Additional notes or comments"
              className="w-full px-3 py-2 border border-input bg-background text-foreground rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              rows={2}
            />
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-border rounded-lg hover:bg-muted transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Accounts;
