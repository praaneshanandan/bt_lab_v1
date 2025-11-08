import React, { useState } from 'react';
import { productApi } from '../../services/api';
import { Calculator } from 'lucide-react';

interface InterestRateCalculatorProps {
  productId: number;
}

interface RateResult {
  applicableRate: number;
  effectiveRate: number;
  monthlyRate: number;
  annualRate: number;
  compoundingFrequency?: string;
  specialRateApplied?: boolean;
  seniorCitizenBonus?: number;
}

const InterestRateCalculator: React.FC<InterestRateCalculatorProps> = ({ productId }) => {
  const [formData, setFormData] = useState({
    amount: '',
    termInMonths: '',
    classification: 'GENERAL',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<RateResult | null>(null);

  const handleCalculate = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const amount = parseFloat(formData.amount);
    const termInMonths = parseInt(formData.termInMonths);

    if (amount <= 0 || termInMonths <= 0) {
      setError('Please enter valid amount and term');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Fetch both applicable rate and effective rate
      const [applicableResponse, effectiveResponse] = await Promise.all([
        productApi.getApplicableInterestRate(productId, amount, termInMonths, formData.classification),
        productApi.calculateEffectiveRate(productId, amount, termInMonths, formData.classification),
      ]);

      const applicableData = applicableResponse.data?.data;
      const effectiveData = effectiveResponse.data?.data;

      setResult({
        applicableRate: applicableData?.interestRate || 0,
        effectiveRate: effectiveData?.effectiveRate || 0,
        monthlyRate: (applicableData?.interestRate || 0) / 12,
        annualRate: applicableData?.interestRate || 0,
        compoundingFrequency: applicableData?.compoundingFrequency,
        specialRateApplied: applicableData?.specialRateApplied,
        seniorCitizenBonus: applicableData?.seniorCitizenBonus,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to calculate interest rate');
      setResult(null);
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

  const calculateMaturityAmount = () => {
    if (!result || !formData.amount || !formData.termInMonths) return 0;
    
    const principal = parseFloat(formData.amount);
    const rate = result.applicableRate / 100;
    const time = parseInt(formData.termInMonths) / 12;
    
    // Simple interest calculation (can be enhanced for compound interest)
    const interest = principal * rate * time;
    return principal + interest;
  };

  return (
    <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-6 shadow-md">
      <div className="flex items-center gap-2 mb-4">
        <Calculator className="text-blue-600" size={24} />
        <h3 className="text-xl font-semibold text-gray-800">Interest Rate Calculator</h3>
      </div>

      <form onSubmit={handleCalculate} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Investment Amount (₹)
            </label>
            <input
              type="number"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="e.g., 100000"
              min="1000"
              step="1000"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Term (Months)
            </label>
            <input
              type="number"
              value={formData.termInMonths}
              onChange={(e) => setFormData({ ...formData, termInMonths: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="e.g., 12"
              min="1"
              max="120"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Customer Classification
            </label>
            <select
              value={formData.classification}
              onChange={(e) => setFormData({ ...formData, classification: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            >
              <option value="GENERAL">General</option>
              <option value="SENIOR_CITIZEN">Senior Citizen</option>
              <option value="SUPER_SENIOR_CITIZEN">Super Senior Citizen</option>
              <option value="STAFF">Staff</option>
              <option value="VIP">VIP</option>
            </select>
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full md:w-auto px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed font-medium"
        >
          {loading ? 'Calculating...' : 'Calculate Interest Rate'}
        </button>
      </form>

      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {result && (
        <div className="mt-6 bg-white rounded-lg p-6 shadow-sm border border-gray-200">
          <h4 className="text-lg font-semibold text-gray-800 mb-4">Calculation Results</h4>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground">Applicable Interest Rate</p>
                <p className="text-3xl font-bold text-blue-600">{result.applicableRate.toFixed(2)}%</p>
                <p className="text-xs text-muted-foreground mt-1">Per annum</p>
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Effective Interest Rate</p>
                <p className="text-2xl font-semibold text-green-600">{result.effectiveRate.toFixed(2)}%</p>
                <p className="text-xs text-muted-foreground mt-1">After compounding</p>
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Monthly Rate</p>
                <p className="text-xl font-medium text-foreground">{result.monthlyRate.toFixed(3)}%</p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="bg-gradient-to-r from-green-50 to-emerald-50 p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Estimated Maturity Amount</p>
                <p className="text-3xl font-bold text-green-700">
                  {formatCurrency(calculateMaturityAmount())}
                </p>
                <div className="mt-3 pt-3 border-t border-green-200">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Principal:</span>
                    <span className="font-medium">{formatCurrency(parseFloat(formData.amount))}</span>
                  </div>
                  <div className="flex justify-between text-sm mt-1">
                    <span className="text-muted-foreground">Interest:</span>
                    <span className="font-medium text-green-600">
                      {formatCurrency(calculateMaturityAmount() - parseFloat(formData.amount))}
                    </span>
                  </div>
                </div>
              </div>

              {result.compoundingFrequency && (
                <div>
                  <p className="text-sm text-muted-foreground">Compounding Frequency</p>
                  <p className="text-lg font-medium text-foreground">{result.compoundingFrequency}</p>
                </div>
              )}

              {result.specialRateApplied && (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                  <p className="text-sm text-yellow-800 font-medium">
                    ✨ Special rate applied
                    {result.seniorCitizenBonus && ` (+${result.seniorCitizenBonus}% bonus)`}
                  </p>
                </div>
              )}
            </div>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-200">
            <p className="text-xs text-muted-foreground">
              <strong>Note:</strong> This is an estimate. Actual returns may vary based on compounding frequency, 
              premature withdrawal penalties, and other terms and conditions. Please refer to the product documentation 
              for complete details.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default InterestRateCalculator;

