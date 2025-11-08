import React, { useState, useEffect } from 'react';
import { productApi } from '../../services/api';
import { Plus, Edit2, Trash2, X } from 'lucide-react';

interface ProductRole {
  id: number;
  productId: number;
  roleType: string;
  description: string;
  minAge?: number;
  maxAge?: number;
  kycRequired: boolean;
  approvalRequired: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ProductRolesManagerProps {
  productId: number;
  isAdmin: boolean;
}

const ProductRolesManager: React.FC<ProductRolesManagerProps> = ({ productId, isAdmin }) => {
  const [roles, setRoles] = useState<ProductRole[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingRole, setEditingRole] = useState<ProductRole | null>(null);
  const [formData, setFormData] = useState({
    roleType: 'PRIMARY_HOLDER',
    description: '',
    minAge: '',
    maxAge: '',
    kycRequired: true,
    approvalRequired: false,
    active: true,
  });

  useEffect(() => {
    fetchRoles();
  }, [productId]);

  const fetchRoles = async () => {
    try {
      setLoading(true);
      const response = await productApi.getProductRoles(productId);
      setRoles(response.data?.data || []);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch roles');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = {
        ...formData,
        minAge: formData.minAge ? parseInt(formData.minAge) : null,
        maxAge: formData.maxAge ? parseInt(formData.maxAge) : null,
      };

      if (editingRole) {
        await productApi.updateProductRole(editingRole.id, data);
      } else {
        await productApi.addProductRole(productId, data);
      }

      await fetchRoles();
      resetForm();
      setShowModal(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save role');
    }
  };

  const handleDelete = async (roleId: number) => {
    if (!window.confirm('Are you sure you want to delete this role?')) return;
    
    try {
      await productApi.deleteProductRole(roleId);
      await fetchRoles();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete role');
    }
  };

  const handleEdit = (role: ProductRole) => {
    setEditingRole(role);
    setFormData({
      roleType: role.roleType,
      description: role.description,
      minAge: role.minAge?.toString() || '',
      maxAge: role.maxAge?.toString() || '',
      kycRequired: role.kycRequired,
      approvalRequired: role.approvalRequired,
      active: role.active,
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      roleType: 'PRIMARY_HOLDER',
      description: '',
      minAge: '',
      maxAge: '',
      kycRequired: true,
      approvalRequired: false,
      active: true,
    });
    setEditingRole(null);
  };

  if (loading) {
    return <div className="text-center py-8">Loading roles...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-xl font-semibold">Product Roles</h3>
        {isAdmin && (
          <button
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus size={20} />
            Add Role
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
              <th className="px-4 py-3 text-left text-sm font-semibold">Role Type</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Description</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Age Range</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">KYC Required</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Approval Required</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Status</th>
              {isAdmin && <th className="px-4 py-3 text-left text-sm font-semibold">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {roles.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 7 : 6} className="px-4 py-8 text-center text-gray-500">
                  No roles configured for this product
                </td>
              </tr>
            ) : (
              roles.map((role) => (
                <tr key={role.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{role.roleType}</td>
                  <td className="px-4 py-3 text-sm">{role.description}</td>
                  <td className="px-4 py-3 text-sm">
                    {role.minAge || role.maxAge ? `${role.minAge || 0}-${role.maxAge || '∞'}` : 'Any'}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${role.kycRequired ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                      {role.kycRequired ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${role.approvalRequired ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-800'}`}>
                      {role.approvalRequired ? 'Yes' : 'No'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${role.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {role.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td className="px-4 py-3 text-sm">
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(role)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(role.id)}
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">
                {editingRole ? 'Edit Role' : 'Add New Role'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Role Type</label>
                <select
                  value={formData.roleType}
                  onChange={(e) => setFormData({ ...formData, roleType: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  required
                >
                  <option value="PRIMARY_HOLDER">Primary Holder</option>
                  <option value="JOINT_HOLDER">Joint Holder</option>
                  <option value="NOMINEE">Nominee</option>
                  <option value="GUARDIAN">Guardian</option>
                  <option value="POWER_OF_ATTORNEY">Power of Attorney</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  rows={3}
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Min Age</label>
                  <input
                    type="number"
                    value={formData.minAge}
                    onChange={(e) => setFormData({ ...formData, minAge: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    min="0"
                    max="120"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Max Age</label>
                  <input
                    type="number"
                    value={formData.maxAge}
                    onChange={(e) => setFormData({ ...formData, maxAge: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    min="0"
                    max="120"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.kycRequired}
                    onChange={(e) => setFormData({ ...formData, kycRequired: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">KYC Required</span>
                </label>

                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.approvalRequired}
                    onChange={(e) => setFormData({ ...formData, approvalRequired: e.target.checked })}
                    className="rounded"
                  />
                  <span className="text-sm">Approval Required</span>
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
                  {editingRole ? 'Update' : 'Create'}
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

export default ProductRolesManager;
