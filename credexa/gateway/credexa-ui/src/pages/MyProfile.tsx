import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { customerApi } from '@/services/api';
import type { Customer, UpdateCustomerRequest, CreateCustomerRequest } from '@/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { ProfileCompletion } from '@/components/ProfileCompletion';
import { User, Edit2, Save, X, Loader2, Plus, Shield } from 'lucide-react';
import { isManagerOrAdmin, decodeToken, getUserRoles } from '@/utils/auth';

export default function MyProfile() {
  const [profile, setProfile] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const isAdmin = isManagerOrAdmin();
  
  const [createForm, setCreateForm] = useState<CreateCustomerRequest>({
    fullName: '',
    mobileNumber: '',
    email: '',
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
    preferredLanguage: 'en',
    preferredCurrency: 'INR',
    emailNotifications: true,
    smsNotifications: true,
  });

  const [editForm, setEditForm] = useState<UpdateCustomerRequest>({});

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Admin users don't have customer profiles, skip fetching
      if (isAdmin) {
        setLoading(false);
        return;
      }
      
      const response = await customerApi.getOwnProfile();
      setProfile(response.data);
      // Initialize edit form with current values
      setEditForm({
        fullName: response.data.fullName,
        mobileNumber: response.data.mobileNumber,
        email: response.data.email,
        panNumber: response.data.panNumber,
        aadharNumber: response.data.aadharNumber,
        dateOfBirth: response.data.dateOfBirth,
        gender: response.data.gender,
        addressLine1: response.data.addressLine1,
        addressLine2: response.data.addressLine2,
        city: response.data.city,
        state: response.data.state,
        pincode: response.data.pincode,
        country: response.data.country,
        accountNumber: response.data.accountNumber,
        ifscCode: response.data.ifscCode,
        preferredLanguage: response.data.preferredLanguage,
        preferredCurrency: response.data.preferredCurrency,
        emailNotifications: response.data.emailNotifications,
        smsNotifications: response.data.smsNotifications,
      });
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to fetch profile. You may need to create your customer profile first.';
      setError(errorMsg);
      console.error('Error fetching profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleCancel = () => {
    setIsEditing(false);
    // Reset form to current profile values
    if (profile) {
      setEditForm({
        fullName: profile.fullName,
        mobileNumber: profile.mobileNumber,
        email: profile.email,
        panNumber: profile.panNumber,
        aadharNumber: profile.aadharNumber,
        dateOfBirth: profile.dateOfBirth,
        gender: profile.gender,
        addressLine1: profile.addressLine1,
        addressLine2: profile.addressLine2,
        city: profile.city,
        state: profile.state,
        pincode: profile.pincode,
        country: profile.country,
        accountNumber: profile.accountNumber,
        ifscCode: profile.ifscCode,
        preferredLanguage: profile.preferredLanguage,
        preferredCurrency: profile.preferredCurrency,
        emailNotifications: profile.emailNotifications,
        smsNotifications: profile.smsNotifications,
      });
    }
  };

  const handleSave = async () => {
    if (!profile) return;

    try {
      setSaving(true);
      await customerApi.updateCustomer(profile.id, editForm);
      toast.success('Profile updated successfully!');
      setIsEditing(false);
      fetchProfile(); // Refresh profile data
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to update profile';
      toast.error(errorMsg);
      console.error('Error updating profile:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleCreateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      console.log('Creating profile with data:', createForm);
      console.log('Auth token:', localStorage.getItem('authToken'));
      console.log('User ID:', localStorage.getItem('userId'));
      
      const response = await customerApi.createCustomer(createForm);
      console.log('Profile created successfully:', response.data);
      toast.success('Profile created successfully!');
      setShowCreateForm(false);
      fetchProfile(); // Refresh to show the new profile
    } catch (err: any) {
      console.error('Error creating profile:', err);
      console.error('Error response:', err.response?.data);
      const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Failed to create profile';
      toast.error(errorMsg);
    } finally {
      setSaving(false);
    }
  };

  const getClassificationBadge = (classification: string) => {
    const colors: Record<string, string> = {
      VIP: 'bg-purple-100 dark:bg-purple-950 text-purple-800 dark:text-purple-200',
      PREMIUM: 'bg-blue-100 dark:bg-blue-950 text-blue-800 dark:text-blue-200',
      SENIOR_CITIZEN: 'bg-orange-100 dark:bg-orange-950 text-orange-800 dark:text-orange-200',
      SUPER_SENIOR: 'bg-red-100 dark:bg-red-950 text-red-800 dark:text-red-200',
      REGULAR: 'bg-muted text-muted-foreground',
    };
    return colors[classification] || colors.REGULAR;
  };

  const getKycBadge = (status: string) => {
    const colors: Record<string, string> = {
      VERIFIED: 'bg-green-100 dark:bg-green-950 text-green-800 dark:text-green-200',
      REJECTED: 'bg-red-100 dark:bg-red-950 text-red-800 dark:text-red-200',
      IN_PROGRESS: 'bg-blue-100 dark:bg-blue-950 text-blue-800 dark:text-blue-200',
      PENDING: 'bg-yellow-100 dark:bg-yellow-950 text-yellow-800 dark:text-yellow-200',
      EXPIRED: 'bg-muted text-muted-foreground',
    };
    return colors[status] || colors.PENDING;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary mx-auto mb-2" />
          <div className="text-lg text-muted-foreground">Loading your profile...</div>
        </div>
      </div>
    );
  }

  // Admin Profile View
  if (isAdmin) {
    const token = localStorage.getItem('authToken');
    const decodedToken = token ? decodeToken(token) : null;
    const username = decodedToken?.sub || 'admin';
    const roles = getUserRoles();
    
    return (
      <div className="p-8 space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground flex items-center gap-2">
            <Shield className="h-8 w-8 text-primary" />
            Admin Profile
          </h1>
          <p className="text-muted-foreground mt-2">System administrator account information</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-6 w-6" />
              Account Information
            </CardTitle>
            <CardDescription>
              Administrator account details and permissions
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">Username</Label>
                <div className="flex items-center gap-2">
                  <Input
                    value={username}
                    readOnly
                    className="bg-muted"
                  />
                  <Badge variant="default" className="whitespace-nowrap">
                    {roles.includes('ROLE_ADMIN') ? 'Admin' : roles.includes('ROLE_MANAGER') ? 'Manager' : 'Staff'}
                  </Badge>
                </div>
              </div>

              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">Account Type</Label>
                <Input
                  value="System Administrator"
                  readOnly
                  className="bg-muted"
                />
              </div>

              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">Roles & Permissions</Label>
                <div className="flex flex-wrap gap-2">
                  {roles.map((role) => (
                    <Badge key={role} variant="secondary">
                      {role.replace('ROLE_', '')}
                    </Badge>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">Access Level</Label>
                <Input
                  value="Full System Access"
                  readOnly
                  className="bg-muted"
                />
              </div>
            </div>

            <Alert>
              <Shield className="h-4 w-4" />
              <AlertDescription>
                <strong>Administrator Account:</strong> This account has full access to all system features including customer management, 
                account creation, transactions, and system configuration. Admin users do not have customer profiles as they are bank staff members.
              </AlertDescription>
            </Alert>

            <div className="space-y-4 pt-4 border-t">
              <h3 className="text-lg font-semibold">System Capabilities</h3>
              <ul className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  Create and manage customer accounts
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  View all FD accounts across customers
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  Process transactions and redemptions
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  Manage products and pricing
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  Access batch management features
                </li>
                <li className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500"></div>
                  Customize interest rates and terms
                </li>
              </ul>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8">
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="p-8 space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-6 w-6" />
              Complete Your Profile
            </CardTitle>
            <CardDescription>
              Welcome! To access all features, please complete your customer profile.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {!showCreateForm ? (
              <>
                <Alert className="mb-4">
                  <AlertDescription>
                    Your login account has been created, but you need to complete your customer profile 
                    to access all banking features.
                  </AlertDescription>
                </Alert>
                <div className="text-sm text-muted-foreground mb-4">
                  <p className="text-foreground"><strong>Current Account Details:</strong></p>
                  <p>Username: {localStorage.getItem('username')}</p>
                  <p>User ID: {localStorage.getItem('userId')}</p>
                </div>
                <Button onClick={() => setShowCreateForm(true)} className="w-full" size="lg">
                  <Plus className="mr-2 h-5 w-5" />
                  Create My Customer Profile
                </Button>
              </>
            ) : (
              <form onSubmit={handleCreateProfile} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="fullName">Full Name *</Label>
                    <Input
                      id="fullName"
                      value={createForm.fullName}
                      onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                      required
                    />
                  </div>
                  <div>
                    <Label htmlFor="mobileNumber">Mobile Number *</Label>
                    <Input
                      id="mobileNumber"
                      value={createForm.mobileNumber}
                      onChange={(e) => setCreateForm({ ...createForm, mobileNumber: e.target.value })}
                      pattern="[0-9]{10}"
                      placeholder="10 digits"
                      required
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="email">Email *</Label>
                  <Input
                    id="email"
                    type="email"
                    value={createForm.email}
                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="dateOfBirth">Date of Birth *</Label>
                    <Input
                      id="dateOfBirth"
                      type="date"
                      value={createForm.dateOfBirth}
                      onChange={(e) => setCreateForm({ ...createForm, dateOfBirth: e.target.value })}
                      required
                    />
                  </div>
                  <div>
                    <Label htmlFor="gender">Gender *</Label>
                    <select
                      id="gender"
                      value={createForm.gender}
                      onChange={(e) => setCreateForm({ ...createForm, gender: e.target.value as any })}
                      className="w-full h-10 px-3 border border-border rounded-md"
                      required
                    >
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="panNumber">PAN Number (Optional)</Label>
                    <Input
                      id="panNumber"
                      value={createForm.panNumber}
                      onChange={(e) => setCreateForm({ ...createForm, panNumber: e.target.value })}
                      pattern="[A-Z]{5}[0-9]{4}[A-Z]"
                      placeholder="ABCDE1234F"
                    />
                  </div>
                  <div>
                    <Label htmlFor="aadharNumber">Aadhar Number (Optional)</Label>
                    <Input
                      id="aadharNumber"
                      value={createForm.aadharNumber}
                      onChange={(e) => setCreateForm({ ...createForm, aadharNumber: e.target.value })}
                      pattern="[0-9]{12}"
                      placeholder="12 digits"
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="addressLine1">Address Line 1 *</Label>
                  <Input
                    id="addressLine1"
                    value={createForm.addressLine1}
                    onChange={(e) => setCreateForm({ ...createForm, addressLine1: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <Label htmlFor="addressLine2">Address Line 2 (Optional)</Label>
                  <Input
                    id="addressLine2"
                    value={createForm.addressLine2}
                    onChange={(e) => setCreateForm({ ...createForm, addressLine2: e.target.value })}
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <Label htmlFor="city">City *</Label>
                    <Input
                      id="city"
                      value={createForm.city}
                      onChange={(e) => setCreateForm({ ...createForm, city: e.target.value })}
                      required
                    />
                  </div>
                  <div>
                    <Label htmlFor="state">State *</Label>
                    <Input
                      id="state"
                      value={createForm.state}
                      onChange={(e) => setCreateForm({ ...createForm, state: e.target.value })}
                      required
                    />
                  </div>
                  <div>
                    <Label htmlFor="pincode">Pincode *</Label>
                    <Input
                      id="pincode"
                      value={createForm.pincode}
                      onChange={(e) => setCreateForm({ ...createForm, pincode: e.target.value })}
                      pattern="[0-9]{6}"
                      placeholder="6 digits"
                      required
                    />
                  </div>
                </div>

                <div className="flex gap-2 pt-4">
                  <Button type="button" variant="outline" onClick={() => setShowCreateForm(false)} className="flex-1">
                    Cancel
                  </Button>
                  <Button type="submit" disabled={saving} className="flex-1">
                    {saving ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Creating...
                      </>
                    ) : (
                      'Create Profile'
                    )}
                  </Button>
                </div>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-foreground flex items-center gap-2">
            <User className="h-8 w-8" />
            My Profile
          </h1>
          <p className="text-muted-foreground mt-2">View and manage your customer information</p>
        </div>
        {!isEditing ? (
          <Button onClick={handleEdit} className="flex items-center gap-2">
            <Edit2 size={16} />
            Edit Profile
          </Button>
        ) : (
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleCancel} disabled={saving}>
              <X size={16} className="mr-2" />
              Cancel
            </Button>
            <Button onClick={handleSave} disabled={saving}>
              {saving ? (
                <>
                  <Loader2 size={16} className="mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                <>
                  <Save size={16} className="mr-2" />
                  Save Changes
                </>
              )}
            </Button>
          </div>
        )}
      </div>

      {/* Profile Completion Card */}
      {!isEditing && <ProfileCompletion profile={profile} />}

      {/* Profile Overview Card */}
      <Card>
        <CardHeader>
          <CardTitle>Profile Information</CardTitle>
          <CardDescription>Your personal and account details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Status Badges */}
          <div className="flex gap-4">
            <div>
              <Label className="text-sm text-muted-foreground">Classification</Label>
              <Badge className={`mt-1 ${getClassificationBadge(profile.classification)}`}>
                {profile.classification.replace('_', ' ')}
              </Badge>
            </div>
            <div>
              <Label className="text-sm text-muted-foreground">KYC Status</Label>
              <Badge className={`mt-1 ${getKycBadge(profile.kycStatus)}`}>
                {profile.kycStatus}
              </Badge>
            </div>
            <div>
              <Label className="text-sm text-muted-foreground">Account Status</Label>
              <Badge className={`mt-1 ${profile.isActive ? 'bg-green-100 dark:bg-green-950 text-green-800 dark:text-green-200' : 'bg-muted text-muted-foreground'}`}>
                {profile.isActive ? 'Active' : 'Inactive'}
              </Badge>
            </div>
          </div>

          {/* Personal Information */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Personal Information</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="fullName">Full Name *</Label>
                <Input
                  id="fullName"
                  value={isEditing ? editForm.fullName : profile.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="dateOfBirth">Date of Birth *</Label>
                <Input
                  id="dateOfBirth"
                  type="date"
                  value={isEditing ? editForm.dateOfBirth : profile.dateOfBirth}
                  onChange={(e) => setEditForm({ ...editForm, dateOfBirth: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="gender">Gender</Label>
                <select
                  id="gender"
                  value={isEditing ? editForm.gender : profile.gender}
                  onChange={(e) => setEditForm({ ...editForm, gender: e.target.value as any })}
                  disabled={!isEditing}
                  className={`w-full h-10 px-3 border border-border rounded-md ${!isEditing ? 'bg-muted' : ''}`}
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div>
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  value={profile.username}
                  disabled
                  className="bg-muted"
                />
              </div>
            </div>
          </div>

          {/* Contact Information */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Contact Information</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="email">Email *</Label>
                <Input
                  id="email"
                  type="email"
                  value={isEditing ? editForm.email : profile.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="mobileNumber">Mobile Number *</Label>
                <Input
                  id="mobileNumber"
                  value={isEditing ? editForm.mobileNumber : profile.mobileNumber}
                  onChange={(e) => setEditForm({ ...editForm, mobileNumber: e.target.value })}
                  disabled={!isEditing}
                  pattern="[0-9]{10}"
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
            </div>
          </div>

          {/* Identity Documents */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Identity Documents</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="panNumber">PAN Number</Label>
                <Input
                  id="panNumber"
                  value={isEditing ? (editForm.panNumber || '') : (profile.panNumber || '-')}
                  onChange={(e) => setEditForm({ ...editForm, panNumber: e.target.value })}
                  disabled={!isEditing}
                  pattern="[A-Z]{5}[0-9]{4}[A-Z]"
                  placeholder="ABCDE1234F"
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="aadharNumber">Aadhar Number</Label>
                <Input
                  id="aadharNumber"
                  value={isEditing ? (editForm.aadharNumber || '') : (profile.aadharNumber || '-')}
                  onChange={(e) => setEditForm({ ...editForm, aadharNumber: e.target.value })}
                  disabled={!isEditing}
                  pattern="[0-9]{12}"
                  placeholder="12 digits"
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
            </div>
          </div>

          {/* Address */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Address</h3>
            <div className="space-y-4">
              <div>
                <Label htmlFor="addressLine1">Address Line 1 *</Label>
                <Input
                  id="addressLine1"
                  value={isEditing ? (editForm.addressLine1 || '') : (profile.addressLine1 || '-')}
                  onChange={(e) => setEditForm({ ...editForm, addressLine1: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="addressLine2">Address Line 2</Label>
                <Input
                  id="addressLine2"
                  value={isEditing ? (editForm.addressLine2 || '') : (profile.addressLine2 || '-')}
                  onChange={(e) => setEditForm({ ...editForm, addressLine2: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label htmlFor="city">City *</Label>
                  <Input
                    id="city"
                    value={isEditing ? (editForm.city || '') : (profile.city || '-')}
                    onChange={(e) => setEditForm({ ...editForm, city: e.target.value })}
                    disabled={!isEditing}
                    className={!isEditing ? 'bg-muted' : ''}
                  />
                </div>
                <div>
                  <Label htmlFor="state">State *</Label>
                  <Input
                    id="state"
                    value={isEditing ? (editForm.state || '') : (profile.state || '-')}
                    onChange={(e) => setEditForm({ ...editForm, state: e.target.value })}
                    disabled={!isEditing}
                    className={!isEditing ? 'bg-muted' : ''}
                  />
                </div>
                <div>
                  <Label htmlFor="pincode">Pincode *</Label>
                  <Input
                    id="pincode"
                    value={isEditing ? (editForm.pincode || '') : (profile.pincode || '-')}
                    onChange={(e) => setEditForm({ ...editForm, pincode: e.target.value })}
                    disabled={!isEditing}
                    pattern="[0-9]{6}"
                    className={!isEditing ? 'bg-muted' : ''}
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="country">Country *</Label>
                <Input
                  id="country"
                  value={isEditing ? (editForm.country || '') : (profile.country || '-')}
                  onChange={(e) => setEditForm({ ...editForm, country: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
            </div>
          </div>

          {/* Banking Details */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Banking Details</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="accountNumber">Account Number</Label>
                <Input
                  id="accountNumber"
                  value={isEditing ? (editForm.accountNumber || '') : (profile.accountNumber || '-')}
                  onChange={(e) => setEditForm({ ...editForm, accountNumber: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
              <div>
                <Label htmlFor="ifscCode">IFSC Code</Label>
                <Input
                  id="ifscCode"
                  value={isEditing ? (editForm.ifscCode || '') : (profile.ifscCode || '-')}
                  onChange={(e) => setEditForm({ ...editForm, ifscCode: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted' : ''}
                />
              </div>
            </div>
          </div>

          {/* Preferences */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Preferences</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="preferredLanguage">Preferred Language</Label>
                <select
                  id="preferredLanguage"
                  value={isEditing ? editForm.preferredLanguage : profile.preferredLanguage}
                  onChange={(e) => setEditForm({ ...editForm, preferredLanguage: e.target.value })}
                  disabled={!isEditing}
                  className={`w-full h-10 px-3 border border-border rounded-md ${!isEditing ? 'bg-muted' : ''}`}
                >
                  <option value="en">English</option>
                  <option value="hi">Hindi</option>
                  <option value="es">Spanish</option>
                </select>
              </div>
              <div>
                <Label htmlFor="preferredCurrency">Preferred Currency</Label>
                <select
                  id="preferredCurrency"
                  value={isEditing ? editForm.preferredCurrency : profile.preferredCurrency}
                  onChange={(e) => setEditForm({ ...editForm, preferredCurrency: e.target.value })}
                  disabled={!isEditing}
                  className={`w-full h-10 px-3 border border-border rounded-md ${!isEditing ? 'bg-muted' : ''}`}
                >
                  <option value="INR">INR (₹)</option>
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                  <option value="GBP">GBP (£)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Notification Preferences */}
          <div>
            <h3 className="text-lg font-semibold mb-4 text-foreground">Notification Preferences</h3>
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="emailNotifications"
                  checked={isEditing ? editForm.emailNotifications : profile.emailNotifications}
                  onChange={(e) => setEditForm({ ...editForm, emailNotifications: e.target.checked })}
                  disabled={!isEditing}
                  className="w-4 h-4"
                />
                <Label htmlFor="emailNotifications" className="cursor-pointer">Email Notifications</Label>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="smsNotifications"
                  checked={isEditing ? editForm.smsNotifications : profile.smsNotifications}
                  onChange={(e) => setEditForm({ ...editForm, smsNotifications: e.target.checked })}
                  disabled={!isEditing}
                  className="w-4 h-4"
                />
                <Label htmlFor="smsNotifications" className="cursor-pointer">SMS Notifications</Label>
              </div>
            </div>
          </div>

          {/* Metadata */}
          <div className="pt-4 border-t text-sm text-gray-600">
            <div className="grid grid-cols-2 gap-2">
              <div>Customer ID: <span className="font-semibold">{profile.id}</span></div>
              <div>User ID: <span className="font-semibold">{profile.userId}</span></div>
              <div>Created: <span className="font-semibold">{new Date(profile.createdAt).toLocaleDateString()}</span></div>
              <div>Last Updated: <span className="font-semibold">{new Date(profile.updatedAt).toLocaleDateString()}</span></div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}



