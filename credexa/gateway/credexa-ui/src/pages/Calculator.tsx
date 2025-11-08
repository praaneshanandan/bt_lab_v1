import { useState, useEffect } from 'react';
import { calculatorApi, productApi } from '@/services/api';
import type { CalculationResponse, Product, MonthlyBreakdown } from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Calculator, TrendingUp, Calendar, Percent, Info, Download, BarChart3 } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { toast } from 'sonner';
import { Checkbox } from '@/components/ui/checkbox';

// Customer classification options
const CUSTOMER_CLASSIFICATIONS = [
  { value: 'SENIOR_CITIZEN', label: 'Senior Citizen', rate: 0.25 },
  { value: 'SUPER_SENIOR', label: 'Super Senior Citizen', rate: 0.25 },
  { value: 'EMPLOYEE', label: 'Bank Employee', rate: 0.25 },
  { value: 'PREMIUM', label: 'Premium Customer', rate: 0.25 },
  { value: 'VIP', label: 'VIP Customer', rate: 0.25 },
  { value: 'WOMEN', label: 'Women Customer', rate: 0.25 },
  { value: 'DEFENSE', label: 'Defense Personnel', rate: 0.25 },
  { value: 'GOVERNMENT', label: 'Government Employee', rate: 0.25 },
];

export default function FDCalculator() {
  // Form states
  const [standaloneForm, setStandaloneForm] = useState({
    principalAmount: '',
    interestRate: '',
    tenure: '',
    tenureUnit: 'MONTHS' as 'DAYS' | 'MONTHS' | 'YEARS',
    calculationType: 'COMPOUND' as 'SIMPLE' | 'COMPOUND',
    compoundingFrequency: 'QUARTERLY' as 'DAILY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUALLY' | 'ANNUALLY',
    tdsRate: '10',
    customerClassifications: [] as string[],
  });

  const [productForm, setProductForm] = useState({
    productId: '',
    principalAmount: '',
    tenure: '',
    tenureUnit: 'MONTHS' as 'DAYS' | 'MONTHS' | 'YEARS',
    applyTds: true,
    customerClassifications: [] as string[],
  });

  const [products, setProducts] = useState<Product[]>([]);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [result, setResult] = useState<CalculationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('standalone');

  // Load active products on mount
  useEffect(() => {
    loadProducts();
  }, []);

  // Ensure products is always an array
  useEffect(() => {
    if (!Array.isArray(products)) {
      setProducts([]);
    }
  }, [products]);

  const loadProducts = async () => {
    try {
      setLoadingProducts(true);
      const response = await productApi.getActiveProducts();
      
      console.log('🔍 Product API Response:', response);
      console.log('🔍 Response Data:', response.data);
      
      // Handle different response structures
      let productsList: Product[] = [];
      
      if (response.data?.data) {
        productsList = Array.isArray(response.data.data) ? response.data.data : [];
      } else if (Array.isArray(response.data)) {
        productsList = response.data;
      } else {
        productsList = [];
      }
      
      console.log('🔍 Parsed Products:', productsList);
      console.log('🔍 First Product:', productsList[0]);
      
      setProducts(productsList);
    } catch (err) {
      console.error('❌ Error loading products:', err);
      setProducts([]); // Ensure products is always an array
      toast.error('Failed to load products. Please try again later.');
    } finally {
      setLoadingProducts(false);
    }
  };

  // Load product details when selected
  const handleProductSelect = async (productId: string) => {
    setProductForm({ ...productForm, productId });
    try {
      const response = await productApi.getProductById(parseInt(productId));
      // Handle different response structures
      if (response.data?.data) {
        setSelectedProduct(response.data.data);
      } else if (response.data) {
        setSelectedProduct(response.data);
      } else {
        setSelectedProduct(null);
      }
    } catch (err) {
      console.error('Error loading product details:', err);
      setSelectedProduct(null);
      toast.error('Failed to load product details');
    }
  };

  const handleStandaloneCalculate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    try {
      const requestData = {
        principalAmount: parseFloat(standaloneForm.principalAmount),
        interestRate: parseFloat(standaloneForm.interestRate),
        tenure: parseInt(standaloneForm.tenure),
        tenureUnit: standaloneForm.tenureUnit,
        calculationType: standaloneForm.calculationType,
        compoundingFrequency: standaloneForm.calculationType === 'COMPOUND' ? standaloneForm.compoundingFrequency : undefined,
        tdsRate: parseFloat(standaloneForm.tdsRate),
        customerClassifications: standaloneForm.customerClassifications,
      };

      const response = await calculatorApi.calculateStandalone(requestData);
      
      if (response.data?.data) {
        setResult(response.data.data);
        toast.success('Calculation completed successfully!');
      } else {
        throw new Error('Invalid response format');
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to calculate. Please check your inputs.';
      setError(errorMsg);
      toast.error(errorMsg);
      console.error('Error calculating:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleProductCalculate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    try {
      const requestData = {
        productId: parseInt(productForm.productId),
        principalAmount: parseFloat(productForm.principalAmount),
        tenure: parseInt(productForm.tenure),
        tenureUnit: productForm.tenureUnit,
        applyTds: productForm.applyTds,
        customerClassifications: productForm.customerClassifications,
      };

      const response = await calculatorApi.calculateWithProduct(requestData);
      
      if (response.data?.data) {
        setResult(response.data.data);
        toast.success('Calculation completed successfully!');
      } else {
        throw new Error('Invalid response format');
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to calculate. Please check your inputs.';
      setError(errorMsg);
      toast.error(errorMsg);
      console.error('Error calculating:', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleClassification = (classification: string, isStandalone: boolean) => {
    if (isStandalone) {
      const current = standaloneForm.customerClassifications;
      const updated = current.includes(classification)
        ? current.filter(c => c !== classification)
        : [...current, classification].slice(0, 8); // Max 8 classifications
      setStandaloneForm({ ...standaloneForm, customerClassifications: updated });
    } else {
      const current = productForm.customerClassifications;
      const updated = current.includes(classification)
        ? current.filter(c => c !== classification)
        : [...current, classification].slice(0, 8);
      setProductForm({ ...productForm, customerClassifications: updated });
    }
  };

  const calculateAdditionalRate = (classifications: string[]) => {
    return Math.min(classifications.length * 0.25, 2.0);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Calculator className="h-8 w-8 text-blue-600" />
            FD Calculator
          </h1>
          <p className="text-gray-600 mt-2">Calculate your fixed deposit returns with advanced features</p>
        </div>
        <Badge variant="outline" className="text-sm">
          <Info className="h-3 w-3 mr-1" />
          All calculations are indicative
        </Badge>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        {/* Left Panel - Calculation Forms */}
        <div className="xl:col-span-2">
          <Card className="sticky top-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calculator size={20} />
                Calculate Returns
              </CardTitle>
              <CardDescription>Choose your calculation method</CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs value={activeTab} onValueChange={setActiveTab}>
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="standalone">Custom Calculation</TabsTrigger>
                  <TabsTrigger value="product">With Product</TabsTrigger>
                </TabsList>

                {/* Standalone Calculation Tab */}
                <TabsContent value="standalone" className="space-y-4 mt-4">
                  <form onSubmit={handleStandaloneCalculate} className="space-y-4">
                    {/* Principal Amount */}
                    <div>
                      <Label htmlFor="principal">Principal Amount (₹)</Label>
                      <Input
                        id="principal"
                        type="number"
                        step="0.01"
                        min="1000"
                        max="100000000"
                        value={standaloneForm.principalAmount}
                        onChange={(e) => setStandaloneForm({ ...standaloneForm, principalAmount: e.target.value })}
                        required
                        placeholder="e.g., 100000"
                      />
                      <p className="text-xs text-gray-500 mt-1">Min: ₹1,000 | Max: ₹10 Crore</p>
                    </div>

                    {/* Interest Rate */}
                    <div>
                      <Label htmlFor="rate">Annual Interest Rate (%)</Label>
                      <Input
                        id="rate"
                        type="number"
                        step="0.01"
                        min="0.1"
                        max="20"
                        value={standaloneForm.interestRate}
                        onChange={(e) => setStandaloneForm({ ...standaloneForm, interestRate: e.target.value })}
                        required
                        placeholder="e.g., 6.5"
                      />
                      <p className="text-xs text-gray-500 mt-1">Range: 0.1% - 20%</p>
                    </div>

                    {/* Tenure */}
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <Label htmlFor="tenure">Tenure</Label>
                        <Input
                          id="tenure"
                          type="number"
                          min="1"
                          value={standaloneForm.tenure}
                          onChange={(e) => setStandaloneForm({ ...standaloneForm, tenure: e.target.value })}
                          required
                          placeholder="12"
                        />
                      </div>
                      <div>
                        <Label htmlFor="tenureUnit">Unit</Label>
                        <Select
                          value={standaloneForm.tenureUnit}
                          onValueChange={(value: any) => setStandaloneForm({ ...standaloneForm, tenureUnit: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="DAYS">Days</SelectItem>
                            <SelectItem value="MONTHS">Months</SelectItem>
                            <SelectItem value="YEARS">Years</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    {/* Calculation Type */}
                    <div>
                      <Label htmlFor="calcType">Calculation Type</Label>
                      <Select
                        value={standaloneForm.calculationType}
                        onValueChange={(value: any) => setStandaloneForm({ ...standaloneForm, calculationType: value })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="SIMPLE">Simple Interest</SelectItem>
                          <SelectItem value="COMPOUND">Compound Interest</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    {/* Compounding Frequency (only for compound) */}
                    {standaloneForm.calculationType === 'COMPOUND' && (
                      <div>
                        <Label htmlFor="frequency">Compounding Frequency</Label>
                        <Select
                          value={standaloneForm.compoundingFrequency}
                          onValueChange={(value: any) => setStandaloneForm({ ...standaloneForm, compoundingFrequency: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="DAILY">Daily</SelectItem>
                            <SelectItem value="MONTHLY">Monthly</SelectItem>
                            <SelectItem value="QUARTERLY">Quarterly</SelectItem>
                            <SelectItem value="SEMI_ANNUALLY">Semi-Annually</SelectItem>
                            <SelectItem value="ANNUALLY">Annually</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    )}

                    {/* TDS Rate */}
                    <div>
                      <Label htmlFor="tdsRate">TDS Rate (%)</Label>
                      <Input
                        id="tdsRate"
                        type="number"
                        step="0.01"
                        min="0"
                        max="30"
                        value={standaloneForm.tdsRate}
                        onChange={(e) => setStandaloneForm({ ...standaloneForm, tdsRate: e.target.value })}
                        placeholder="10"
                      />
                      <p className="text-xs text-gray-500 mt-1">Standard: 10% | Max: 30%</p>
                    </div>

                    {/* Customer Classifications */}
                    <div>
                      <Label>Customer Classifications (Max 8)</Label>
                      <div className="mt-2 space-y-2 max-h-48 overflow-y-auto border rounded-md p-3">
                        {CUSTOMER_CLASSIFICATIONS.map((classification) => (
                          <div key={classification.value} className="flex items-center space-x-2">
                            <Checkbox
                              id={`standalone-${classification.value}`}
                              checked={standaloneForm.customerClassifications.includes(classification.value)}
                              onCheckedChange={() => toggleClassification(classification.value, true)}
                              disabled={
                                !standaloneForm.customerClassifications.includes(classification.value) &&
                                standaloneForm.customerClassifications.length >= 8
                              }
                            />
                            <label
                              htmlFor={`standalone-${classification.value}`}
                              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 flex-1 cursor-pointer"
                            >
                              {classification.label}
                              <span className="text-xs text-green-600 ml-2">+{classification.rate}%</span>
                            </label>
                          </div>
                        ))}
                      </div>
                      {standaloneForm.customerClassifications.length > 0 && (
                        <p className="text-xs text-green-600 mt-2">
                          Additional Rate: +{calculateAdditionalRate(standaloneForm.customerClassifications)}% 
                          ({standaloneForm.customerClassifications.length} {standaloneForm.customerClassifications.length === 1 ? 'classification' : 'classifications'})
                        </p>
                      )}
                    </div>

                    <Button type="submit" className="w-full" disabled={loading}>
                      {loading ? (
                        <>
                          <span className="animate-spin mr-2">⏳</span>
                          Calculating...
                        </>
                      ) : (
                        <>
                          <Calculator className="mr-2 h-4 w-4" />
                          Calculate
                        </>
                      )}
                    </Button>
                  </form>
                </TabsContent>

                {/* Product-Based Calculation Tab */}
                <TabsContent value="product" className="space-y-4 mt-4">
                  <form onSubmit={handleProductCalculate} className="space-y-4">
                    {/* Product Selection */}
                    <div>
                      <Label htmlFor="product">Select Product</Label>
                      <Select
                        value={productForm.productId}
                        onValueChange={handleProductSelect}
                        disabled={loadingProducts}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder={loadingProducts ? "Loading products..." : "Choose a product"} />
                        </SelectTrigger>
                        <SelectContent>
                          {Array.isArray(products) && products.length > 0 ? (
                            products
                              .filter(product => product && (product.id || product.productId))
                              .map((product) => {
                                const productIdValue = product.id || product.productId || 0;
                                const productKey = `${product.productCode}-${productIdValue}`;
                                return (
                                  <SelectItem key={productKey} value={productIdValue.toString()}>
                                    {product.productName || 'Unnamed Product'} ({product.productCode || 'N/A'})
                                  </SelectItem>
                                );
                              })
                          ) : (
                            <SelectItem value="none" disabled>
                              No products available
                            </SelectItem>
                          )}
                        </SelectContent>
                      </Select>
                    </div>

                    {/* Product Details Preview */}
                    {selectedProduct && (
                      <Alert>
                        <Info className="h-4 w-4" />
                        <AlertDescription className="text-sm space-y-1">
                          <div><strong>Rate:</strong> {selectedProduct.baseInterestRate}% p.a.</div>
                          <div><strong>Min Amount:</strong> {formatCurrency(selectedProduct.minAmount)}</div>
                          <div><strong>Term:</strong> {selectedProduct.minTermMonths}-{selectedProduct.maxTermMonths} months</div>
                        </AlertDescription>
                      </Alert>
                    )}

                    {/* Principal Amount */}
                    <div>
                      <Label htmlFor="productPrincipal">Principal Amount (₹)</Label>
                      <Input
                        id="productPrincipal"
                        type="number"
                        step="0.01"
                        min={selectedProduct?.minAmount || 1000}
                        max={selectedProduct?.maxAmount || 100000000}
                        value={productForm.principalAmount}
                        onChange={(e) => setProductForm({ ...productForm, principalAmount: e.target.value })}
                        required
                        placeholder="e.g., 100000"
                      />
                      {selectedProduct && (
                        <p className="text-xs text-gray-500 mt-1">
                          Min: {formatCurrency(selectedProduct.minAmount)} | Max: {formatCurrency(selectedProduct.maxAmount)}
                        </p>
                      )}
                    </div>

                    {/* Tenure */}
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <Label htmlFor="productTenure">Tenure</Label>
                        <Input
                          id="productTenure"
                          type="number"
                          min="1"
                          value={productForm.tenure}
                          onChange={(e) => setProductForm({ ...productForm, tenure: e.target.value })}
                          required
                          placeholder="12"
                        />
                      </div>
                      <div>
                        <Label htmlFor="productTenureUnit">Unit</Label>
                        <Select
                          value={productForm.tenureUnit}
                          onValueChange={(value: any) => setProductForm({ ...productForm, tenureUnit: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="DAYS">Days</SelectItem>
                            <SelectItem value="MONTHS">Months</SelectItem>
                            <SelectItem value="YEARS">Years</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    {/* Apply TDS */}
                    <div className="flex items-center space-x-2">
                      <Checkbox
                        id="applyTds"
                        checked={productForm.applyTds}
                        onCheckedChange={(checked) => setProductForm({ ...productForm, applyTds: checked as boolean })}
                      />
                      <label
                        htmlFor="applyTds"
                        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                      >
                        Apply TDS (Tax Deducted at Source)
                      </label>
                    </div>

                    {/* Customer Classifications */}
                    <div>
                      <Label>Customer Classifications (Max 8)</Label>
                      <div className="mt-2 space-y-2 max-h-48 overflow-y-auto border rounded-md p-3">
                        {CUSTOMER_CLASSIFICATIONS.map((classification) => (
                          <div key={classification.value} className="flex items-center space-x-2">
                            <Checkbox
                              id={`product-${classification.value}`}
                              checked={productForm.customerClassifications.includes(classification.value)}
                              onCheckedChange={() => toggleClassification(classification.value, false)}
                              disabled={
                                !productForm.customerClassifications.includes(classification.value) &&
                                productForm.customerClassifications.length >= 8
                              }
                            />
                            <label
                              htmlFor={`product-${classification.value}`}
                              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 flex-1 cursor-pointer"
                            >
                              {classification.label}
                              <span className="text-xs text-green-600 ml-2">+{classification.rate}%</span>
                            </label>
                          </div>
                        ))}
                      </div>
                      {productForm.customerClassifications.length > 0 && (
                        <p className="text-xs text-green-600 mt-2">
                          Additional Rate: +{calculateAdditionalRate(productForm.customerClassifications)}%
                        </p>
                      )}
                    </div>

                    <Button type="submit" className="w-full" disabled={loading || !productForm.productId}>
                      {loading ? (
                        <>
                          <span className="animate-spin mr-2">⏳</span>
                          Calculating...
                        </>
                      ) : (
                        <>
                          <Calculator className="mr-2 h-4 w-4" />
                          Calculate
                        </>
                      )}
                    </Button>
                  </form>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        </div>

        {/* Right Panel - Results */}
        <div className="xl:col-span-3 space-y-6">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {!result ? (
            <Card>
              <CardContent className="pt-6">
                <div className="text-center text-gray-500 py-12">
                  <TrendingUp className="h-16 w-16 mx-auto mb-4 text-gray-300" />
                  <p className="text-lg font-medium">No calculation yet</p>
                  <p className="text-sm mt-2">Fill in the form and calculate to see detailed results</p>
                </div>
              </CardContent>
            </Card>
          ) : (
            <>
              {/* Main Results Card */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <TrendingUp size={20} />
                    Calculation Results
                    {result.productName && (
                      <Badge variant="outline" className="ml-auto">
                        {result.productCode}
                      </Badge>
                    )}
                  </CardTitle>
                  <CardDescription>
                    {result.productName || 'Custom FD Calculation'} • {result.calculationType} Interest
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  {/* Key Metrics */}
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="p-4 bg-blue-50 rounded-lg border border-blue-100">
                      <div className="text-sm text-gray-600 mb-1">Principal Amount</div>
                      <div className="text-2xl font-bold text-blue-600">
                        {formatCurrency(result.principalAmount)}
                      </div>
                    </div>

                    <div className="p-4 bg-green-50 rounded-lg border border-green-100">
                      <div className="text-sm text-gray-600 mb-1">Interest Earned</div>
                      <div className="text-2xl font-bold text-green-600">
                        {formatCurrency(result.interestEarned)}
                      </div>
                      {result.tdsAmount && result.tdsAmount > 0 && (
                        <div className="text-xs text-gray-500 mt-1">
                          TDS: {formatCurrency(result.tdsAmount)} @ {result.tdsRate}%
                        </div>
                      )}
                    </div>

                    <div className="p-4 bg-purple-50 rounded-lg border border-purple-100">
                      <div className="text-sm text-gray-600 mb-1">Maturity Amount</div>
                      <div className="text-2xl font-bold text-purple-600">
                        {formatCurrency(result.maturityAmount)}
                      </div>
                      {result.netInterest !== result.interestEarned && (
                        <div className="text-xs text-gray-500 mt-1">
                          Net Interest: {formatCurrency(result.netInterest)}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Detailed Information */}
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t">
                    <div>
                      <div className="text-sm text-gray-600 flex items-center gap-1">
                        <Percent className="h-3 w-3" />
                        Interest Rate
                      </div>
                      <div className="font-semibold mt-1">{result.interestRate}% p.a.</div>
                      {result.baseInterestRate && result.additionalInterestRate && result.additionalInterestRate > 0 && (
                        <div className="text-xs text-gray-500">
                          Base: {result.baseInterestRate}% + {result.additionalInterestRate}%
                        </div>
                      )}
                    </div>

                    <div>
                      <div className="text-sm text-gray-600 flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        Tenure
                      </div>
                      <div className="font-semibold mt-1">
                        {result.tenure} {result.tenureUnit}
                      </div>
                      <div className="text-xs text-gray-500">
                        {result.tenureInYears.toFixed(2)} years
                      </div>
                    </div>

                    <div>
                      <div className="text-sm text-gray-600">Start Date</div>
                      <div className="font-semibold mt-1 text-sm">{formatDate(result.startDate)}</div>
                    </div>

                    <div>
                      <div className="text-sm text-gray-600">Maturity Date</div>
                      <div className="font-semibold mt-1 text-sm">{formatDate(result.maturityDate)}</div>
                    </div>
                  </div>

                  {/* Customer Classifications */}
                  {result.customerClassifications && result.customerClassifications.length > 0 && (
                    <div className="pt-4 border-t">
                      <Label className="text-sm">Applied Classifications</Label>
                      <div className="flex flex-wrap gap-2 mt-2">
                        {result.customerClassifications.map((classification) => (
                          <Badge key={classification} variant="secondary">
                            {classification.replace('_', ' ')}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Compounding Details */}
                  {result.compoundingFrequency && (
                    <div className="flex items-center justify-between pt-4 border-t text-sm">
                      <span className="text-gray-600">Compounding Frequency:</span>
                      <Badge variant="outline">{result.compoundingFrequency}</Badge>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Monthly Breakdown */}
              {result.monthlyBreakdown && result.monthlyBreakdown.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <BarChart3 size={20} />
                      Monthly Breakdown
                    </CardTitle>
                    <CardDescription>
                      Month-by-month interest calculation
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="border-b">
                            <th className="text-left p-2">Month</th>
                            <th className="text-left p-2">Date</th>
                            <th className="text-right p-2">Opening Balance</th>
                            <th className="text-right p-2">Interest</th>
                            <th className="text-right p-2">Closing Balance</th>
                            <th className="text-right p-2">Cumulative</th>
                          </tr>
                        </thead>
                        <tbody>
                          {result.monthlyBreakdown.map((month: MonthlyBreakdown) => (
                            <tr key={month.month} className="border-b hover:bg-gray-50">
                              <td className="p-2">{month.month}</td>
                              <td className="p-2">{formatDate(month.date)}</td>
                              <td className="text-right p-2">{formatCurrency(month.openingBalance)}</td>
                              <td className="text-right p-2 text-green-600">{formatCurrency(month.interestEarned)}</td>
                              <td className="text-right p-2 font-medium">{formatCurrency(month.closingBalance)}</td>
                              <td className="text-right p-2 text-blue-600">{formatCurrency(month.cumulativeInterest)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Action Buttons */}
              <div className="flex gap-3">
                <Button variant="outline" className="flex-1">
                  <Download className="mr-2 h-4 w-4" />
                  Download Report
                </Button>
                <Button variant="outline" className="flex-1" onClick={() => setResult(null)}>
                  Calculate Again
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
