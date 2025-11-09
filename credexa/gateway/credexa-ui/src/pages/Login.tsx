import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2, CheckCircle2, AlertTriangle, Lock } from 'lucide-react';
import { PasswordStrength } from '@/components/PasswordStrength';
import { ThemeToggle } from '@/components/ThemeToggle';
import { authApi } from '@/services/api';
import type { BankConfigResponse } from '@/types';

export default function Login() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('login');
  const [bankConfig, setBankConfig] = useState<BankConfigResponse | null>(null);
  
  // Login state
  const [loginForm, setLoginForm] = useState({
    usernameOrEmailOrMobile: '',
    password: '',
  });
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isAccountLocked, setIsAccountLocked] = useState(false);

  // Register state
  const [registerStep, setRegisterStep] = useState(1);
  const [registerForm, setRegisterForm] = useState<{
    // Step 1: Account Details
    username: string;
    password: string;
    confirmPassword: string;
    email: string;
    mobileNumber: string;
    preferredLanguage: string;
    preferredCurrency: string;
    // Step 2: Personal Details
    fullName: string;
    dateOfBirth: string;
    gender: 'MALE' | 'FEMALE' | 'OTHER';
    panNumber: string;
    aadharNumber: string;
    classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
    // Step 3: Address Details
    addressLine1: string;
    addressLine2: string;
    city: string;
    state: string;
    pincode: string;
    country: string;
    // Optional: Financial Details
    accountNumber: string;
    ifscCode: string;
    // Communication Preferences
    emailNotifications: boolean;
    smsNotifications: boolean;
  }>({
    // Step 1: Account Details
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    mobileNumber: '',
    preferredLanguage: 'en',
    preferredCurrency: 'INR',
    // Step 2: Personal Details
    fullName: '',
    dateOfBirth: '',
    gender: 'MALE',
    panNumber: '',
    aadharNumber: '',
    classification: 'REGULAR',
    // Step 3: Address Details
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    pincode: '',
    country: 'India',
    // Optional: Financial Details
    accountNumber: '',
    ifscCode: '',
    // Communication Preferences
    emailNotifications: true,
    smsNotifications: true,
  });
  const [registerLoading, setRegisterLoading] = useState(false);
  const [registerError, setRegisterError] = useState<string | null>(null);
  const [registerSuccess, setRegisterSuccess] = useState(false);

  useEffect(() => {
    fetchBankConfig();
  }, []);

  const fetchBankConfig = async () => {
    try {
      const response = await authApi.getBankConfig();
      setBankConfig(response.data.data);
    } catch (error) {
      console.error('Failed to fetch bank config:', error);
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginLoading(true);
    setLoginError(null);

    try {
      console.log('Attempting login with:', loginForm.usernameOrEmailOrMobile);
      const response = await authApi.login(loginForm);
      console.log('Login response:', response.data);
      const { data } = response.data;
      
      if (!data || !data.token) {
        throw new Error('Invalid response from server');
      }
      
      // Reset failed attempts on successful login
      setFailedAttempts(0);
      setIsAccountLocked(false);
      
      // Clear any stale customer data from previous sessions
      localStorage.removeItem('customerId');
      
      // Store authentication data
      localStorage.setItem('isAuthenticated', 'true');
      localStorage.setItem('authToken', data.token);
      localStorage.setItem('userId', data.userId.toString());
      localStorage.setItem('username', data.username);
      localStorage.setItem('userRoles', JSON.stringify(data.roles));
      
      console.log('Login successful, navigating to dashboard');
      navigate('/');
    } catch (error: unknown) {
      console.error('Login error:', error);
      const err = error as { response?: { data?: { error?: string; message?: string } } };
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Login failed. Please check your credentials.';
      
      // Check for specific account locked error
      if (errorMsg.toLowerCase().includes('locked')) {
        setIsAccountLocked(true);
        setLoginError('Your account has been locked after 5 failed login attempts. Please contact support to unlock your account.');
      } else if (errorMsg.toLowerCase().includes('invalid credentials') || errorMsg.toLowerCase().includes('incorrect')) {
        // Increment failed attempts
        const newFailedAttempts = failedAttempts + 1;
        setFailedAttempts(newFailedAttempts);
        
        if (newFailedAttempts >= 5) {
          setIsAccountLocked(true);
          setLoginError('Your account has been locked after 5 failed login attempts. Please contact support.');
        } else {
          const remainingAttempts = 5 - newFailedAttempts;
          setLoginError(`Invalid credentials. You have ${remainingAttempts} attempt${remainingAttempts > 1 ? 's' : ''} remaining before your account is locked.`);
        }
      } else {
        setLoginError(errorMsg);
      }
    } finally {
      setLoginLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegisterLoading(true);
    setRegisterError(null);
    setRegisterSuccess(false);

    // Validate passwords match
    if (registerForm.password !== registerForm.confirmPassword) {
      setRegisterError('Passwords do not match');
      setRegisterLoading(false);
      return;
    }

    // Validate password length
    if (registerForm.password.length < 8) {
      setRegisterError('Password must be at least 8 characters long');
      setRegisterLoading(false);
      return;
    }

    // Validate PAN format
    const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
    if (registerForm.panNumber && !panRegex.test(registerForm.panNumber)) {
      setRegisterError('Invalid PAN format (e.g., ABCDE1234F)');
      setRegisterLoading(false);
      return;
    }

    // Validate Aadhar format
    const aadharRegex = /^[0-9]{12}$/;
    if (registerForm.aadharNumber && !aadharRegex.test(registerForm.aadharNumber)) {
      setRegisterError('Aadhar number must be exactly 12 digits');
      setRegisterLoading(false);
      return;
    }

    // Validate pincode
    const pincodeRegex = /^[0-9]{6}$/;
    if (!pincodeRegex.test(registerForm.pincode)) {
      setRegisterError('Pincode must be exactly 6 digits');
      setRegisterLoading(false);
      return;
    }

    try {
      const { confirmPassword, ...registerData } = registerForm;
      console.log('Attempting registration with:', registerData);
      const response = await authApi.register(registerData);
      console.log('Registration response:', response.data);
      
      setRegisterSuccess(true);
      toast.success('Registration successful! Please login with your credentials.');
      
      // Reset form
      setRegisterForm({
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        mobileNumber: '',
        preferredLanguage: 'en',
        preferredCurrency: 'INR',
        fullName: '',
        dateOfBirth: '',
        gender: 'MALE',
        panNumber: '',
        aadharNumber: '',
        classification: 'REGULAR',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        pincode: '',
        country: 'India',
        accountNumber: '',
        ifscCode: '',
        emailNotifications: true,
        smsNotifications: true,
      });
      setRegisterStep(1);
      
      // Switch to login tab after 2 seconds
      setTimeout(() => {
        setActiveTab('login');
        setRegisterSuccess(false);
      }, 2000);
    } catch (error: unknown) {
      console.error('Registration error:', error);
      const err = error as { response?: { data?: { error?: string; message?: string } } };
      const errorMsg = err.response?.data?.error || err.response?.data?.message || 'Registration failed. Please try again.';
      setRegisterError(errorMsg);
    } finally {
      setRegisterLoading(false);
    }
  };

  const nextStep = () => {
    setRegisterError(null);
    
    // Validate Step 1
    if (registerStep === 1) {
      if (!registerForm.username || registerForm.username.length < 3) {
        setRegisterError('Username must be at least 3 characters');
        return;
      }
      if (!registerForm.email) {
        setRegisterError('Email is required');
        return;
      }
      if (!registerForm.password || registerForm.password.length < 8) {
        setRegisterError('Password must be at least 8 characters');
        return;
      }
      if (registerForm.password !== registerForm.confirmPassword) {
        setRegisterError('Passwords do not match');
        return;
      }
      if (!registerForm.mobileNumber || registerForm.mobileNumber.length < 10) {
        setRegisterError('Mobile number must be at least 10 digits');
        return;
      }
    }
    
    // Validate Step 2
    if (registerStep === 2) {
      if (!registerForm.fullName) {
        setRegisterError('Full name is required');
        return;
      }
      if (!registerForm.dateOfBirth) {
        setRegisterError('Date of birth is required');
        return;
      }
      if (!registerForm.gender) {
        setRegisterError('Gender is required');
        return;
      }
    }
    
    setRegisterStep(registerStep + 1);
  };

  const prevStep = () => {
    setRegisterError(null);
    setRegisterStep(registerStep - 1);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 p-4 overflow-auto">
      {/* Theme Toggle - Top Right */}
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      
      <Card className="w-full max-w-md shadow-xl my-8 bg-card dark:bg-card border-border dark:border-border">
        <CardHeader className="space-y-1 text-center pb-4">
          <div className="flex justify-center mb-4">
            <img 
              src="/bank-logo.png" 
              alt="Bank Logo" 
              className="h-20 w-auto object-contain"
            />
          </div>
          <CardTitle className="text-3xl font-bold bg-gradient-to-r from-primary to-primary/80 bg-clip-text text-transparent">
            {bankConfig?.bankName || 'Credexa FD'}
          </CardTitle>
          <CardDescription className="text-base text-muted-foreground">
            Fixed Deposit Management System
          </CardDescription>
        </CardHeader>
        
        <CardContent className="max-h-[calc(100vh-300px)] overflow-y-auto">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger value="login" className="text-sm font-medium">Login</TabsTrigger>
              <TabsTrigger value="register" className="text-sm font-medium">Register</TabsTrigger>
            </TabsList>

            {/* Login Tab */}
            <TabsContent value="login" className="space-y-4">
              <form onSubmit={handleLogin} className="space-y-4">
                {isAccountLocked && (
                  <Alert variant="destructive" className="border-red-300 dark:border-red-800 bg-red-50 dark:bg-red-950">
                    <Lock className="h-4 w-4" />
                    <AlertDescription className="font-medium">
                      Your account has been locked after 5 failed login attempts. Please contact support to unlock your account.
                    </AlertDescription>
                  </Alert>
                )}
                
                {loginError && !isAccountLocked && (
                  <Alert variant={failedAttempts > 0 ? "destructive" : "default"} className={failedAttempts >= 3 ? 'border-orange-300 dark:border-orange-800 bg-orange-50 dark:bg-orange-950' : ''}>
                    {failedAttempts >= 3 && <AlertTriangle className="h-4 w-4 text-orange-600 dark:text-orange-400" />}
                    <AlertDescription className={failedAttempts >= 3 ? 'text-orange-800 dark:text-orange-300 font-medium' : ''}>
                      {loginError}
                    </AlertDescription>
                  </Alert>
                )}

                <div className="space-y-2">
                  <Label htmlFor="usernameOrEmailOrMobile">Username, Email or Mobile</Label>
                  <Input
                    id="usernameOrEmailOrMobile"
                    type="text"
                    placeholder="Enter username, email or mobile"
                    value={loginForm.usernameOrEmailOrMobile}
                    onChange={(e) => setLoginForm({ ...loginForm, usernameOrEmailOrMobile: e.target.value })}
                    required
                    autoFocus
                    disabled={isAccountLocked}
                    className="h-11"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="Enter your password"
                    value={loginForm.password}
                    onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                    required
                    disabled={isAccountLocked}
                    className="h-11"
                  />
                </div>

                <Button type="submit" className="w-full h-11 text-base font-medium" disabled={loginLoading || isAccountLocked}>
                  {loginLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Signing in...
                    </>
                  ) : (
                    'Sign In'
                  )}
                </Button>

                {/* Divider */}
                <div className="relative my-6">
                  <div className="absolute inset-0 flex items-center">
                    <span className="w-full border-t border-border" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-card dark:bg-card px-2 text-muted-foreground">Or continue with</span>
                  </div>
                </div>

                {/* Google Sign-In Button */}
                <Button
                  type="button"
                  variant="outline"
                  className="w-full h-11 text-base font-medium"
                  onClick={() => window.location.href = 'http://localhost:8081/api/auth/oauth2/authorization/google'}
                  disabled={isAccountLocked}
                >
                  <svg className="mr-2 h-5 w-5" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                    />
                  </svg>
                  Continue with Google
                </Button>

                <div className="text-center text-sm text-muted-foreground mt-4 p-3 bg-primary/10 dark:bg-primary/20 rounded-lg border border-primary/20">
                  <p className="font-medium mb-1 text-foreground">New User?</p>
                  <p className="text-xs">Switch to the Register tab to create an account</p>
                </div>
              </form>
            </TabsContent>

            {/* Register Tab */}
            <TabsContent value="register" className="space-y-4 mt-0">
              {registerSuccess ? (
                <Alert className="bg-green-50 dark:bg-green-950 border-green-200 dark:border-green-800">
                  <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400" />
                  <AlertDescription className="text-green-800 dark:text-green-300 font-medium">
                    Registration successful! Redirecting to login...
                  </AlertDescription>
                </Alert>
              ) : (
                <div className="space-y-4">
                  {/* Step Indicator */}
                  <div className="flex justify-between items-center mb-4">
                    {[1, 2, 3].map((step) => (
                      <div key={step} className="flex items-center flex-1">
                        <div
                          className={`w-8 h-8 rounded-full flex items-center justify-center font-semibold text-sm ${
                            registerStep >= step
                              ? 'bg-primary text-primary-foreground'
                              : 'bg-muted text-muted-foreground'
                          }`}
                        >
                          {step}
                        </div>
                        {step < 3 && (
                          <div
                            className={`flex-1 h-1 mx-2 ${
                              registerStep > step ? 'bg-primary' : 'bg-muted'
                            }`}
                          />
                        )}
                      </div>
                    ))}
                  </div>

                  <div className="text-center mb-4">
                    <p className="text-sm font-semibold text-foreground">
                      {registerStep === 1 && 'Step 1: Account Details'}
                      {registerStep === 2 && 'Step 2: Personal Information'}
                      {registerStep === 3 && 'Step 3: Address Details'}
                    </p>
                  </div>

                  <form onSubmit={handleRegister} className="space-y-4 pb-2">
                    {registerError && (
                      <Alert variant="destructive">
                        <AlertDescription>{registerError}</AlertDescription>
                      </Alert>
                    )}

                    {/* Step 1: Account Details */}
                    {registerStep === 1 && (
                      <>
                        <div className="space-y-2">
                          <Label htmlFor="reg-username">Username *</Label>
                          <Input
                            id="reg-username"
                            type="text"
                            placeholder="Choose a username (min 3 chars)"
                            value={registerForm.username}
                            onChange={(e) => setRegisterForm({ ...registerForm, username: e.target.value })}
                            required
                            minLength={3}
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-email">Email *</Label>
                          <Input
                            id="reg-email"
                            type="email"
                            placeholder="your.email@example.com"
                            value={registerForm.email}
                            onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })}
                            required
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-mobile">Mobile Number *</Label>
                          <Input
                            id="reg-mobile"
                            type="tel"
                            placeholder="1234567890"
                            value={registerForm.mobileNumber}
                            onChange={(e) => setRegisterForm({ ...registerForm, mobileNumber: e.target.value })}
                            required
                            minLength={10}
                            maxLength={15}
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-password">Password *</Label>
                          <Input
                            id="reg-password"
                            type="password"
                            placeholder="Min 8 characters"
                            value={registerForm.password}
                            onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })}
                            required
                            minLength={8}
                            className="h-11"
                          />
                          <PasswordStrength password={registerForm.password} />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-confirm-password">Confirm Password *</Label>
                          <Input
                            id="reg-confirm-password"
                            type="password"
                            placeholder="Re-enter password"
                            value={registerForm.confirmPassword}
                            onChange={(e) => setRegisterForm({ ...registerForm, confirmPassword: e.target.value })}
                            required
                            className="h-11"
                          />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="reg-language">Language</Label>
                            <Select
                              value={registerForm.preferredLanguage}
                              onValueChange={(value) => setRegisterForm({ ...registerForm, preferredLanguage: value })}
                            >
                              <SelectTrigger className="h-11">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="en">English</SelectItem>
                                <SelectItem value="es">Spanish</SelectItem>
                                <SelectItem value="hi">Hindi</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>

                          <div className="space-y-2">
                            <Label htmlFor="reg-currency">Currency</Label>
                            <Select
                              value={registerForm.preferredCurrency}
                              onValueChange={(value) => setRegisterForm({ ...registerForm, preferredCurrency: value })}
                            >
                              <SelectTrigger className="h-11">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="USD">USD ($)</SelectItem>
                                <SelectItem value="EUR">EUR (€)</SelectItem>
                                <SelectItem value="INR">INR (₹)</SelectItem>
                                <SelectItem value="BHD">BHD</SelectItem>
                                <SelectItem value="JPY">JPY (¥)</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                      </>
                    )}

                    {/* Step 2: Personal Information */}
                    {registerStep === 2 && (
                      <>
                        <div className="space-y-2">
                          <Label htmlFor="reg-fullname">Full Name *</Label>
                          <Input
                            id="reg-fullname"
                            type="text"
                            placeholder="Enter your full name"
                            value={registerForm.fullName}
                            onChange={(e) => setRegisterForm({ ...registerForm, fullName: e.target.value })}
                            required
                            maxLength={100}
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-dob">Date of Birth *</Label>
                          <Input
                            id="reg-dob"
                            type="date"
                            value={registerForm.dateOfBirth}
                            onChange={(e) => setRegisterForm({ ...registerForm, dateOfBirth: e.target.value })}
                            required
                            max={new Date().toISOString().split('T')[0]}
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-gender">Gender *</Label>
                          <Select
                            value={registerForm.gender}
                            onValueChange={(value) => setRegisterForm({ ...registerForm, gender: value as 'MALE' | 'FEMALE' | 'OTHER' })}
                          >
                            <SelectTrigger className="h-11">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="MALE">Male</SelectItem>
                              <SelectItem value="FEMALE">Female</SelectItem>
                              <SelectItem value="OTHER">Other</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-classification">Customer Type *</Label>
                          <Select
                            value={registerForm.classification}
                            onValueChange={(value) => setRegisterForm({ ...registerForm, classification: value as 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR' })}
                          >
                            <SelectTrigger className="h-11">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="REGULAR">Regular</SelectItem>
                              <SelectItem value="PREMIUM">Premium</SelectItem>
                              <SelectItem value="VIP">VIP</SelectItem>
                              <SelectItem value="SENIOR_CITIZEN">Senior Citizen</SelectItem>
                              <SelectItem value="SUPER_SENIOR">Super Senior</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-pan">PAN Number (Optional)</Label>
                          <Input
                            id="reg-pan"
                            type="text"
                            placeholder="ABCDE1234F"
                            value={registerForm.panNumber}
                            onChange={(e) => setRegisterForm({ ...registerForm, panNumber: e.target.value.toUpperCase() })}
                            maxLength={10}
                            className="h-11"
                          />
                          <p className="text-xs text-muted-foreground">Format: 5 letters, 4 digits, 1 letter</p>
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-aadhar">Aadhar Number (Optional)</Label>
                          <Input
                            id="reg-aadhar"
                            type="text"
                            placeholder="123456789012"
                            value={registerForm.aadharNumber}
                            onChange={(e) => setRegisterForm({ ...registerForm, aadharNumber: e.target.value.replace(/\D/g, '') })}
                            maxLength={12}
                            className="h-11"
                          />
                          <p className="text-xs text-muted-foreground">12-digit Aadhar number</p>
                        </div>
                      </>
                    )}

                    {/* Step 3: Address Details */}
                    {registerStep === 3 && (
                      <>
                        <div className="space-y-2">
                          <Label htmlFor="reg-address1">Address Line 1 *</Label>
                          <Input
                            id="reg-address1"
                            type="text"
                            placeholder="House/Flat no., Street"
                            value={registerForm.addressLine1}
                            onChange={(e) => setRegisterForm({ ...registerForm, addressLine1: e.target.value })}
                            required
                            maxLength={255}
                            className="h-11"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="reg-address2">Address Line 2 (Optional)</Label>
                          <Input
                            id="reg-address2"
                            type="text"
                            placeholder="Locality, Landmark"
                            value={registerForm.addressLine2}
                            onChange={(e) => setRegisterForm({ ...registerForm, addressLine2: e.target.value })}
                            maxLength={255}
                            className="h-11"
                          />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="reg-city">City *</Label>
                            <Input
                              id="reg-city"
                              type="text"
                              placeholder="City"
                              value={registerForm.city}
                              onChange={(e) => setRegisterForm({ ...registerForm, city: e.target.value })}
                              required
                              maxLength={100}
                              className="h-11"
                            />
                          </div>

                          <div className="space-y-2">
                            <Label htmlFor="reg-state">State *</Label>
                            <Input
                              id="reg-state"
                              type="text"
                              placeholder="State"
                              value={registerForm.state}
                              onChange={(e) => setRegisterForm({ ...registerForm, state: e.target.value })}
                              required
                              maxLength={100}
                              className="h-11"
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="reg-pincode">Pincode *</Label>
                            <Input
                              id="reg-pincode"
                              type="text"
                              placeholder="123456"
                              value={registerForm.pincode}
                              onChange={(e) => setRegisterForm({ ...registerForm, pincode: e.target.value.replace(/\D/g, '') })}
                              required
                              maxLength={6}
                              className="h-11"
                            />
                          </div>

                          <div className="space-y-2">
                            <Label htmlFor="reg-country">Country *</Label>
                            <Input
                              id="reg-country"
                              type="text"
                              placeholder="Country"
                              value={registerForm.country}
                              onChange={(e) => setRegisterForm({ ...registerForm, country: e.target.value })}
                              required
                              maxLength={100}
                              className="h-11"
                            />
                          </div>
                        </div>

                        <div className="space-y-3 pt-2">
                          <Label className="text-sm font-semibold">Notification Preferences</Label>
                          <div className="flex items-center space-x-2">
                            <input
                              type="checkbox"
                              id="email-notif"
                              checked={registerForm.emailNotifications}
                              onChange={(e) => setRegisterForm({ ...registerForm, emailNotifications: e.target.checked })}
                              className="h-4 w-4"
                            />
                            <Label htmlFor="email-notif" className="font-normal cursor-pointer">
                              Email Notifications
                            </Label>
                          </div>
                          <div className="flex items-center space-x-2">
                            <input
                              type="checkbox"
                              id="sms-notif"
                              checked={registerForm.smsNotifications}
                              onChange={(e) => setRegisterForm({ ...registerForm, smsNotifications: e.target.checked })}
                              className="h-4 w-4"
                            />
                            <Label htmlFor="sms-notif" className="font-normal cursor-pointer">
                              SMS Notifications
                            </Label>
                          </div>
                        </div>
                      </>
                    )}

                    {/* Navigation Buttons */}
                    <div className="flex gap-2 pt-2">
                      {registerStep > 1 && (
                        <Button
                          type="button"
                          variant="outline"
                          onClick={prevStep}
                          className="flex-1 h-11"
                        >
                          Previous
                        </Button>
                      )}
                      
                      {registerStep < 3 ? (
                        <Button
                          type="button"
                          onClick={nextStep}
                          className="flex-1 h-11"
                        >
                          Next
                        </Button>
                      ) : (
                        <Button
                          type="submit"
                          className="flex-1 h-11 text-base font-medium"
                          disabled={registerLoading}
                        >
                          {registerLoading ? (
                            <>
                              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                              Creating Account...
                            </>
                          ) : (
                            'Create Account'
                          )}
                        </Button>
                      )}
                    </div>
                  </form>
                </div>
              )}
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}

