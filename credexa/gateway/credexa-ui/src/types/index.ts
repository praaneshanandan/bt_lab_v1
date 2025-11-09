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
  productId: number;  // Backend uses productId, not id
  productName: string;
  productCode: string;
  productType: string;
  description?: string;
  effectiveDate: string;
  bankBranchCode: string;
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
  
  // Relationships
  allowedRoles?: ProductRole[];
  charges?: ProductCharge[];
  interestRateMatrix?: InterestRateMatrix[];
  
  // Audit
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  currentlyActive?: boolean;
  
  // Legacy compatibility (for backward compatibility)
  id?: number;
  interestRate?: number;
  compoundingFrequency?: string;
  isActive?: boolean;
  features?: string;
  endDate?: string;
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

// Calculator Types - Matching Backend DTOs
export interface StandaloneCalculationRequest {
  principalAmount: number;
  interestRate: number;
  tenure: number;
  tenureUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  calculationType: 'SIMPLE' | 'COMPOUND';
  compoundingFrequency?: 'DAILY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUALLY' | 'ANNUALLY';
  tdsRate?: number;
  customerClassifications?: string[];
}

export interface ProductBasedCalculationRequest {
  productId: number;
  principalAmount: number;
  tenure: number;
  tenureUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  calculationType?: 'SIMPLE' | 'COMPOUND';
  compoundingFrequency?: 'DAILY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUALLY' | 'ANNUALLY';
  customInterestRate?: number;
  customerId?: number;
  customerClassifications?: string[];
  applyTds?: boolean;
}

export interface MonthlyBreakdown {
  month: number;
  date: string;
  openingBalance: number;
  interestEarned: number;
  closingBalance: number;
  cumulativeInterest: number;
}

export interface CalculationResponse {
  principalAmount: number;
  interestRate: number;
  baseInterestRate?: number;
  additionalInterestRate?: number;
  tenure: number;
  tenureUnit: string;
  tenureInYears: number;
  calculationType: string;
  compoundingFrequency?: string;
  interestEarned: number;
  tdsAmount?: number;
  tdsRate?: number;
  maturityAmount: number;
  netInterest: number;
  startDate: string;
  maturityDate: string;
  productId?: number;
  productName?: string;
  productCode?: string;
  customerClassifications?: string[];
  monthlyBreakdown?: MonthlyBreakdown[];
}

export interface ComparisonScenario {
  principalAmount: number;
  interestRate: number;
  tenure: number;
  tenureUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  calculationType: 'SIMPLE' | 'COMPOUND';
  compoundingFrequency?: 'DAILY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUALLY' | 'ANNUALLY';
  tdsRate?: number;
  customerClassifications?: string[];
}

export interface ComparisonRequest {
  commonPrincipal?: number;
  scenarios: ComparisonScenario[];
}

export interface ComparisonResponse {
  scenarios: CalculationResponse[];
  bestScenario: CalculationResponse;
  bestScenarioIndex: number;
}

// Legacy types for backward compatibility
export interface CalculatorRequest {
  principalAmount: number;
  interestRate: number;
  termInMonths: number;
  compoundingFrequency: string;
}

export interface CalculatorResponse extends CalculationResponse {
  termInMonths?: number;
  effectiveInterestRate?: number;
  calculationDate?: string;
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

// ==================== ACCOUNT SERVICE TYPES (Port 8087) ====================

// Account Management Types
export interface FDAccount {
  accountNumber: string;
  customerId: number;
  productCode: string;
  principalAmount: number;
  interestRate: number;
  tenure: number;
  tenureUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  maturityDate: string;
  maturityAmount: number;
  nomineeDetails?: string;
  accountStatus: 'ACTIVE' | 'MATURED' | 'CLOSED' | 'SUSPENDED';
  openingDate: string;
  lastInterestCreditDate?: string;
  accruedInterest?: number;
  closingDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDefaultAccountRequest {
  accountName: string;
  customerId: number;
  productCode: string;
  principalAmount: number;
  termMonths: number;
  effectiveDate: string; // ISO format: YYYY-MM-DD
  branchCode?: string;
  branchName?: string;
  remarks?: string;
}

export interface CreateCustomAccountRequest {
  accountName: string;
  customerId: number;
  productCode: string;
  principalAmount: number;
  termMonths: number;
  effectiveDate: string; // ISO format: YYYY-MM-DD
  interestRate?: number;
  branchCode?: string;
  branchName?: string;
  remarks?: string;
}

export interface AccountInquiryRequest {
  accountNumber?: string;
  customerId?: number;
  productCode?: string;
  status?: 'ACTIVE' | 'MATURED' | 'CLOSED' | 'SUSPENDED';
}

export interface AccountBalanceResponse {
  accountNumber: string;
  principalAmount: number;
  accruedInterest: number;
  totalBalance: number;
  maturityAmount: number;
  accountStatus: string;
}

// Transaction Types
export interface FDTransaction {
  id: number;
  transactionReference: string;
  accountNumber: string;
  transactionType: 'OPENING' | 'INTEREST_CREDIT' | 'MATURITY' | 'PREMATURE_WITHDRAWAL' | 'CLOSURE';
  transactionAmount: number;
  transactionDate: string;
  transactionStatus: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  description?: string;
  performedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionInquiryRequest {
  accountNumber?: string;
  transactionType?: string;
  transactionStatus?: string;
  fromDate?: string;
  toDate?: string;
}

// Batch Management Types
export interface BatchStatusResponse {
  batchType: 'MATURITY_PROCESSING' | 'INTEREST_CAPITALIZATION' | 'INTEREST_ACCRUAL';
  status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  lastRunTime?: string;
  nextScheduledTime?: string;
  processedCount?: number;
  failedCount?: number;
  message?: string;
}

export interface TimeTravelStatusResponse {
  enabled: boolean;
  currentSimulatedDate?: string;
  actualSystemDate: string;
  message: string;
}

export interface SetTimeTravelRequest {
  targetDate: string; // ISO date string
}

// Interest Calculation Types
export interface InterestCalculationRequest {
  accountNumber: string;
  calculationDate?: string; // Optional, defaults to today
}

export interface InterestCalculationResponse {
  accountNumber: string;
  calculatedInterest: number;
  accruedInterest: number;
  lastCreditDate?: string;
  nextCreditDate?: string;
  calculationDate: string;
  credited: boolean;
  message: string;
}

// Redemption Types
export interface RedemptionInquiryRequest {
  accountNumber: string;
  redemptionDate?: string; // Optional, defaults to today
}

export interface RedemptionInquiryResponse {
  accountNumber: string;
  principalAmount: number;
  accruedInterest: number;
  penaltyAmount: number;
  netRedemptionAmount: number;
  maturityDate: string;
  redemptionDate: string;
  isPremature: boolean;
  message: string;
}

export interface ProcessRedemptionRequest {
  accountNumber: string;
  redemptionDate?: string;
  reason?: string;
}

export interface ProcessRedemptionResponse {
  accountNumber: string;
  transactionReference: string;
  redemptionAmount: number;
  penaltyApplied: number;
  status: 'COMPLETED' | 'FAILED';
  message: string;
  processedAt: string;
}

