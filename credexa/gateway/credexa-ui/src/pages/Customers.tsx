import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { customerApi, authApi } from '@/services/api';
import { isManagerOrAdmin } from '@/utils/auth';
import type { Customer, AdminCreateCustomerRequest, AdminCreateCustomerResponse } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Plus, Search, Eye, Filter, Download, X, Copy, CheckCircle } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useDuplicateDetection } from '@/hooks/useDuplicateDetection';
import { DuplicateWarning } from '@/components/DuplicateWarning';

export default function Customers() {
  const navigate = useNavigate();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [classificationFilter, setClassificationFilter] = useState<string>('ALL');
  const [kycStatusFilter, setKycStatusFilter] = useState<string>('ALL');
  const [activeStatusFilter, setActiveStatusFilter] = useState<string>('ALL');
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [createdCustomerInfo, setCreatedCustomerInfo] = useState<AdminCreateCustomerResponse | null>(null);
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [passwordCopied, setPasswordCopied] = useState(false);
  const [newCustomer, setNewCustomer] = useState<AdminCreateCustomerRequest>({
    username: '',
    email: '',
    mobileNumber: '',
    preferredLanguage: 'en',
    preferredCurrency: 'INR',
    fullName: '',
    panNumber: '',
    aadharNumber: '',
    dateOfBirth: '',
    gender: 'MALE',
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
  
  // Duplicate detection for customer creation
  const { warnings: duplicateWarnings, hasDuplicates } = useDuplicateDetection({
    customers,
    email: newCustomer.email,
    mobileNumber: newCustomer.mobileNumber,
    panNumber: newCustomer.panNumber,
    aadharNumber: newCustomer.aadharNumber,
  });
  
  // Check authorization on mount
  useEffect(() => {
    if (!isManagerOrAdmin()) {
      // Redirect regular customers to their profile page
      toast.info('Redirecting to your profile...');
      navigate('/profile');
      return;
    }
    fetchCustomers();
  }, [navigate]);

  const fetchCustomers = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await customerApi.getAllCustomers();
      setCustomers(response.data);
    } catch (err) {
      setError('Failed to fetch customers. Please ensure all services are running.');
      console.error('Error fetching customers:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Prevent submission if there are duplicate warnings
    if (hasDuplicates) {
      toast.error('Cannot create customer: Duplicate information detected. Please review the warnings.');
      return;
    }
    
    try {
      // Use admin endpoint which creates both user account and customer profile
      const response = await authApi.adminCreateCustomer(newCustomer);
      const customerInfo = response.data.data;
      
      setCreatedCustomerInfo(customerInfo);
      setShowPasswordDialog(true);
      setIsCreateDialogOpen(false);
      
      // Reset form
      setNewCustomer({
        username: '',
        email: '',
        mobileNumber: '',
        preferredLanguage: 'en',
        preferredCurrency: 'INR',
        fullName: '',
        panNumber: '',
        aadharNumber: '',
        dateOfBirth: '',
        gender: 'MALE',
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
      
      toast.success('Customer account created successfully!');
      fetchCustomers();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      const errorMsg = error.response?.data?.message || 'Failed to create customer';
      toast.error(errorMsg);
      console.error('Error creating customer:', err);
    }
  };

  const copyPasswordToClipboard = () => {
    if (createdCustomerInfo?.temporaryPassword) {
      navigator.clipboard.writeText(createdCustomerInfo.temporaryPassword);
      setPasswordCopied(true);
      toast.success('Password copied to clipboard!');
      setTimeout(() => setPasswordCopied(false), 3000);
    }
  };

  // Comprehensive filtering logic
  const filteredCustomers = customers.filter((customer) => {
    // Search term filter (name, email, mobile, PAN, Aadhar)
    const searchLower = searchTerm.toLowerCase();
    const matchesSearch = 
      customer.fullName.toLowerCase().includes(searchLower) ||
      customer.email?.toLowerCase().includes(searchLower) ||
      customer.mobileNumber?.includes(searchTerm) ||
      customer.panNumber?.toLowerCase().includes(searchLower) ||
      customer.aadharNumber?.includes(searchTerm) ||
      customer.username?.toLowerCase().includes(searchLower);

    // Classification filter
    const matchesClassification = 
      classificationFilter === 'ALL' || customer.classification === classificationFilter;

    // KYC status filter
    const matchesKycStatus = 
      kycStatusFilter === 'ALL' || customer.kycStatus === kycStatusFilter;

    // Active status filter
    const matchesActiveStatus = 
      activeStatusFilter === 'ALL' ||
      (activeStatusFilter === 'ACTIVE' && customer.isActive) ||
      (activeStatusFilter === 'INACTIVE' && !customer.isActive);

    return matchesSearch && matchesClassification && matchesKycStatus && matchesActiveStatus;
  });

  // Export to CSV function
  const exportToCSV = () => {
    const headers = ['ID', 'Name', 'Email', 'Mobile', 'Classification', 'KYC Status', 'Status', 'Date of Birth', 'City'];
    const rows = filteredCustomers.map(c => [
      c.id,
      c.fullName,
      c.email,
      c.mobileNumber,
      c.classification,
      c.kycStatus,
      c.isActive ? 'Active' : 'Inactive',
      c.dateOfBirth,
      c.city || 'N/A'
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `customers_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
    toast.success('Customer data exported successfully!');
  };

  // Clear all filters
  const clearFilters = () => {
    setSearchTerm('');
    setClassificationFilter('ALL');
    setKycStatusFilter('ALL');
    setActiveStatusFilter('ALL');
    toast.info('Filters cleared');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-gray-600">Loading customers...</div>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Customers</h1>
          <p className="text-gray-600 mt-2">Manage customer accounts</p>
        </div>
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button className="flex items-center gap-2">
              <Plus size={16} />
              Add Customer
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Create New Customer</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleCreateCustomer} className="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
              <Alert className="bg-blue-50 border-blue-200">
                <AlertDescription className="text-sm text-blue-800">
                  <strong>Admin Customer Creation:</strong> A user account and customer profile will be created. 
                  A temporary password will be generated for the customer to login.
                </AlertDescription>
              </Alert>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="username">Username *</Label>
                  <Input
                    id="username"
                    value={newCustomer.username}
                    onChange={(e) => setNewCustomer({ ...newCustomer, username: e.target.value })}
                    minLength={3}
                    maxLength={100}
                    placeholder="Unique username for login"
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="fullName">Full Name *</Label>
                  <Input
                    id="fullName"
                    value={newCustomer.fullName}
                    onChange={(e) => setNewCustomer({ ...newCustomer, fullName: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="mobileNumber">Mobile Number *</Label>
                  <Input
                    id="mobileNumber"
                    value={newCustomer.mobileNumber}
                    onChange={(e) => setNewCustomer({ ...newCustomer, mobileNumber: e.target.value })}
                    pattern="[0-9]{10}"
                    placeholder="10 digits"
                    required
                  />
                  {duplicateWarnings.mobile && (
                    <DuplicateWarning message={duplicateWarnings.mobile} field="mobile" />
                  )}
                </div>
                <div>
                  <Label htmlFor="email">Email *</Label>
                  <Input
                    id="email"
                    type="email"
                    value={newCustomer.email}
                    onChange={(e) => setNewCustomer({ ...newCustomer, email: e.target.value })}
                    required
                  />
                  {duplicateWarnings.email && (
                    <DuplicateWarning message={duplicateWarnings.email} field="email" />
                  )}
                </div>
                <div>
                  <Label htmlFor="dateOfBirth">Date of Birth *</Label>
                  <Input
                    id="dateOfBirth"
                    type="date"
                    value={newCustomer.dateOfBirth}
                    onChange={(e) => setNewCustomer({ ...newCustomer, dateOfBirth: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="gender">Gender *</Label>
                  <select
                    id="gender"
                    value={newCustomer.gender}
                    onChange={(e) => setNewCustomer({ ...newCustomer, gender: e.target.value as 'MALE' | 'FEMALE' | 'OTHER' })}
                    className="w-full h-10 px-3 border border-gray-300 rounded-md"
                    required
                  >
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div>
                  <Label htmlFor="classification">Classification *</Label>
                  <select
                    id="classification"
                    value={newCustomer.classification}
                    onChange={(e) => setNewCustomer({ ...newCustomer, classification: e.target.value as 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR' })}
                    className="w-full h-10 px-3 border border-gray-300 rounded-md"
                    required
                  >
                    <option value="REGULAR">Regular</option>
                    <option value="PREMIUM">Premium</option>
                    <option value="VIP">VIP</option>
                    <option value="SENIOR_CITIZEN">Senior Citizen</option>
                    <option value="SUPER_SENIOR">Super Senior (80+)</option>
                  </select>
                </div>
                <div>
                  <Label htmlFor="panNumber">PAN Number</Label>
                  <Input
                    id="panNumber"
                    value={newCustomer.panNumber}
                    onChange={(e) => setNewCustomer({ ...newCustomer, panNumber: e.target.value })}
                    pattern="[A-Z]{5}[0-9]{4}[A-Z]"
                    placeholder="ABCDE1234F"
                  />
                  {duplicateWarnings.pan && (
                    <DuplicateWarning message={duplicateWarnings.pan} field="pan" />
                  )}
                </div>
                <div>
                  <Label htmlFor="aadharNumber">Aadhar Number</Label>
                  <Input
                    id="aadharNumber"
                    value={newCustomer.aadharNumber}
                    onChange={(e) => setNewCustomer({ ...newCustomer, aadharNumber: e.target.value })}
                    pattern="[0-9]{12}"
                    placeholder="12 digits"
                  />
                  {duplicateWarnings.aadhar && (
                    <DuplicateWarning message={duplicateWarnings.aadhar} field="aadhar" />
                  )}
                </div>
              </div>
              
              <div>
                <Label htmlFor="addressLine1">Address Line 1 *</Label>
                <Input
                  id="addressLine1"
                  value={newCustomer.addressLine1}
                  onChange={(e) => setNewCustomer({ ...newCustomer, addressLine1: e.target.value })}
                  required
                />
              </div>
              <div>
                <Label htmlFor="addressLine2">Address Line 2</Label>
                <Input
                  id="addressLine2"
                  value={newCustomer.addressLine2}
                  onChange={(e) => setNewCustomer({ ...newCustomer, addressLine2: e.target.value })}
                />
              </div>
              
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label htmlFor="city">City *</Label>
                  <Input
                    id="city"
                    value={newCustomer.city}
                    onChange={(e) => setNewCustomer({ ...newCustomer, city: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="state">State *</Label>
                  <Input
                    id="state"
                    value={newCustomer.state}
                    onChange={(e) => setNewCustomer({ ...newCustomer, state: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="pincode">Pincode *</Label>
                  <Input
                    id="pincode"
                    value={newCustomer.pincode}
                    onChange={(e) => setNewCustomer({ ...newCustomer, pincode: e.target.value })}
                    pattern="[0-9]{6}"
                    placeholder="6 digits"
                    required
                  />
                </div>
              </div>
              
              <div>
                <Label htmlFor="country">Country *</Label>
                <Input
                  id="country"
                  value={newCustomer.country}
                  onChange={(e) => setNewCustomer({ ...newCustomer, country: e.target.value })}
                  required
                />
              </div>

              {hasDuplicates && (
                <Alert variant="destructive" className="mt-4">
                  <AlertDescription>
                    <strong>Cannot create customer:</strong> Duplicate information detected. Please review the warnings above and correct the information before submitting.
                  </AlertDescription>
                </Alert>
              )}

              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button type="button" variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={hasDuplicates}>
                  Create Customer
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
        
        {/* Password Dialog - Shows temporary password after customer creation */}
        <Dialog open={showPasswordDialog} onOpenChange={setShowPasswordDialog}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2 text-green-600">
                <CheckCircle size={24} />
                Customer Created Successfully!
              </DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <Alert className="bg-green-50 border-green-200">
                <AlertDescription>
                  <div className="space-y-2">
                    <p className="font-medium text-green-800">Account Details:</p>
                    <div className="text-sm space-y-1">
                      <p><strong>Username:</strong> {createdCustomerInfo?.username}</p>
                      <p><strong>Customer ID:</strong> {createdCustomerInfo?.customerId}</p>
                      <p><strong>Full Name:</strong> {createdCustomerInfo?.fullName}</p>
                      <p><strong>Classification:</strong> {createdCustomerInfo?.classification}</p>
                    </div>
                  </div>
                </AlertDescription>
              </Alert>

              <Alert className="bg-yellow-50 border-yellow-300">
                <AlertDescription>
                  <div className="space-y-2">
                    <p className="font-bold text-yellow-900">⚠️ Temporary Password:</p>
                    <div className="flex items-center gap-2 bg-white p-3 rounded border border-yellow-300">
                      <code className="flex-1 font-mono text-lg font-bold text-yellow-900">
                        {createdCustomerInfo?.temporaryPassword}
                      </code>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={copyPasswordToClipboard}
                        className="flex items-center gap-1"
                      >
                        {passwordCopied ? (
                          <>
                            <CheckCircle size={14} className="text-green-600" />
                            Copied
                          </>
                        ) : (
                          <>
                            <Copy size={14} />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                    <p className="text-xs text-yellow-800">
                      <strong>Important:</strong> Please save this password and share it with the customer securely. 
                      They will need to change it on first login.
                    </p>
                  </div>
                </AlertDescription>
              </Alert>

              <div className="flex justify-end">
                <Button onClick={() => setShowPasswordDialog(false)}>
                  Done
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Customer List ({filteredCustomers.length} customers)</CardTitle>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={clearFilters} className="flex items-center gap-1">
                <X size={14} />
                Clear Filters
              </Button>
              <Button variant="outline" size="sm" onClick={exportToCSV} className="flex items-center gap-1">
                <Download size={14} />
                Export CSV
              </Button>
            </div>
          </div>

          {/* Search and Filters */}
          <div className="space-y-4 mt-4">
            {/* Search Bar */}
            <div className="flex items-center gap-2">
              <Search className="text-gray-400" size={20} />
              <Input
                placeholder="Search by name, email, mobile, PAN, Aadhar, username..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex-1"
              />
            </div>

            {/* Filter Dropdowns */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <Label className="text-sm flex items-center gap-1 mb-2">
                  <Filter size={14} />
                  Classification
                </Label>
                <Select value={classificationFilter} onValueChange={setClassificationFilter}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Classifications</SelectItem>
                    <SelectItem value="REGULAR">Regular</SelectItem>
                    <SelectItem value="PREMIUM">Premium</SelectItem>
                    <SelectItem value="VIP">VIP</SelectItem>
                    <SelectItem value="SENIOR_CITIZEN">Senior Citizen</SelectItem>
                    <SelectItem value="SUPER_SENIOR">Super Senior</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm flex items-center gap-1 mb-2">
                  <Filter size={14} />
                  KYC Status
                </Label>
                <Select value={kycStatusFilter} onValueChange={setKycStatusFilter}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All KYC Status</SelectItem>
                    <SelectItem value="PENDING">Pending</SelectItem>
                    <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                    <SelectItem value="VERIFIED">Verified</SelectItem>
                    <SelectItem value="REJECTED">Rejected</SelectItem>
                    <SelectItem value="EXPIRED">Expired</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-sm flex items-center gap-1 mb-2">
                  <Filter size={14} />
                  Account Status
                </Label>
                <Select value={activeStatusFilter} onValueChange={setActiveStatusFilter}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Status</SelectItem>
                    <SelectItem value="ACTIVE">Active</SelectItem>
                    <SelectItem value="INACTIVE">Inactive</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Active Filters Display */}
            {(searchTerm || classificationFilter !== 'ALL' || kycStatusFilter !== 'ALL' || activeStatusFilter !== 'ALL') && (
              <div className="flex items-center gap-2 text-sm">
                <span className="text-gray-600">Active filters:</span>
                {searchTerm && (
                  <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">
                    Search: "{searchTerm}"
                  </span>
                )}
                {classificationFilter !== 'ALL' && (
                  <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">
                    {classificationFilter.replace('_', ' ')}
                  </span>
                )}
                {kycStatusFilter !== 'ALL' && (
                  <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">
                    KYC: {kycStatusFilter}
                  </span>
                )}
                {activeStatusFilter !== 'ALL' && (
                  <span className="px-2 py-1 bg-gray-100 text-foreground rounded text-xs">
                    {activeStatusFilter}
                  </span>
                )}
              </div>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {filteredCustomers.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              No customers found
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Mobile</TableHead>
                  <TableHead>Classification</TableHead>
                  <TableHead>KYC Status</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCustomers.map((customer) => (
                  <TableRow key={customer.id}>
                    <TableCell>{customer.id}</TableCell>
                    <TableCell className="font-medium">{customer.fullName}</TableCell>
                    <TableCell>{customer.email}</TableCell>
                    <TableCell>{customer.mobileNumber}</TableCell>
                    <TableCell>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        customer.classification === 'VIP' ? 'bg-purple-100 text-purple-800' :
                        customer.classification === 'PREMIUM' ? 'bg-blue-100 text-blue-800' :
                        customer.classification === 'SENIOR_CITIZEN' ? 'bg-orange-100 text-orange-800' :
                        customer.classification === 'SUPER_SENIOR' ? 'bg-red-100 text-red-800' :
                        'bg-muted text-muted-foreground'
                      }`}>
                        {customer.classification.replace('_', ' ')}
                      </span>
                    </TableCell>
                    <TableCell>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        customer.kycStatus === 'VERIFIED' ? 'bg-green-100 text-green-800' :
                        customer.kycStatus === 'REJECTED' ? 'bg-red-100 text-red-800' :
                        customer.kycStatus === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>
                        {customer.kycStatus}
                      </span>
                    </TableCell>
                    <TableCell>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        customer.isActive 
                          ? 'bg-green-100 text-green-800'
                          : 'bg-muted text-muted-foreground'
                      }`}>
                        {customer.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/customers/${customer.id}/360-view`)}
                        className="flex items-center gap-1"
                      >
                        <Eye size={14} />
                        View 360°
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

