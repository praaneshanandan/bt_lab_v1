import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productApi } from '@/services/api';
import type { Product } from '@/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  ArrowLeft, 
  Calendar, 
  DollarSign, 
  Percent, 
  CheckCircle, 
  XCircle,
  Info,
  CreditCard,
  Loader2
} from 'lucide-react';
import ProductRolesManager from '@/components/products/ProductRolesManager';
import ProductChargesManager from '@/components/products/ProductChargesManager';
import ProductCommunicationsManager from '@/components/products/ProductCommunicationsManager';
import InterestRateCalculator from '@/components/products/InterestRateCalculator';

export default function ProductDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'rates' | 'roles' | 'charges' | 'communications'>('overview');
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [isAdmin, setIsAdmin] = useState(false);
  
  // Get user roles from localStorage (stored as JSON array)
  useEffect(() => {
    const getUserRoles = (): string[] => {
      try {
        const rolesString = localStorage.getItem('userRoles');
        console.log('Raw userRoles from localStorage:', rolesString);
        
        if (!rolesString) {
          console.warn('No userRoles found in localStorage');
          return [];
        }
        
        const roles = JSON.parse(rolesString);
        console.log('Parsed roles:', roles);
        
        if (!Array.isArray(roles)) {
          console.warn('userRoles is not an array:', roles);
          return [];
        }
        
        return roles;
      } catch (error) {
        console.error('Error parsing user roles:', error);
        return [];
      }
    };
    
    const roles = getUserRoles();
    // Check for both "ADMIN"/"MANAGER" and "ROLE_ADMIN"/"ROLE_MANAGER" formats
    const adminStatus = roles.some(role => 
      role === 'ADMIN' || 
      role === 'MANAGER' || 
      role === 'ROLE_ADMIN' || 
      role === 'ROLE_MANAGER'
    );
    
    console.log('ProductDetails - User Roles:', roles);
    console.log('ProductDetails - Is Admin:', adminStatus);
    
    setUserRoles(roles);
    setIsAdmin(adminStatus);
  }, []);

  useEffect(() => {
    if (id) {
      fetchProductDetails();
    }
  }, [id]);

  const fetchProductDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await productApi.getProductById(Number(id));
      const productData = response?.data?.data;
      setProduct(productData || null);
    } catch (err: any) {
      console.error('Error fetching product details:', err);
      setError(err.response?.data?.message || 'Failed to load product details');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const handleApply = () => {
    if (product) {
      navigate(`/accounts/create?productId=${product.productId}&productCode=${product.productCode}`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="container mx-auto p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-6">
            <div className="flex items-center gap-3">
              <XCircle className="h-6 w-6 text-red-600" />
              <div>
                <h3 className="font-semibold text-red-900">Error Loading Product</h3>
                <p className="text-sm text-red-700">{error || 'Product not found'}</p>
              </div>
            </div>
            <Button 
              onClick={() => navigate('/products')} 
              variant="outline" 
              className="mt-4"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Products
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      'ACTIVE': 'bg-green-100 text-green-800',
      'INACTIVE': 'bg-gray-100 text-gray-800',
      'DRAFT': 'bg-yellow-100 text-yellow-800',
      'SUSPENDED': 'bg-orange-100 text-orange-800',
      'CLOSED': 'bg-red-100 text-red-800',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button 
            onClick={() => navigate('/products')} 
            variant="outline"
            size="sm"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Button>
          <div>
            <h1 className="text-3xl font-bold">{product.productName}</h1>
            <p className="text-gray-600">{product.productCode}</p>
          </div>
        </div>
        <Badge className={getStatusColor(product.status)}>
          {product.status}
        </Badge>
      </div>

      {/* Key Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-2">
              <Percent className="h-6 w-6 text-blue-600" />
              <span className="text-sm font-medium text-blue-900">Interest Rate</span>
            </div>
            <p className="text-4xl font-bold text-blue-900">{product.baseInterestRate}%</p>
            <p className="text-xs text-blue-700 mt-1">
              {product.interestCalculationMethod} • {product.interestPayoutFrequency}
            </p>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-br from-green-50 to-green-100 border-green-200">
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-2">
              <DollarSign className="h-6 w-6 text-green-600" />
              <span className="text-sm font-medium text-green-900">Investment Range</span>
            </div>
            <p className="text-2xl font-bold text-green-900">
              {formatCurrency(product.minAmount)}
            </p>
            <p className="text-sm text-green-700">
              to {formatCurrency(product.maxAmount)}
            </p>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
          <CardContent className="p-6">
            <div className="flex items-center gap-3 mb-2">
              <Calendar className="h-6 w-6 text-purple-600" />
              <span className="text-sm font-medium text-purple-900">Term Range</span>
            </div>
            <p className="text-2xl font-bold text-purple-900">
              {product.minTermMonths} - {product.maxTermMonths}
            </p>
            <p className="text-sm text-purple-700">months</p>
          </CardContent>
        </Card>
      </div>

      {/* Description */}
      {product.description && (
        <Card>
          <CardHeader>
            <CardTitle>About This Product</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-gray-700">{product.description}</p>
          </CardContent>
        </Card>
      )}

      {/* Features & Benefits */}
      <Card>
        <CardHeader>
          <CardTitle>Features & Benefits</CardTitle>
          <CardDescription>What this product offers</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[
              { label: 'Premature Withdrawal', value: product.prematureWithdrawalAllowed },
              { label: 'Partial Withdrawal', value: product.partialWithdrawalAllowed },
              { label: 'Loan Against Deposit', value: product.loanAgainstDepositAllowed },
              { label: 'Auto Renewal', value: product.autoRenewalAllowed },
              { label: 'Nominee Facility', value: product.nomineeAllowed },
              { label: 'Joint Account', value: product.jointAccountAllowed },
              { label: 'TDS Applicable', value: product.tdsApplicable },
            ].map((feature) => (
              <div 
                key={feature.label} 
                className="flex items-center gap-3 p-3 rounded-lg bg-gray-50"
              >
                {feature.value ? (
                  <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0" />
                ) : (
                  <XCircle className="h-5 w-5 text-gray-400 flex-shrink-0" />
                )}
                <span className={feature.value ? 'text-gray-900' : 'text-gray-500'}>
                  {feature.label}
                </span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Additional Details */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Financial Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Currency</span>
              <span className="font-semibold">{product.currencyCode}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Min Balance Required</span>
              <span className="font-semibold">{formatCurrency(product.minBalanceRequired || 0)}</span>
            </div>
            {product.tdsApplicable && (
              <div className="flex justify-between py-2 border-b">
                <span className="text-gray-600">TDS Rate</span>
                <span className="font-semibold">{product.tdsRate}%</span>
              </div>
            )}
            <div className="flex justify-between py-2">
              <span className="text-gray-600">Bank/Branch Code</span>
              <span className="font-semibold">{product.bankBranchCode}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Product Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Product Type</span>
              <span className="font-semibold">{product.productType.replace(/_/g, ' ')}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Effective Date</span>
              <span className="font-semibold">
                {new Date(product.effectiveDate).toLocaleDateString('en-IN')}
              </span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-gray-600">Status</span>
              <Badge className={getStatusColor(product.status)}>
                {product.status}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Apply Button - Only show for non-admin users */}
      {product.status === 'ACTIVE' && !isAdmin && (
        <Card className="bg-blue-50 border-blue-200">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-start gap-4">
                <CreditCard className="h-8 w-8 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-blue-900 text-lg mb-1">
                    Ready to Invest?
                  </h3>
                  <p className="text-sm text-blue-800">
                    Open a new account with this product and start earning attractive returns today.
                  </p>
                </div>
              </div>
              <Button 
                onClick={handleApply}
                size="lg"
                className="bg-blue-600 hover:bg-blue-700"
              >
                Apply Now
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Advanced Management Tabs */}
      <Card>
        <CardHeader>
          <CardTitle>
            Product Management 
            {/* Debug indicator - remove in production */}
            <span className="ml-2 text-xs font-normal text-gray-500">
              (Role: {userRoles.join(', ') || 'None'} | Admin: {isAdmin ? 'YES' : 'NO'})
            </span>
          </CardTitle>
          <CardDescription>
            {isAdmin ? 'Manage product configuration, rates, and communications' : 'View product details and calculate interest rates'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {/* Tab Navigation */}
          <div className="flex gap-2 mb-6 border-b">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-2 font-medium transition-colors ${
                activeTab === 'overview'
                  ? 'text-blue-600 border-b-2 border-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('rates')}
              className={`px-4 py-2 font-medium transition-colors ${
                activeTab === 'rates'
                  ? 'text-blue-600 border-b-2 border-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Interest Calculator
            </button>
            {isAdmin && (
              <>
                <button
                  onClick={() => setActiveTab('roles')}
                  className={`px-4 py-2 font-medium transition-colors ${
                    activeTab === 'roles'
                      ? 'text-blue-600 border-b-2 border-blue-600'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Roles
                </button>
                <button
                  onClick={() => setActiveTab('charges')}
                  className={`px-4 py-2 font-medium transition-colors ${
                    activeTab === 'charges'
                      ? 'text-blue-600 border-b-2 border-blue-600'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Charges
                </button>
                <button
                  onClick={() => setActiveTab('communications')}
                  className={`px-4 py-2 font-medium transition-colors ${
                    activeTab === 'communications'
                      ? 'text-blue-600 border-b-2 border-blue-600'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Communications
                </button>
              </>
            )}
          </div>

          {/* Tab Content */}
          <div className="mt-6">
            {activeTab === 'overview' && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold">Product Summary</h3>
                <p className="text-gray-700">{product.description}</p>
                <div className="grid grid-cols-2 gap-4 mt-4">
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-600">Base Interest Rate</p>
                    <p className="text-2xl font-bold text-gray-900">{product.baseInterestRate}%</p>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-600">Interest Calculation</p>
                    <p className="text-lg font-semibold text-gray-900">{product.interestCalculationMethod}</p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'rates' && (
              <InterestRateCalculator productId={product.productId} />
            )}

            {activeTab === 'roles' && isAdmin && (
              <ProductRolesManager productId={product.productId} isAdmin={isAdmin} />
            )}

            {activeTab === 'charges' && isAdmin && (
              <ProductChargesManager productId={product.productId} isAdmin={isAdmin} />
            )}

            {activeTab === 'communications' && isAdmin && (
              <ProductCommunicationsManager productId={product.productId} isAdmin={isAdmin} />
            )}
          </div>
        </CardContent>
      </Card>

      {/* Info Notice */}
      <Card className="border-gray-200 bg-gray-50">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <Info className="h-5 w-5 text-gray-600 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-gray-700">
              <strong>Note:</strong> Interest rates and terms are subject to change. 
              Please verify all details before opening an account. 
              For more information, contact your nearest branch or customer service.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
