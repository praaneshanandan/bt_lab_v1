import React, { useState, useEffect } from 'react';
import { productApi } from '../../services/api';
import { Plus, Edit2, Trash2, X } from 'lucide-react';

interface ProductCharge {
  id: number;
  productId: number;
  chargeType: string;
  chargeName: string;
  description: string;
  chargeAmount?: number;
  chargePercentage?: number;
  calculationMethod: string;
  frequency: string;
  applicableOn: string;
  minAmount?: number;
  maxAmount?: number;
  waiverAllowed: boolean;
  taxable: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ProductChargesManagerProps {
  productId: number;
  isAdmin: boolean;
}

const ProductChargesManager: React.FC<ProductChargesManagerProps> = ({ productId, isAdmin }) => {
  const [charges, setCharges] = useState<ProductCharge[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingCharge, setEditingCharge] = useState<ProductCharge | null>(null);
  const [formData, setFormData] = useState({
    chargeType: 'ACCOUNT_OPENING',
    chargeName: '',
    description: '',
    chargeAmount: '',
    chargePercentage: '',
    calculationMethod: 'FIXED',
    frequency: 'ONE_TIME',
    applicableOn: 'ACCOUNT_OPENING',
    minAmount: '',
    maxAmount: '',
    waiverAllowed: false,
    taxable: true,
    active: true,
  });

  useEffect(() => {
    fetchCharges();
  }, [productId]);

  const fetchCharges = async () => {
    try {
      setLoading(true);
      const response = await productApi.getProductCharges(productId);
      setCharges(response.data?.data || []);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch charges');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = {
        ...formData,
        chargeAmount: formData.chargeAmount ? parseFloat(formData.chargeAmount) : null,
        chargePercentage: formData.chargePercentage ? parseFloat(formData.chargePercentage) : null,
        minAmount: formData.minAmount ? parseFloat(formData.minAmount) : null,
        maxAmount: formData.maxAmount ? parseFloat(formData.maxAmount) : null,
      };

      if (editingCharge) {
        await productApi.updateProductCharge(editingCharge.id, data);
      } else {
        await productApi.addProductCharge(productId, data);
      }

      await fetchCharges();
      resetForm();
      setShowModal(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save charge');
    }
  };

  const handleDelete = async (chargeId: number) => {
    if (!window.confirm('Are you sure you want to delete this charge?')) return;
    
    try {
      await productApi.deleteProductCharge(chargeId);
      await fetchCharges();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete charge');
    }
  };

  const handleEdit = (charge: ProductCharge) => {
    setEditingCharge(charge);
    setFormData({
      chargeType: charge.chargeType,
      chargeName: charge.chargeName,
      description: charge.description,
      chargeAmount: charge.chargeAmount?.toString() || '',
      chargePercentage: charge.chargePercentage?.toString() || '',
      calculationMethod: charge.calculationMethod,
      frequency: charge.frequency,
      applicableOn: charge.applicableOn,
      minAmount: charge.minAmount?.toString() || '',
      maxAmount: charge.maxAmount?.toString() || '',
      waiverAllowed: charge.waiverAllowed,
      taxable: charge.taxable,
      active: charge.active,
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      chargeType: 'ACCOUNT_OPENING',
      chargeName: '',
      description: '',
      chargeAmount: '',
      chargePercentage: '',
      calculationMethod: 'FIXED',
      frequency: 'ONE_TIME',
      applicableOn: 'ACCOUNT_OPENING',
      minAmount: '',
      maxAmount: '',
      waiverAllowed: false,
      taxable: true,
      active: true,
    });
    setEditingCharge(null);
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
    }).format(amount);
  };

  if (loading) {
    return <div className="text-center py-8">Loading charges...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-xl font-semibold">Product Charges</h3>
        {isAdmin && (
          <button
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus size={20} />
            Add Charge
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full bg-white border border-gray-200 rounded-lg">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold">Charge Type</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Name</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Amount</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Method</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Frequency</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Taxable</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Status</th>
              {isAdmin && <th className="px-4 py-3 text-left text-sm font-semibold">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {charges.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 8 : 7} className="px-4 py-8 text-center text-gray-500">
                  No charges configured for this product
                </td>
              </tr>
            ) : (
              charges.map((charge) => (
                <tr key={charge.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{charge.chargeType.replace(/_/g, ' ')}</td>
                  <td className="px-4 py-3 text-sm font-medium">{charge.chargeName}</td>
                  <td className="px-4 py-3 text-sm">
                    {charge.chargeAmount ? formatCurrency(charge.chargeAmount) : 
                     charge.chargePercentage ? `${charge.chargePercentage}%` : '-'}
                  </td>
                  <td className="px-4 py-3 text-sm">{charge.calculationMethod}</td>
                  <td className="px-4 py-3 text-sm">{charge.frequency.replace(/_/g, ' ')}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${charge.taxable ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-800'}`}>
                      {charge.taxable ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${charge.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {charge.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td className="px-4 py-3 text-sm">
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(charge)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(charge.id)}
                          className="text-red-600 hover:text-red-800"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl my-8">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">
                {editingCharge ? 'Edit Charge' : 'Add New Charge'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Charge Type</label>
                  <select
                    value={formData.chargeType}
                    onChange={(e) => setFormData({ ...formData, chargeType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="ACCOUNT_OPENING">Account Opening</option>
                    <option value="ACCOUNT_MAINTENANCE">Account Maintenance</option>
                    <option value="PREMATURE_WITHDRAWAL">Premature Withdrawal</option>
                    <option value="TRANSACTION">Transaction</option>
                    <option value="STATEMENT">Statement</option>
                    <option value="CHEQUE_BOOK">Cheque Book</option>
                    <option value="PENALTY">Penalty</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Charge Name</label>
                  <input
                    type="text"
                    value={formData.chargeName}
                    onChange={(e) => setFormData({ ...formData, chargeName: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  rows={2}
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Calculation Method</label>
                  <select
                    value={formData.calculationMethod}
                    onChange={(e) => setFormData({ ...formData, calculationMethod: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="FIXED">Fixed Amount</option>
                    <option value="PERCENTAGE">Percentage</option>
                    <option value="TIERED">Tiered</option>
                    <option value="CUSTOM">Custom</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Frequency</label>
                  <select
                    value={formData.frequency}
                    onChange={(e) => setFormData({ ...formData, frequency: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="ONE_TIME">One Time</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="QUARTERLY">Quarterly</option>
                    <option value="ANNUALLY">Annually</option>
                    <option value="PER_TRANSACTION">Per Transaction</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Charge Amount (₹)</label>
                  <input
                    type="number"
                    value={formData.chargeAmount}
                    onChange={(e) => setFormData({ ...formData, chargeAmount: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    step="0.01"
                    min="0"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Charge Percentage (%)</label>
                  <input
                    type="number"
                    value={formData.chargePercentage}
                    onChange={(e) => setFormData({ ...formData, chargePercentage: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    step="0.01"
                    min="0"
                    max="100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Min Amount (₹)</label>
                  <input
                    type="number"
                    value={formData.minAmount}
                    onChange={(e) => setFormData({ ...formData, minAmount: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    step="0.01"
                    min="0"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Max Amount (₹)</label>
                  <input
                    type="number"
                    value={formData.maxAmount}
                    onChange={(e) => setFormData({ ...formData, maxAmount: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    step="0.01"
                    min="0"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Applicable On</label>
                <select
                  value={formData.applicableOn}
                  onChange={(e) => setFormData({ ...formData, applicableOn: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  required
                >
                  <option value="ACCOUNT_OPENING">Account Opening</option>
                  <option value="MATURITY">Maturity</option>
                  <option value="PREMATURE_CLOSURE">Premature Closure</option>
                  <option value="TRANSACTION">Transaction</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="ANNUALLY">Annually</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.waiverAllowed}
                    onChange={(e) => setFormData({ ...formData, waiverAllowed: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">Waiver Allowed</span>
                </label>

                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.taxable}
                    onChange={(e) => setFormData({ ...formData, taxable: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">Taxable</span>
                </label>

                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">Active</span>
                </label>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                >
                  {editingCharge ? 'Update' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductChargesManager;
