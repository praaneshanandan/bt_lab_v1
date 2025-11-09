import axios from 'axios';
import type {
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CreateStandardAccountRequest,
  CustomizeAccountRequest,
  SearchAccountParams,
  CreateTransactionRequest,
  PrematureWithdrawalRequest,
  AddRoleRequest,
  UpdateRoleRequest,
  LoginRequest,
  RegisterRequest,
  AdminCreateCustomerRequest,
} from '../types';

// All requests now go through the Gateway (port 8080)
// Gateway routes to backend services based on path prefixes
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

// Create axios instance for Login Service (via Gateway /api/auth/*)
const loginApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Customer Service (via Gateway /api/customer/*)
const customerApiInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for FD Account Service (via Gateway /api/fd-accounts/*)
const accountApiInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Product Pricing Service (via Gateway /api/products/*)
const productApiInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Calculator Service (via Gateway /api/calculator/*)
const calculatorApiInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for NEW Account Service (via Gateway /api/accounts/*)
const newAccountApiInstance = axios.create({
  baseURL: API_BASE_URL + '/api/accounts',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token interceptor to all instances
const addAuthInterceptor = (instance: any) => {
  instance.interceptors.request.use((config: any) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });
};

addAuthInterceptor(loginApi);
addAuthInterceptor(customerApiInstance);
addAuthInterceptor(accountApiInstance);
addAuthInterceptor(productApiInstance);
addAuthInterceptor(calculatorApiInstance);
addAuthInterceptor(newAccountApiInstance);

// Auth/Login Service APIs (context-path is /api/auth)
export const authApi = {
  login: (data: LoginRequest) => loginApi.post('/api/auth/login', data),
  register: (data: RegisterRequest) => loginApi.post('/api/auth/register', data),
  logout: () => loginApi.post('/api/auth/logout'),
  validateToken: (token: string) => loginApi.post('/api/auth/validate-token', token),
  getBankConfig: () => loginApi.get('/api/auth/bank-config'),
  getUserByUsername: (username: string) => loginApi.get(`/api/auth/user/${username}`),
  adminCreateCustomer: (data: AdminCreateCustomerRequest) => loginApi.post('/api/auth/admin/create-customer', data),
  health: () => loginApi.get('/api/auth/health'),
};

// Customer Service APIs
export const customerApi = {
  getCustomer: (id: number) => customerApiInstance.get(`/api/customer/${id}`),
  getAllCustomers: () => customerApiInstance.get('/api/customer/all'),
  getOwnProfile: () => customerApiInstance.get('/api/customer/profile'),
  createCustomer: (data: CreateCustomerRequest) => customerApiInstance.post('/api/customer', data),
  updateCustomer: (id: number, data: UpdateCustomerRequest) => customerApiInstance.put(`/api/customer/${id}`, data),
  getCustomerByUserId: (userId: number) => customerApiInstance.get(`/api/customer/user/${userId}`),
  getCustomerClassification: (id: number) => customerApiInstance.get(`/api/customer/${id}/classification`),
  getCustomer360View: (id: number) => customerApiInstance.get(`/api/customer/${id}/360-view`),
};

// Product Pricing Service APIs
export const productApi = {
  // Product queries
  getAllProducts: () => productApiInstance.get('/api/products'),
  getActiveProducts: () => productApiInstance.get('/api/products/active'),
  getCurrentlyActiveProducts: () => productApiInstance.get('/api/products/currently-active'),
  getProductByCode: (code: string) => productApiInstance.get(`/api/products/code/${code}`),
  getProductById: (id: number) => productApiInstance.get(`/api/products/${id}`),
  getProductsByType: (type: string) => productApiInstance.get(`/api/products/type/${type}`),
  getProductsByStatus: (status: string) => productApiInstance.get(`/api/products/status/${status}`),
  searchProducts: (searchParams: any) => productApiInstance.post('/api/products/search', searchParams),
  
  // Admin operations
  createProduct: (data: any) => productApiInstance.post('/api/products', data),
  updateProduct: (id: number, data: any) => productApiInstance.put(`/api/products/${id}`, data),
  updateProductStatus: (id: number, status: string) => productApiInstance.put(`/api/products/${id}/status?status=${status}`),
  deleteProduct: (id: number) => productApiInstance.delete(`/api/products/${id}`),
  hardDeleteProduct: (id: number) => productApiInstance.delete(`/api/products/${id}/hard`),
  
  // Interest rates
  getProductInterestRates: (id: number) => productApiInstance.get(`/api/products/${id}/interest-rates`),
  getActiveInterestRates: (id: number) => productApiInstance.get(`/api/products/${id}/interest-rates/active`),
  
  // Product charges
  getProductCharges: (id: number) => productApiInstance.get(`/api/products/${id}/charges`),
  getProductChargesByType: (id: number, chargeType: string) => productApiInstance.get(`/api/products/${id}/charges/type/${chargeType}`),
  getChargeById: (chargeId: number) => productApiInstance.get(`/api/products/charges/${chargeId}`),
  addProductCharge: (productId: number, data: any) => productApiInstance.post(`/api/products/${productId}/charges`, data),
  updateProductCharge: (chargeId: number, data: any) => productApiInstance.put(`/api/products/charges/${chargeId}`, data),
  deleteProductCharge: (chargeId: number) => productApiInstance.delete(`/api/products/charges/${chargeId}`),
  
  // Product roles
  getProductRoles: (id: number) => productApiInstance.get(`/api/products/${id}/roles`),
  getProductRolesByType: (id: number, roleType: string) => productApiInstance.get(`/api/products/${id}/roles/type/${roleType}`),
  getRoleById: (roleId: number) => productApiInstance.get(`/api/products/roles/${roleId}`),
  addProductRole: (productId: number, data: any) => productApiInstance.post(`/api/products/${productId}/roles`, data),
  updateProductRole: (roleId: number, data: any) => productApiInstance.put(`/api/products/roles/${roleId}`, data),
  deleteProductRole: (roleId: number) => productApiInstance.delete(`/api/products/roles/${roleId}`),
  
  // Interest rate calculations
  getApplicableInterestRate: (productId: number, amount: number, termInMonths: number, classification: string) => 
    productApiInstance.get(`/api/products/${productId}/interest-rates/applicable?amount=${amount}&termInMonths=${termInMonths}&customerClassification=${classification}`),
  calculateEffectiveRate: (productId: number, amount: number, termInMonths: number, classification: string) =>
    productApiInstance.get(`/api/products/${productId}/interest-rates/calculate?amount=${amount}&termInMonths=${termInMonths}&customerClassification=${classification}`),
  
  // Customer communications
  getProductCommunications: (productId: number) => productApiInstance.get(`/api/products/${productId}/communications`),
  getCommunicationsByType: (productId: number, type: string) => productApiInstance.get(`/api/products/${productId}/communications/type/${type}`),
  getCommunicationsByEvent: (productId: number, event: string) => productApiInstance.get(`/api/products/${productId}/communications/event/${event}`),
  getCommunicationById: (id: number) => productApiInstance.get(`/api/products/communications/${id}`),
  addProductCommunication: (productId: number, data: any) => productApiInstance.post(`/api/products/${productId}/communications`, data),
  updateProductCommunication: (id: number, data: any) => productApiInstance.put(`/api/products/communications/${id}`, data),
  deleteProductCommunication: (id: number) => productApiInstance.delete(`/api/products/communications/${id}`),
};

// FD Calculator Service APIs
export const calculatorApi = {
  // Standalone calculation (manual inputs)
  calculateStandalone: (data: {
    principalAmount: number;
    interestRate: number;
    tenure: number;
    tenureUnit: string;
    calculationType: string;
    compoundingFrequency?: string;
    tdsRate?: number;
    customerClassifications?: string[];
  }) => calculatorApiInstance.post('/api/calculator/calculate/standalone', data),
  
  // Product-based calculation
  calculateWithProduct: (data: {
    productId: number;
    principalAmount: number;
    tenure: number;
    tenureUnit: string;
    calculationType?: string;
    compoundingFrequency?: string;
    customInterestRate?: number;
    customerId?: number;
    customerClassifications?: string[];
    applyTds?: boolean;
  }) => calculatorApiInstance.post('/api/calculator/calculate/product-based', data),
  
  // Lab L6 endpoint - Calculate FD with auto-fetched user categories
  calculateFD: (data: {
    principalAmount: number;
    interestRate: number;
    tenure: number;
    tenureUnit: string;
    calculationType: string;
    compoundingFrequency?: string;
    tdsRate?: number;
    customerClassifications?: string[];
  }) => calculatorApiInstance.post('/api/calculator/fd/calculate', data),
  
  // Scenario comparison
  compareScenarios: (data: {
    commonPrincipal?: number;
    scenarios: Array<{
      principalAmount: number;
      interestRate: number;
      tenure: number;
      tenureUnit: string;
      calculationType: string;
      compoundingFrequency?: string;
      tdsRate?: number;
      customerClassifications?: string[];
    }>;
  }) => calculatorApiInstance.post('/api/calculator/compare', data),
  
  // Health check
  health: () => calculatorApiInstance.get('/api/calculator/health'),
};

// FD Account Service APIs
export const fdAccountApi = {
  // Account Creation
  createStandardAccount: (data: CreateStandardAccountRequest) =>
    accountApiInstance.post('/api/fd-accounts/accounts/create/standard', data),

  customizeAccount: (data: CustomizeAccountRequest) =>
    accountApiInstance.post('/api/fd-accounts/accounts/create/customize', data),

  // Account Inquiry
  getAccount: (accountNumber: string) => accountApiInstance.get(`/api/fd-accounts/accounts/${accountNumber}`),
  getAccountSummary: (accountNumber: string) => accountApiInstance.get(`/api/fd-accounts/accounts/${accountNumber}/summary`),
  getAccountsByCustomer: (customerId: number) => accountApiInstance.get(`/api/fd-accounts/accounts/customer/${customerId}`),
  getAccountsByProduct: (productCode: string) => accountApiInstance.get(`/api/fd-accounts/accounts/product/${productCode}`),
  getAccountsByBranch: (branchCode: string) => accountApiInstance.get(`/api/fd-accounts/accounts/branch/${branchCode}`),
  getAccountsMaturingInDays: (days: number) => accountApiInstance.get(`/api/fd-accounts/accounts/maturing?days=${days}`),
  checkAccountExists: (accountNumber: string) => accountApiInstance.get(`/api/fd-accounts/accounts/exists/${accountNumber}`),

  // Account Search
  searchAccounts: (searchParams: SearchAccountParams) => accountApiInstance.post('/api/fd-accounts/accounts/search', searchParams),

  // Transactions
  createTransaction: (data: CreateTransactionRequest) =>
    accountApiInstance.post('/api/fd-accounts/transactions', data),
  
  getTransactionsByAccount: (accountNumber: string) => 
    accountApiInstance.get(`/api/fd-accounts/transactions/account/${accountNumber}`),
  
  reverseTransaction: (transactionReference: string) => 
    accountApiInstance.post(`/api/fd-accounts/transactions/${transactionReference}/reverse`),

  // Premature Withdrawal
  inquirePrematureWithdrawal: (accountNumber: string) =>
    accountApiInstance.post('/api/fd-accounts/transactions/premature-withdrawal/inquire', { accountNumber }),
  
  processPrematureWithdrawal: (data: PrematureWithdrawalRequest) =>
    accountApiInstance.post('/api/fd-accounts/transactions/premature-withdrawal/process', data),

  // Roles Management
  addRole: (accountNumber: string, data: AddRoleRequest) =>
    accountApiInstance.post(`/api/fd-accounts/roles/account/${accountNumber}`, data),

  getRolesByAccount: (accountNumber: string) => 
    accountApiInstance.get(`/api/fd-accounts/roles/account/${accountNumber}`),
  
  getActiveRolesByAccount: (accountNumber: string) =>
    accountApiInstance.get(`/api/fd-accounts/roles/account/${accountNumber}/active`),
  
  getRolesByCustomer: (customerId: number) =>
    accountApiInstance.get(`/api/fd-accounts/roles/customer/${customerId}`),
  
  updateRole: (roleId: number, data: UpdateRoleRequest) =>
    accountApiInstance.put(`/api/fd-accounts/roles/${roleId}`, data),
  
  deactivateRole: (roleId: number) =>
    accountApiInstance.delete(`/api/fd-accounts/roles/${roleId}`),
};

// ==================== NEW ACCOUNT SERVICE API (Port 8087) ====================

export const accountServiceApi = {
  // ===== ACCOUNT MANAGEMENT (8 endpoints) =====
  
  // List all accounts (Admin/Manager only)
  getAllAccounts: () => newAccountApiInstance.get('/'),
  
  // Get account by account number
  getAccountByNumber: (accountNumber: string) => 
    newAccountApiInstance.get(`/${accountNumber}`),
  
  // Get account balance
  getAccountBalance: (accountNumber: string) => 
    newAccountApiInstance.get(`/${accountNumber}/balance`),
  
  // Health check
  accountsHealth: () => newAccountApiInstance.get('/health'),
  
  // Get accounts by customer ID
  getAccountsByCustomerId: (customerId: number) => 
    newAccountApiInstance.get(`/customer/${customerId}`),
  
  // Account inquiry (search with filters)
  inquireAccounts: (data: import('../types').AccountInquiryRequest) => 
    newAccountApiInstance.post('/inquiry', data),
  
  // Create default account (uses product defaults)
  createDefaultAccount: (data: import('../types').CreateDefaultAccountRequest) => 
    newAccountApiInstance.post('/create/default', data),
  
  // Create custom account (custom rates/tenure)
  createCustomAccount: (data: import('../types').CreateCustomAccountRequest) => 
    newAccountApiInstance.post('/create/custom', data),

  // ===== BATCH MANAGEMENT (8 endpoints - Admin/Manager only) =====
  
  // Time Travel Status
  getTimeTravelStatus: () => newAccountApiInstance.get('/batch/time-travel/status'),
  
  // Set Time Travel (simulate date)
  setTimeTravel: (data: import('../types').SetTimeTravelRequest) => 
    newAccountApiInstance.post('/batch/time-travel/set', data),
  
  // Clear Time Travel (reset to real date)
  clearTimeTravel: () => newAccountApiInstance.post('/batch/time-travel/clear'),
  
  // Get overall batch status
  getBatchStatus: () => newAccountApiInstance.get('/batch/status'),
  
  // Trigger maturity processing batch
  triggerMaturityProcessing: () => 
    newAccountApiInstance.post('/batch/maturity-processing/trigger'),
  
  // Get maturity processing status
  getMaturityProcessingStatus: () => 
    newAccountApiInstance.get('/batch/maturity-processing/status'),
  
  // Trigger interest capitalization batch
  triggerInterestCapitalization: () => 
    newAccountApiInstance.post('/batch/interest-capitalization/trigger'),
  
  // Trigger interest accrual batch
  triggerInterestAccrual: () => 
    newAccountApiInstance.post('/batch/interest-accrual/trigger'),

  // ===== INTEREST CALCULATION (1 endpoint - Admin/Manager only) =====
  
  // Calculate and credit interest
  calculateInterest: (data: import('../types').InterestCalculationRequest) => 
    newAccountApiInstance.post('/interest/calculate', data),

  // ===== REDEMPTION MANAGEMENT (2 endpoints) =====
  
  // Inquire redemption (Customer can inquire their own, Admin/Manager can inquire any)
  inquireRedemption: (data: import('../types').RedemptionInquiryRequest) => 
    newAccountApiInstance.post('/redemptions/inquiry', data),
  
  // Process redemption (Admin/Manager only)
  processRedemption: (data: import('../types').ProcessRedemptionRequest) => 
    newAccountApiInstance.post('/redemptions/process', data),

  // ===== TRANSACTION MANAGEMENT (8 endpoints) =====
  
  // Get transaction by ID
  getTransactionById: (id: number) => 
    newAccountApiInstance.get(`/transactions/${id}`),
  
  // Get transactions by type
  getTransactionsByType: (type: string) => 
    newAccountApiInstance.get(`/transactions/type/${type}`),
  
  // Get transactions by status
  getTransactionsByStatus: (status: string) => 
    newAccountApiInstance.get(`/transactions/status/${status}`),
  
  // List transactions by account number
  getTransactionsByAccount: (accountNumber: string) => 
    newAccountApiInstance.get(`/transactions/account/${accountNumber}`),
  
  // Count transactions by account
  countTransactionsByAccount: (accountNumber: string) => 
    newAccountApiInstance.get(`/transactions/account/${accountNumber}/count`),
  
  // Transaction inquiry (search with filters)
  inquireTransactions: (data: import('../types').TransactionInquiryRequest) => 
    newAccountApiInstance.post('/transactions/inquiry', data),
  
  // Create transaction
  createTransaction: (data: import('../types').CreateTransactionRequest) => 
    newAccountApiInstance.post('/transactions', data),
  
  // Get all transactions (Admin/Manager only)
  getAllTransactions: () => newAccountApiInstance.get('/transactions'),
};

export default loginApi;
