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

// Service ports:
// Login Service: 8081
// Customer Service: 8082
// FD Account Service: 8083
// Product Pricing Service: 8084
// FD Calculator Service: 8085

// Create axios instance for Login Service
const loginApi = axios.create({
  baseURL: 'http://localhost:8081',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Customer Service
const customerApiInstance = axios.create({
  baseURL: 'http://localhost:8082',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for FD Account Service
const accountApiInstance = axios.create({
  baseURL: 'http://localhost:8083',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Product Pricing Service
const productApiInstance = axios.create({
  baseURL: 'http://localhost:8084',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for Calculator Service
const calculatorApiInstance = axios.create({
  baseURL: 'http://localhost:8085',
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
  
  // Product roles
  getProductRoles: (id: number) => productApiInstance.get(`/api/products/${id}/roles`),
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

export default loginApi;
