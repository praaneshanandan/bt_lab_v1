import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Product } from '@/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  Calendar, 
  DollarSign, 
  Percent, 
  Shield, 
  CheckCircle,
  Info,
  ArrowRight,
  Eye
} from 'lucide-react';

interface Props {
  products: Product[];
  loading: boolean;
}

export default function CustomerProductsView({ products, loading }: Props) {
  const navigate = useNavigate();
  const [filterType, setFilterType] = useState<string>('ALL');

  const filteredProducts = products.filter(product => {
    if (filterType === 'ALL') return true;
    return product.productType === filterType;
  });

  const getProductTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      'FIXED_DEPOSIT': 'bg-blue-100 text-blue-800',
      'RECURRING_DEPOSIT': 'bg-green-100 text-green-800',
      'SAVINGS': 'bg-purple-100 text-purple-800',
      'LOAN': 'bg-orange-100 text-orange-800',
    };
    return colors[type] || 'bg-gray-100 text-gray-800';
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const handleApply = (product: Product) => {
    navigate(`/accounts/create?productId=${product.productId}&productCode=${product.productCode}`);
  };

  const handleViewDetails = (product: Product) => {
    navigate(`/products/${product.productId}`);
  };

  if (loading) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Filter Tabs */}
      <div className="flex gap-2 flex-wrap">
        {['ALL', 'FIXED_DEPOSIT', 'RECURRING_DEPOSIT', 'SAVINGS'].map((type) => (
          <Button
            key={type}
            variant={filterType === type ? 'default' : 'outline'}
            onClick={() => setFilterType(type)}
            size="sm"
          >
            {type.replace('_', ' ')}
          </Button>
        ))}
      </div>

      {/* Products Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredProducts.length === 0 ? (
          <div className="col-span-full text-center py-12">
            <Info className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <p className="text-muted-foreground">No products available at the moment</p>
          </div>
        ) : (
          filteredProducts.map((product) => (
            <Card key={product.productId} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-xl">{product.productName}</CardTitle>
                    <CardDescription className="mt-1">{product.productCode}</CardDescription>
                  </div>
                  <Badge className={getProductTypeColor(product.productType)}>
                    {product.productType.replace('_', ' ')}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Key Highlights */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-blue-50 p-3 rounded-lg">
                    <div className="flex items-center gap-2 text-blue-700 mb-1">
                      <Percent className="h-4 w-4" />
                      <span className="text-xs font-medium">Interest Rate</span>
                    </div>
                    <p className="text-2xl font-bold text-blue-900">
                      {product.baseInterestRate}%
                    </p>
                  </div>
                  
                  <div className="bg-green-50 p-3 rounded-lg">
                    <div className="flex items-center gap-2 text-green-700 mb-1">
                      <DollarSign className="h-4 w-4" />
                      <span className="text-xs font-medium">Min Amount</span>
                    </div>
                    <p className="text-lg font-bold text-green-900">
                      {formatCurrency(product.minAmount)}
                    </p>
                  </div>
                </div>

                {/* Term Range */}
                <div className="flex items-center gap-2 text-sm text-muted-foreground bg-gray-50 p-3 rounded-lg">
                  <Calendar className="h-4 w-4" />
                  <span>
                    <strong>Term:</strong> {product.minTermMonths} - {product.maxTermMonths} months
                  </span>
                </div>

                {/* Features */}
                <div className="space-y-2">
                  {product.prematureWithdrawalAllowed && (
                    <div className="flex items-center gap-2 text-sm text-green-700">
                      <CheckCircle className="h-4 w-4" />
                      <span>Premature withdrawal allowed</span>
                    </div>
                  )}
                  {product.autoRenewalAllowed && (
                    <div className="flex items-center gap-2 text-sm text-green-700">
                      <CheckCircle className="h-4 w-4" />
                      <span>Auto-renewal available</span>
                    </div>
                  )}
                  {product.nomineeAllowed && (
                    <div className="flex items-center gap-2 text-sm text-green-700">
                      <CheckCircle className="h-4 w-4" />
                      <span>Nominee facility</span>
                    </div>
                  )}
                </div>

                {/* Description */}
                {product.description && (
                  <p className="text-sm text-muted-foreground line-clamp-2">
                    {product.description}
                  </p>
                )}

                {/* Actions */}
                <div className="flex gap-2 pt-2">
                  <Button 
                    onClick={() => handleViewDetails(product)}
                    variant="outline"
                    className="flex-1"
                  >
                    <Eye className="mr-2 h-4 w-4" />
                    Details
                  </Button>
                  <Button 
                    onClick={() => handleApply(product)}
                    className="flex-1 bg-blue-600 hover:bg-blue-700"
                  >
                    Apply Now
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Info Section */}
      <Card className="bg-blue-50 border-blue-200">
        <CardContent className="p-6">
          <div className="flex items-start gap-4">
            <Shield className="h-6 w-6 text-blue-600 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-blue-900 mb-2">Safe and Secure Investment</h3>
              <p className="text-sm text-blue-800">
                All our fixed deposit products are insured and backed by the bank's commitment to financial security. 
                Your investments are protected and guaranteed returns are assured.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

