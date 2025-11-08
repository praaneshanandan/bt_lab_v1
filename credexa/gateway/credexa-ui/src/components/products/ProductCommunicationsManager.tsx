import React, { useState, useEffect } from 'react';
import { productApi } from '../../services/api';
import { Plus, Edit2, Trash2, X } from 'lucide-react';

interface ProductCommunication {
  id: number;
  productId: number;
  communicationType: string;
  eventType: string;
  templateName: string;
  subject?: string;
  messageContent: string;
  channel: string;
  recipientType: string;
  timing: string;
  priority: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ProductCommunicationsManagerProps {
  productId: number;
  isAdmin: boolean;
}

const ProductCommunicationsManager: React.FC<ProductCommunicationsManagerProps> = ({ productId, isAdmin }) => {
  const [communications, setCommunications] = useState<ProductCommunication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingComm, setEditingComm] = useState<ProductCommunication | null>(null);
  const [formData, setFormData] = useState({
    communicationType: 'EMAIL',
    eventType: 'ACCOUNT_OPENING',
    templateName: '',
    subject: '',
    messageContent: '',
    channel: 'EMAIL',
    recipientType: 'CUSTOMER',
    timing: 'IMMEDIATE',
    priority: 'MEDIUM',
    active: true,
  });

  useEffect(() => {
    fetchCommunications();
  }, [productId]);

  const fetchCommunications = async () => {
    try {
      setLoading(true);
      const response = await productApi.getProductCommunications(productId);
      setCommunications(response.data?.data || []);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch communications');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingComm) {
        await productApi.updateProductCommunication(editingComm.id, formData);
      } else {
        await productApi.addProductCommunication(productId, formData);
      }

      await fetchCommunications();
      resetForm();
      setShowModal(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save communication');
    }
  };

  const handleDelete = async (commId: number) => {
    if (!window.confirm('Are you sure you want to delete this communication configuration?')) return;
    
    try {
      await productApi.deleteProductCommunication(commId);
      await fetchCommunications();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete communication');
    }
  };

  const handleEdit = (comm: ProductCommunication) => {
    setEditingComm(comm);
    setFormData({
      communicationType: comm.communicationType,
      eventType: comm.eventType,
      templateName: comm.templateName,
      subject: comm.subject || '',
      messageContent: comm.messageContent,
      channel: comm.channel,
      recipientType: comm.recipientType,
      timing: comm.timing,
      priority: comm.priority,
      active: comm.active,
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      communicationType: 'EMAIL',
      eventType: 'ACCOUNT_OPENING',
      templateName: '',
      subject: '',
      messageContent: '',
      channel: 'EMAIL',
      recipientType: 'CUSTOMER',
      timing: 'IMMEDIATE',
      priority: 'MEDIUM',
      active: true,
    });
    setEditingComm(null);
  };

  if (loading) {
    return <div className="text-center py-8">Loading communications...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-xl font-semibold">Customer Communications</h3>
        {isAdmin && (
          <button
            onClick={() => {
              resetForm();
              setShowModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus size={20} />
            Add Communication
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
              <th className="px-4 py-3 text-left text-sm font-semibold">Event Type</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Template</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Channel</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Recipient</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Timing</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Priority</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Status</th>
              {isAdmin && <th className="px-4 py-3 text-left text-sm font-semibold">Actions</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {communications.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 8 : 7} className="px-4 py-8 text-center text-gray-500">
                  No communications configured for this product
                </td>
              </tr>
            ) : (
              communications.map((comm) => (
                <tr key={comm.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{comm.eventType.replace(/_/g, ' ')}</td>
                  <td className="px-4 py-3 text-sm font-medium">{comm.templateName}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className="px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-800">
                      {comm.channel}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">{comm.recipientType}</td>
                  <td className="px-4 py-3 text-sm">{comm.timing}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${
                      comm.priority === 'HIGH' ? 'bg-red-100 text-red-800' :
                      comm.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-gray-100 text-gray-800'
                    }`}>
                      {comm.priority}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${comm.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {comm.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td className="px-4 py-3 text-sm">
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(comm)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(comm.id)}
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
                {editingComm ? 'Edit Communication' : 'Add New Communication'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Event Type</label>
                  <select
                    value={formData.eventType}
                    onChange={(e) => setFormData({ ...formData, eventType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="ACCOUNT_OPENING">Account Opening</option>
                    <option value="ACCOUNT_ACTIVATION">Account Activation</option>
                    <option value="DEPOSIT">Deposit</option>
                    <option value="WITHDRAWAL">Withdrawal</option>
                    <option value="INTEREST_CREDIT">Interest Credit</option>
                    <option value="MATURITY">Maturity</option>
                    <option value="MATURITY_REMINDER">Maturity Reminder</option>
                    <option value="PREMATURE_CLOSURE">Premature Closure</option>
                    <option value="RENEWAL">Renewal</option>
                    <option value="STATEMENT">Statement</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Communication Type</label>
                  <select
                    value={formData.communicationType}
                    onChange={(e) => setFormData({ ...formData, communicationType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="EMAIL">Email</option>
                    <option value="SMS">SMS</option>
                    <option value="PUSH_NOTIFICATION">Push Notification</option>
                    <option value="IN_APP">In-App Message</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Template Name</label>
                  <input
                    type="text"
                    value={formData.templateName}
                    onChange={(e) => setFormData({ ...formData, templateName: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Channel</label>
                  <select
                    value={formData.channel}
                    onChange={(e) => setFormData({ ...formData, channel: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="EMAIL">Email</option>
                    <option value="SMS">SMS</option>
                    <option value="PUSH">Push</option>
                    <option value="IN_APP">In-App</option>
                    <option value="WHATSAPP">WhatsApp</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Subject (for emails)</label>
                <input
                  type="text"
                  value={formData.subject}
                  onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Message Content</label>
                <textarea
                  value={formData.messageContent}
                  onChange={(e) => setFormData({ ...formData, messageContent: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  rows={4}
                  placeholder="Use placeholders: {{customerName}}, {{accountNumber}}, {{amount}}, {{maturityDate}}"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">
                  Available placeholders: customerName, accountNumber, amount, maturityDate, interestRate
                </p>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Recipient Type</label>
                  <select
                    value={formData.recipientType}
                    onChange={(e) => setFormData({ ...formData, recipientType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="CUSTOMER">Customer</option>
                    <option value="NOMINEE">Nominee</option>
                    <option value="JOINT_HOLDER">Joint Holder</option>
                    <option value="ALL">All</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Timing</label>
                  <select
                    value={formData.timing}
                    onChange={(e) => setFormData({ ...formData, timing: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="IMMEDIATE">Immediate</option>
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="BEFORE_EVENT">Before Event</option>
                    <option value="AFTER_EVENT">After Event</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Priority</label>
                  <select
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    required
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>

              <div>
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
                  {editingComm ? 'Update' : 'Create'}
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

export default ProductCommunicationsManager;
