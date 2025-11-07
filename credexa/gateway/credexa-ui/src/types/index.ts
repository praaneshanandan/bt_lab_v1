// Customer Types
export interface Customer {
  id: number;
  userId: number;
  username: string;
  fullName: string;
  mobileNumber: string;
  email: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  kycStatus: 'PENDING' | 'IN_PROGRESS' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
  country?: string;
  isActive: boolean;
  accountNumber?: string;
  ifscCode?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  emailNotifications: boolean;
  smsNotifications: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  // NO userId - it's auto-fetched from JWT token by backend
  fullName: string;
  mobileNumber: string;
  email: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  accountNumber?: string;
  ifscCode?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface UpdateCustomerRequest {
  fullName?: string;
  mobileNumber?: string;
  email?: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  classification?: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  kycStatus?: 'PENDING' | 'IN_PROGRESS' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
  country?: string;
  isActive?: boolean;
  accountNumber?: string;
  ifscCode?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

// Product Types (matching backend)
export interface Product {
  id: number;
  productName: string;
  productCode: string;
  productType: string;
  description?: string;
  effectiveDate: string;
  endDate?: string;
  currencyCode: string;
  status: string;
  
  // Term and Amount
  minTermMonths: number;
  maxTermMonths: number;
  minAmount: number;
  maxAmount: number;
  minBalanceRequired?: number;
  
  // Interest
  baseInterestRate: number;
  interestCalculationMethod?: string;
  interestPayoutFrequency?: string;
  
  // TDS
  tdsRate?: number;
  tdsApplicable: boolean;
  
  // Flags
  prematureWithdrawalAllowed: boolean;
  partialWithdrawalAllowed: boolean;
  loanAgainstDepositAllowed: boolean;
  autoRenewalAllowed: boolean;
  nomineeAllowed: boolean;
  jointAccountAllowed: boolean;
  
  // Audit
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  
  // Legacy compatibility
  productId?: number;
  interestRate?: number;
  compoundingFrequency?: string;
  isActive?: boolean;
  features?: string;
}

export interface InterestRateMatrix {
  id?: number;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  interestRate: number;
  customerClassification?: string;
  effectiveDate: string;
  endDate?: string;
}

export interface ProductCharge {
  id?: number;
  chargeName: string;
  chargeType: string;
  amount?: number;
  percentage?: number;
  frequency: string;
  description?: string;
}

export interface ProductRole {
  id?: number;
  roleType: string;
  mandatory: boolean;
  minCount: number;
  maxCount: number;
  description?: string;
}

// Calculator Types
export interface CalculatorRequest {
  principalAmount: number;
  interestRate: number;
  termInMonths: number;
  compoundingFrequency: string;
}

export interface CalculatorResponse {
  principalAmount: number;
  interestRate: number;
  termInMonths: number;
  compoundingFrequency: string;
  maturityAmount: number;
  interestEarned: number;
  effectiveInterestRate: number;
  calculationDate: string;
}

// FD Account Types
export interface FdAccount {
  accountNumber: string;
  productCode: string;
  customerId: number;
  principalAmount: number;
  interestRate: number;
  termMonths: number;
  maturityAmount: number;
  accountStatus: string;
  openDate: string;
  maturityDate: string;
  branchCode?: string;
  maturityInstruction?: string;
  autoRenewal: boolean;
  compoundingFrequency: string;
  lastInterestCreditDate?: string;
  remarks?: string;
  createdDate: string;
  lastModifiedDate?: string;
  roles?: AccountRole[];
  transactions?: Transaction[];
}

export interface AccountSummary {
  accountNumber: string;
  customerName: string;
  productName: string;
  principalAmount: number;
  currentValue: number;
  interestEarned: number;
  maturityDate: string;
  accountStatus: string;
  daysToMaturity: number;
}

export interface CreateStandardAccountRequest {
  productCode: string;
  customerId: number;
  principalAmount: number;
  termMonths: number;
  branchCode?: string;
  maturityInstruction?: string;
  autoRenewal?: boolean;
  remarks?: string;
}

export interface CustomizeAccountRequest {
  productCode: string;
  customerId: number;
  principalAmount: number;
  customTermMonths: number;
  customInterestRate: number;
  branchCode?: string;
  maturityInstruction?: string;
  autoRenewal?: boolean;
  remarks?: string;
}

export interface SearchAccountParams {
  customerId?: number;
  productCode?: string;
  branchCode?: string;
  accountStatus?: string;
  fromDate?: string;
  toDate?: string;
  minPrincipalAmount?: number;
  maxPrincipalAmount?: number;
}

// Transaction Types
export interface Transaction {
  transactionId: number;
  accountNumber: string;
  transactionType: string;
  amount: number;
  transactionDate: string;
  valueDate: string;
  description?: string;
  transactionReference: string;
  balanceAfter: number;
  initiatedBy?: string;
  approvedBy?: string;
  status: string;
  reversalReference?: string;
  createdDate: string;
}

export interface CreateTransactionRequest {
  accountNumber: string;
  transactionType: string;
  amount: number;
  description?: string;
}

export interface PrematureWithdrawalInquiry {
  accountNumber: string;
  principalAmount: number;
  interestEarned: number;
  penaltyAmount: number;
  netPayable: number;
  reducedInterestRate: number;
  originalMaturityDate: string;
  withdrawalDate: string;
  remarks: string;
}

export interface PrematureWithdrawalRequest {
  accountNumber: string;
  withdrawalAmount: number;
  reason?: string;
}

// Role Types
export interface AccountRole {
  roleId: number;
  accountNumber: string;
  customerId: number;
  roleType: string;
  ownershipPercentage?: number;
  isPrimary: boolean;
  isActive: boolean;
  effectiveDate: string;
  expiryDate?: string;
  remarks?: string;
  createdDate: string;
  lastModifiedDate?: string;
}

export interface AddRoleRequest {
  customerId: number;
  roleType: string;
  ownershipPercentage?: number;
  isPrimary?: boolean;
  remarks?: string;
}

export interface UpdateRoleRequest {
  roleType?: string;
  ownershipPercentage?: number;
  isPrimary?: boolean;
  remarks?: string;
}

// Auth/Login Types
export interface LoginRequest {
  usernameOrEmailOrMobile: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  roles: string[];
  preferredLanguage: string;
  preferredCurrency: string;
  loginTime: string;
  expiresIn: number;
}

export interface RegisterRequest {
  // Login Account Fields
  username: string;
  password: string;
  email: string;
  mobileNumber: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  // Customer Profile Fields
  fullName: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  // Address Details
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  // Optional Financial Details
  accountNumber?: string;
  ifscCode?: string;
  // Communication Preferences
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface AdminCreateCustomerRequest {
  username: string;
  email: string;
  mobileNumber: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  temporaryPassword?: string; // Optional - auto-generated if not provided
  fullName: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  accountNumber?: string;
  ifscCode?: string;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface AdminCreateCustomerResponse {
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  temporaryPassword: string; // The generated or provided temporary password
  accountActive: boolean;
  customerId: number;
  fullName: string;
  classification: string;
  kycStatus: string;
  message: string;
}

export interface TokenValidationResponse {
  valid: boolean;
  username?: string;
  userId?: number;
  roles?: string[];
}

export interface BankConfigResponse {
  bankName: string;
  bankLogo?: string;
  defaultLanguage: string;
  defaultCurrency: string;
  supportedLanguages: string[];
  supportedCurrencies: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
}
