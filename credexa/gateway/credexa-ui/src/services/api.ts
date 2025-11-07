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
} from '../types';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth/Login Service APIs
export const authApi = {
  login: (data: LoginRequest) => api.post('/api/auth/login', data),
  register: (data: RegisterRequest) => api.post('/api/auth/register', data),
  logout: () => api.post('/api/auth/logout'),
  validateToken: (token: string) => api.post('/api/auth/validate-token', token),
  getBankConfig: () => api.get('/api/auth/bank-config'),
  getUserByUsername: (username: string) => api.get(`/api/auth/user/${username}`),
  health: () => api.get('/api/auth/health'),
};

// Customer Service APIs
export const customerApi = {
  getCustomer: (id: number) => api.get(`/api/customer/${id}`),
  getAllCustomers: () => api.get('/api/customer/all'),
  getOwnProfile: () => api.get('/api/customer/profile'),
  createCustomer: (data: CreateCustomerRequest) => api.post('/api/customer', data),
  updateCustomer: (id: number, data: UpdateCustomerRequest) => api.put(`/api/customer/${id}`, data),
  getCustomerByUserId: (userId: number) => api.get(`/api/customer/user/${userId}`),
  getCustomerClassification: (id: number) => api.get(`/api/customer/${id}/classification`),
  getCustomer360View: (id: number) => api.get(`/api/customer/${id}/360-view`),
};

// Product Pricing Service APIs
export const productApi = {
  getAllProducts: () => api.get('/api/products'),
  getProductByCode: (code: string) => api.get(`/api/products/code/${code}`),
  getProductById: (id: number) => api.get(`/api/products/${id}`),
};

// FD Calculator Service APIs
export const calculatorApi = {
  calculateStandalone: (data: {
    principalAmount: number;
    interestRate: number;
    termInMonths: number;
    compoundingFrequency: string;
  }) => api.post('/api/calculator/calculate/standalone', data),
  calculateWithProduct: (productCode: string, principalAmount: number) =>
    api.post(`/api/calculator/calculate/product/${productCode}`, { principalAmount }),
};

// FD Account Service APIs
export const fdAccountApi = {
  // Account Creation
  createStandardAccount: (data: CreateStandardAccountRequest) =>
    api.post('/api/fd-accounts/accounts/create/standard', data),

  customizeAccount: (data: CustomizeAccountRequest) =>
    api.post('/api/fd-accounts/accounts/create/customize', data),

  // Account Inquiry
  getAccount: (accountNumber: string) => api.get(`/api/fd-accounts/accounts/${accountNumber}`),
  getAccountSummary: (accountNumber: string) => api.get(`/api/fd-accounts/accounts/${accountNumber}/summary`),
  getAccountsByCustomer: (customerId: number) => api.get(`/api/fd-accounts/accounts/customer/${customerId}`),
  getAccountsByProduct: (productCode: string) => api.get(`/api/fd-accounts/accounts/product/${productCode}`),
  getAccountsByBranch: (branchCode: string) => api.get(`/api/fd-accounts/accounts/branch/${branchCode}`),
  getAccountsMaturingInDays: (days: number) => api.get(`/api/fd-accounts/accounts/maturing?days=${days}`),
  checkAccountExists: (accountNumber: string) => api.get(`/api/fd-accounts/accounts/exists/${accountNumber}`),

  // Account Search
  searchAccounts: (searchParams: SearchAccountParams) => api.post('/api/fd-accounts/accounts/search', searchParams),

  // Transactions
  createTransaction: (data: CreateTransactionRequest) =>
    api.post('/api/fd-accounts/transactions', data),
  
  getTransactionsByAccount: (accountNumber: string) => 
    api.get(`/api/fd-accounts/transactions/account/${accountNumber}`),
  
  reverseTransaction: (transactionReference: string) => 
    api.post(`/api/fd-accounts/transactions/${transactionReference}/reverse`),

  // Premature Withdrawal
  inquirePrematureWithdrawal: (accountNumber: string) =>
    api.post('/api/fd-accounts/transactions/premature-withdrawal/inquire', { accountNumber }),
  
  processPrematureWithdrawal: (data: PrematureWithdrawalRequest) =>
    api.post('/api/fd-accounts/transactions/premature-withdrawal/process', data),

  // Roles Management
  addRole: (accountNumber: string, data: AddRoleRequest) =>
    api.post(`/api/fd-accounts/roles/account/${accountNumber}`, data),

  getRolesByAccount: (accountNumber: string) => 
    api.get(`/api/fd-accounts/roles/account/${accountNumber}`),
  
  getActiveRolesByAccount: (accountNumber: string) =>
    api.get(`/api/fd-accounts/roles/account/${accountNumber}/active`),
  
  getRolesByCustomer: (customerId: number) =>
    api.get(`/api/fd-accounts/roles/customer/${customerId}`),
  
  updateRole: (roleId: number, data: UpdateRoleRequest) =>
    api.put(`/api/fd-accounts/roles/${roleId}`, data),
  
  deactivateRole: (roleId: number) =>
    api.delete(`/api/fd-accounts/roles/${roleId}`),
};

export default api;
