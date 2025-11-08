import { useState } from 'react';
import { toast } from 'sonner';
import { productApi } from '@/services/api';
import type { Product } from '@/types';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { 
  Plus, 
  Edit, 
  Trash2, 
  Search,
} from 'lucide-react';

interface Props {
  products: Product[];
  loading: boolean;
  onRefresh: () => void;
}

export default function AdminProductsView({ products, loading, onRefresh }: Props) {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [showDialog, setShowDialog] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [formData, setFormData] = useState<Partial<Product>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const filteredProducts = products.filter(product => {
    const matchesSearch = 
      product.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.productCode.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === 'ALL' || product.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const getStatusBadge = (status: string) => {
    const colors: Record<string, string> = {
      'ACTIVE': 'bg-green-100 text-green-800',
      'INACTIVE': 'bg-gray-100 text-gray-800',
      'DRAFT': 'bg-yellow-100 text-yellow-800',
      'SUSPENDED': 'bg-red-100 text-red-800',
      'CLOSED': 'bg-gray-100 text-gray-800',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  const handleCreate = () => {
    setSelectedProduct(null);
    setFormData({
      productType: 'FIXED_DEPOSIT',
      status: 'DRAFT',
      currencyCode: 'INR',
      minTermMonths: 6,
      maxTermMonths: 60,
      minAmount: 10000,
      maxAmount: 10000000,
      minBalanceRequired: 0,
      baseInterestRate: 6.5,
      interestCalculationMethod: 'SIMPLE',
      interestPayoutFrequency: 'MATURITY',
      prematureWithdrawalAllowed: true,
      partialWithdrawalAllowed: false,
      loanAgainstDepositAllowed: true,
      autoRenewalAllowed: true,
      nomineeAllowed: true,
      jointAccountAllowed: true,
      tdsApplicable: true,
      tdsRate: 10,
    });
    setShowDialog(true);
  };

  const handleEdit = (product: Product) => {
    setSelectedProduct(product);
    setFormData(product);
    setShowDialog(true);
  };

  const handleDelete = (product: Product) => {
    setSelectedProduct(product);
    setDeleteDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!selectedProduct) return;

    setIsSubmitting(true);
    try {
      await productApi.deleteProduct(selectedProduct.id);
      toast.success('Product deleted successfully');
      onRefresh();
      setDeleteDialogOpen(false);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete product');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      if (selectedProduct) {
        await productApi.updateProduct(selectedProduct.id, formData);
        toast.success('Product updated successfully');
      } else {
        await productApi.createProduct(formData);
        toast.success('Product created successfully');
      }
      onRefresh();
      setShowDialog(false);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to save product');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (loading) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Header with Search and Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search by product name or code..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="w-full md:w-48">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="INACTIVE">Inactive</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="SUSPENDED">Suspended</SelectItem>
              </SelectContent>
            </Select>
            <Button onClick={handleCreate}>
              <Plus className="mr-2 h-4 w-4" />
              Create Product
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Products Table */}
      <Card>
        <CardContent className="pt-6">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product Code</TableHead>
                  <TableHead>Product Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Interest Rate</TableHead>
                  <TableHead>Term Range</TableHead>
                  <TableHead>Amount Range</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredProducts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8 text-gray-500">
                      No products found
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredProducts.map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="font-medium">{product.productCode}</TableCell>
                      <TableCell>{product.productName}</TableCell>
                      <TableCell>
                        <span className="text-sm">{product.productType.replace('_', ' ')}</span>
                      </TableCell>
                      <TableCell>{product.baseInterestRate}%</TableCell>
                      <TableCell>
                        {product.minTermMonths} - {product.maxTermMonths} months
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          <div>{formatCurrency(product.minAmount)}</div>
                          <div className="text-gray-500">to {formatCurrency(product.maxAmount)}</div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusBadge(product.status)}>
                          {product.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleEdit(product)}
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDelete(product)}
                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={showDialog} onOpenChange={setShowDialog}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedProduct ? 'Edit Product' : 'Create New Product'}
            </DialogTitle>
            <DialogDescription>
              {selectedProduct ? 'Update product details' : 'Add a new product to the system'}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Information */}
            <div className="space-y-4">
              <h3 className="font-semibold">Basic Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="productCode">Product Code *</Label>
                  <Input
                    id="productCode"
                    value={formData.productCode || ''}
                    onChange={(e) => setFormData({...formData, productCode: e.target.value})}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="productName">Product Name *</Label>
                  <Input
                    id="productName"
                    value={formData.productName || ''}
                    onChange={(e) => setFormData({...formData, productName: e.target.value})}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="productType">Product Type *</Label>
                  <Select
                    value={formData.productType}
                    onValueChange={(value) => setFormData({...formData, productType: value as any})}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FIXED_DEPOSIT">Fixed Deposit</SelectItem>
                      <SelectItem value="RECURRING_DEPOSIT">Recurring Deposit</SelectItem>
                      <SelectItem value="SAVINGS">Savings</SelectItem>
                      <SelectItem value="LOAN">Loan</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="status">Status *</Label>
                  <Select
                    value={formData.status}
                    onValueChange={(value) => setFormData({...formData, status: value as any})}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DRAFT">Draft</SelectItem>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="INACTIVE">Inactive</SelectItem>
                      <SelectItem value="SUSPENDED">Suspended</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <textarea
                  id="description"
                  value={formData.description || ''}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setFormData({...formData, description: e.target.value})}
                  rows={3}
                  className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                />
              </div>
            </div>

            {/* Term and Amount Configuration */}
            <div className="space-y-4">
              <h3 className="font-semibold">Term & Amount Configuration</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="minTermMonths">Min Term (Months) *</Label>
                  <Input
                    id="minTermMonths"
                    type="number"
                    value={formData.minTermMonths || ''}
                    onChange={(e) => setFormData({...formData, minTermMonths: Number(e.target.value)})}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="maxTermMonths">Max Term (Months) *</Label>
                  <Input
                    id="maxTermMonths"
                    type="number"
                    value={formData.maxTermMonths || ''}
                    onChange={(e) => setFormData({...formData, maxTermMonths: Number(e.target.value)})}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="minAmount">Min Amount *</Label>
                  <Input
                    id="minAmount"
                    type="number"
                    value={formData.minAmount || ''}
                    onChange={(e) => setFormData({...formData, minAmount: Number(e.target.value)})}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="maxAmount">Max Amount *</Label>
                  <Input
                    id="maxAmount"
                    type="number"
                    value={formData.maxAmount || ''}
                    onChange={(e) => setFormData({...formData, maxAmount: Number(e.target.value)})}
                    required
                  />
                </div>
              </div>
            </div>

            {/* Interest Configuration */}
            <div className="space-y-4">
              <h3 className="font-semibold">Interest Configuration</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="baseInterestRate">Base Interest Rate (%) *</Label>
                  <Input
                    id="baseInterestRate"
                    type="number"
                    step="0.1"
                    value={formData.baseInterestRate || ''}
                    onChange={(e) => setFormData({...formData, baseInterestRate: Number(e.target.value)})}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="interestCalculationMethod">Calculation Method *</Label>
                  <Select
                    value={formData.interestCalculationMethod}
                    onValueChange={(value) => setFormData({...formData, interestCalculationMethod: value})}
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
              </div>

              <div className="space-y-2">
                <Label htmlFor="interestPayoutFrequency">Payout Frequency *</Label>
                <Select
                  value={formData.interestPayoutFrequency}
                  onValueChange={(value) => setFormData({...formData, interestPayoutFrequency: value})}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MONTHLY">Monthly</SelectItem>
                    <SelectItem value="QUARTERLY">Quarterly</SelectItem>
                    <SelectItem value="ANNUALLY">Annually</SelectItem>
                    <SelectItem value="MATURITY">At Maturity</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Features */}
            <div className="space-y-4">
              <h3 className="font-semibold">Features</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="prematureWithdrawalAllowed"
                    checked={formData.prematureWithdrawalAllowed}
                    onChange={(e) => setFormData({...formData, prematureWithdrawalAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="prematureWithdrawalAllowed">Premature Withdrawal</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="partialWithdrawalAllowed"
                    checked={formData.partialWithdrawalAllowed}
                    onChange={(e) => setFormData({...formData, partialWithdrawalAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="partialWithdrawalAllowed">Partial Withdrawal</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="loanAgainstDepositAllowed"
                    checked={formData.loanAgainstDepositAllowed}
                    onChange={(e) => setFormData({...formData, loanAgainstDepositAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="loanAgainstDepositAllowed">Loan Against Deposit</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="autoRenewalAllowed"
                    checked={formData.autoRenewalAllowed}
                    onChange={(e) => setFormData({...formData, autoRenewalAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="autoRenewalAllowed">Auto Renewal</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="nomineeAllowed"
                    checked={formData.nomineeAllowed}
                    onChange={(e) => setFormData({...formData, nomineeAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="nomineeAllowed">Nominee Allowed</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="jointAccountAllowed"
                    checked={formData.jointAccountAllowed}
                    onChange={(e) => setFormData({...formData, jointAccountAllowed: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="jointAccountAllowed">Joint Account</Label>
                </div>
              </div>
            </div>

            {/* Tax Configuration */}
            <div className="space-y-4">
              <h3 className="font-semibold">Tax Configuration</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="tdsApplicable"
                    checked={formData.tdsApplicable}
                    onChange={(e) => setFormData({...formData, tdsApplicable: e.target.checked})}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="tdsApplicable">TDS Applicable</Label>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="tdsRate">TDS Rate (%)</Label>
                  <Input
                    id="tdsRate"
                    type="number"
                    step="0.1"
                    value={formData.tdsRate || ''}
                    onChange={(e) => setFormData({...formData, tdsRate: Number(e.target.value)})}
                    disabled={!formData.tdsApplicable}
                  />
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setShowDialog(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Saving...' : (selectedProduct ? 'Update Product' : 'Create Product')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Product</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{selectedProduct?.productName}"? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={confirmDelete}
              disabled={isSubmitting}
              className="bg-red-600 hover:bg-red-700"
            >
              {isSubmitting ? 'Deleting...' : 'Delete Product'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
