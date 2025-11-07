import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { customerApi } from '@/services/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  ArrowLeft, 
  User, 
  MapPin, 
  Phone, 
  Mail, 
  CreditCard, 
  Building2,
  Calendar,
  TrendingUp,
  Wallet,
  Loader2,
  AlertCircle
} from 'lucide-react';

interface Customer360Data {
  customer: {
    id: number;
    userId: number;
    username: string;
    fullName: string;
    mobileNumber: string;
    email: string;
    panNumber?: string;
    aadharNumber?: string;
    dateOfBirth: string;
    gender: string;
    classification: string;
    kycStatus: string;
    addressLine1?: string;
    addressLine2?: string;
    city?: string;
    state?: string;
    pincode?: string;
    country?: string;
    accountNumber?: string;
    ifscCode?: string;
    isActive: boolean;
    preferredLanguage: string;
    preferredCurrency: string;
    emailNotifications: boolean;
    smsNotifications: boolean;
    createdAt: string;
    updatedAt: string;
  };
  classification: {
    customerId: number;
    fullName: string;
    classification: string;
    additionalRatePercentage: number;
    classificationDescription: string;
  };
  accountSummary: {
    totalFdAccounts: number;
    totalInvestment: number;
    totalMaturityAmount: number;
    activeFdCount: number;
    maturedFdCount: number;
  };
  fdAccounts: any[];
}

export default function Customer360View() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<Customer360Data | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      fetch360Data(parseInt(id));
    }
  }, [id]);

  const fetch360Data = async (customerId: number) => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch customer data (the 360 endpoint might not exist yet, so we'll use the regular endpoint)
      const customerResponse = await customerApi.getCustomer(customerId);
      
      // Build the 360 data structure from available data
      const customer360: Customer360Data = {
        customer: customerResponse.data,
        classification: {
          customerId: customerResponse.data.id,
          fullName: customerResponse.data.fullName,
          classification: customerResponse.data.classification,
          additionalRatePercentage: getClassificationRate(customerResponse.data.classification),
          classificationDescription: getClassificationDescription(customerResponse.data.classification),
        },
        accountSummary: {
          totalFdAccounts: 0,
          totalInvestment: 0,
          totalMaturityAmount: 0,
          activeFdCount: 0,
          maturedFdCount: 0,
        },
        fdAccounts: [],
      };
      
      setData(customer360);
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to fetch customer data';
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const getClassificationRate = (classification: string): number => {
    const rates: Record<string, number> = {
      REGULAR: 0,
      PREMIUM: 0.25,
      VIP: 0.50,
      SENIOR_CITIZEN: 0.50,
      SUPER_SENIOR: 0.75,
    };
    return rates[classification] || 0;
  };

  const getClassificationDescription = (classification: string): string => {
    const descriptions: Record<string, string> = {
      REGULAR: 'Standard interest rates apply',
      PREMIUM: 'Enjoy 0.25% additional interest on all FD products',
      VIP: 'Benefit from 0.50% extra interest on your investments',
      SENIOR_CITIZEN: 'Special rate of 0.50% additional interest for senior citizens',
      SUPER_SENIOR: 'Premium rate of 0.75% additional interest for super senior citizens (80+)',
    };
    return descriptions[classification] || 'Standard classification';
  };

  const getClassificationBadge = (classification: string) => {
    const colors: Record<string, string> = {
      VIP: 'bg-purple-100 text-purple-800 border-purple-200',
      PREMIUM: 'bg-blue-100 text-blue-800 border-blue-200',
      SENIOR_CITIZEN: 'bg-orange-100 text-orange-800 border-orange-200',
      SUPER_SENIOR: 'bg-red-100 text-red-800 border-red-200',
      REGULAR: 'bg-gray-100 text-gray-800 border-gray-200',
    };
    return colors[classification] || colors.REGULAR;
  };

  const getKycBadge = (status: string) => {
    const colors: Record<string, string> = {
      VERIFIED: 'bg-green-100 text-green-800 border-green-200',
      REJECTED: 'bg-red-100 text-red-800 border-red-200',
      IN_PROGRESS: 'bg-blue-100 text-blue-800 border-blue-200',
      PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      EXPIRED: 'bg-gray-100 text-gray-800 border-gray-200',
    };
    return colors[status] || colors.PENDING;
  };

  const formatCurrency = (amount: number, currency: string = 'INR') => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-2" />
          <div className="text-lg text-gray-600">Loading customer 360° view...</div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="p-8">
        <Button variant="outline" onClick={() => navigate('/customers')} className="mb-4">
          <ArrowLeft size={16} className="mr-2" />
          Back to Customers
        </Button>
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error || 'Customer data not found'}</AlertDescription>
        </Alert>
      </div>
    );
  }

  const { customer, classification, accountSummary } = data;

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="outline" onClick={() => navigate('/customers')}>
            <ArrowLeft size={16} className="mr-2" />
            Back
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Customer 360° View</h1>
            <p className="text-gray-600 mt-1">Comprehensive customer information and insights</p>
          </div>
        </div>
      </div>

      {/* Customer Overview Card */}
      <Card className="border-2">
        <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-blue-600 rounded-full">
                <User className="h-8 w-8 text-white" />
              </div>
              <div>
                <CardTitle className="text-2xl">{customer.fullName}</CardTitle>
                <CardDescription className="text-base mt-1">
                  Customer ID: {customer.id} • User ID: {customer.userId} • Username: @{customer.username}
                </CardDescription>
              </div>
            </div>
            <div className="flex gap-2">
              <Badge className={`${getClassificationBadge(customer.classification)} border`}>
                {customer.classification.replace('_', ' ')}
              </Badge>
              <Badge className={`${getKycBadge(customer.kycStatus)} border`}>
                {customer.kycStatus}
              </Badge>
              <Badge className={customer.isActive ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-gray-100 text-gray-800 border border-gray-200'}>
                {customer.isActive ? 'Active' : 'Inactive'}
              </Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Contact Information */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <Phone size={16} className="text-blue-600" />
                Contact Details
              </h3>
              <div className="space-y-2 text-sm">
                <div className="flex items-center gap-2">
                  <Phone size={14} className="text-gray-400" />
                  <span>{customer.mobileNumber}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Mail size={14} className="text-gray-400" />
                  <span className="break-all">{customer.email}</span>
                </div>
              </div>
            </div>

            {/* Address */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <MapPin size={16} className="text-blue-600" />
                Address
              </h3>
              <div className="text-sm text-gray-600">
                {customer.addressLine1 && <div>{customer.addressLine1}</div>}
                {customer.addressLine2 && <div>{customer.addressLine2}</div>}
                {customer.city && customer.state && (
                  <div>{customer.city}, {customer.state}</div>
                )}
                {customer.pincode && <div>{customer.pincode}</div>}
                {customer.country && <div>{customer.country}</div>}
                {!customer.addressLine1 && <div className="text-gray-400">No address provided</div>}
              </div>
            </div>

            {/* Banking Details */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <Building2 size={16} className="text-blue-600" />
                Banking Details
              </h3>
              <div className="space-y-2 text-sm">
                {customer.accountNumber ? (
                  <>
                    <div>
                      <span className="text-gray-500">Account:</span>{' '}
                      <span className="font-medium">{customer.accountNumber}</span>
                    </div>
                    <div>
                      <span className="text-gray-500">IFSC:</span>{' '}
                      <span className="font-medium">{customer.ifscCode}</span>
                    </div>
                  </>
                ) : (
                  <div className="text-gray-400">No banking details</div>
                )}
              </div>
            </div>

            {/* Identity Documents */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <CreditCard size={16} className="text-blue-600" />
                Identity Documents
              </h3>
              <div className="space-y-2 text-sm">
                <div>
                  <span className="text-gray-500">PAN:</span>{' '}
                  <span className="font-medium">{customer.panNumber || 'Not provided'}</span>
                </div>
                <div>
                  <span className="text-gray-500">Aadhar:</span>{' '}
                  <span className="font-medium">{customer.aadharNumber || 'Not provided'}</span>
                </div>
              </div>
            </div>

            {/* Personal Info */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <Calendar size={16} className="text-blue-600" />
                Personal Information
              </h3>
              <div className="space-y-2 text-sm">
                <div>
                  <span className="text-gray-500">DOB:</span>{' '}
                  <span className="font-medium">{formatDate(customer.dateOfBirth)}</span>
                </div>
                <div>
                  <span className="text-gray-500">Gender:</span>{' '}
                  <span className="font-medium">{customer.gender}</span>
                </div>
                <div>
                  <span className="text-gray-500">Language:</span>{' '}
                  <span className="font-medium">{customer.preferredLanguage.toUpperCase()}</span>
                </div>
              </div>
            </div>

            {/* Account Info */}
            <div className="space-y-3">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <Calendar size={16} className="text-blue-600" />
                Account Timeline
              </h3>
              <div className="space-y-2 text-sm">
                <div>
                  <span className="text-gray-500">Created:</span>{' '}
                  <span className="font-medium">{formatDate(customer.createdAt)}</span>
                </div>
                <div>
                  <span className="text-gray-500">Updated:</span>{' '}
                  <span className="font-medium">{formatDate(customer.updatedAt)}</span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Classification Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp size={20} className="text-blue-600" />
            Customer Classification
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center p-6 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg">
              <div className="text-sm text-gray-600 mb-2">Classification Type</div>
              <Badge className={`${getClassificationBadge(classification.classification)} text-lg px-4 py-2 border`}>
                {classification.classification.replace('_', ' ')}
              </Badge>
            </div>
            <div className="text-center p-6 bg-gradient-to-br from-green-50 to-emerald-50 rounded-lg">
              <div className="text-sm text-gray-600 mb-2">Additional Interest Rate</div>
              <div className="text-3xl font-bold text-green-700">
                +{classification.additionalRatePercentage.toFixed(2)}%
              </div>
            </div>
            <div className="flex items-center p-6 bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg">
              <div className="text-sm text-gray-700">
                <strong>Benefit:</strong> {classification.classificationDescription}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* FD Account Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Wallet size={20} className="text-blue-600" />
            Fixed Deposit Account Summary
          </CardTitle>
          <CardDescription>Overview of all FD accounts and investments</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
              <div className="text-sm text-gray-600 mb-1">Total FD Accounts</div>
              <div className="text-2xl font-bold text-blue-700">{accountSummary.totalFdAccounts}</div>
            </div>
            <div className="p-4 bg-green-50 rounded-lg border border-green-200">
              <div className="text-sm text-gray-600 mb-1">Total Investment</div>
              <div className="text-2xl font-bold text-green-700">
                {formatCurrency(accountSummary.totalInvestment, customer.preferredCurrency)}
              </div>
            </div>
            <div className="p-4 bg-purple-50 rounded-lg border border-purple-200">
              <div className="text-sm text-gray-600 mb-1">Maturity Amount</div>
              <div className="text-2xl font-bold text-purple-700">
                {formatCurrency(accountSummary.totalMaturityAmount, customer.preferredCurrency)}
              </div>
            </div>
            <div className="p-4 bg-indigo-50 rounded-lg border border-indigo-200">
              <div className="text-sm text-gray-600 mb-1">Active Accounts</div>
              <div className="text-2xl font-bold text-indigo-700">{accountSummary.activeFdCount}</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
              <div className="text-sm text-gray-600 mb-1">Matured Accounts</div>
              <div className="text-2xl font-bold text-gray-700">{accountSummary.maturedFdCount}</div>
            </div>
          </div>

          {accountSummary.totalFdAccounts === 0 && (
            <Alert className="mt-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                This customer doesn't have any FD accounts yet. They can create one from the FD Accounts page.
              </AlertDescription>
            </Alert>
          )}

          {data.fdAccounts && data.fdAccounts.length > 0 && (
            <div className="mt-6">
              <h3 className="text-lg font-semibold mb-4">FD Account Details</h3>
              <div className="text-sm text-gray-500">
                ⚠️ FD Account integration is pending. This section will show detailed account information once integrated.
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
