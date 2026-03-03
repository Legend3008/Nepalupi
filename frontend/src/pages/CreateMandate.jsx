import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import PinPad from '../components/common/PinPad';
import { CURRENCY } from '../utils/constants';
import { validateUpiId, validateAmount } from '../utils/validators';
import { IoCalendar, IoCheckmarkCircle } from 'react-icons/io5';

const frequencies = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'YEARLY', label: 'Yearly' },
  { value: 'ONE_TIME', label: 'One-time' },
];

export default function CreateMandate() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({ merchantVpa: '', purpose: '', amount: '', frequency: 'MONTHLY', startDate: '', endDate: '' });
  const [errors, setErrors] = useState({});
  const [showPin, setShowPin] = useState(false);
  const [success, setSuccess] = useState(false);

  const updateField = (field, value) => {
    setForm(p => ({ ...p, [field]: value }));
    setErrors(p => ({ ...p, [field]: null }));
  };

  const validateStep1 = () => {
    const e = {};
    const vpaErr = validateUpiId(form.merchantVpa);
    if (vpaErr) e.merchantVpa = vpaErr;
    if (!form.purpose.trim()) e.purpose = 'Purpose is required';
    if (Object.keys(e).length) { setErrors(e); return; }
    setStep(2);
  };

  const validateStep2 = () => {
    const e = {};
    const amtErr = validateAmount(form.amount);
    if (amtErr) e.amount = amtErr;
    if (!form.startDate) e.startDate = 'Start date is required';
    if (!form.endDate) e.endDate = 'End date is required';
    if (form.startDate && form.endDate && new Date(form.endDate) <= new Date(form.startDate)) {
      e.endDate = 'End date must be after start date';
    }
    if (Object.keys(e).length) { setErrors(e); return; }
    setShowPin(true);
  };

  const handlePinComplete = async (pin) => {
    await new Promise(r => setTimeout(r, 1500));
    setShowPin(false);
    setSuccess(true);
  };

  if (success) {
    return (
      <div className="page-container flex items-center justify-center min-h-screen">
        <div className="text-center px-6">
          <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4 animate-bounce-in">
            <IoCheckmarkCircle className="w-12 h-12 text-green-500" />
          </div>
          <h2 className="text-xl font-bold text-green-700">Mandate Created!</h2>
          <p className="text-sm text-nupi-text-secondary mt-2">
            Auto-pay of {CURRENCY.symbol} {Number(form.amount).toLocaleString()} / {form.frequency.toLowerCase()} to {form.merchantVpa} has been set up.
          </p>
          <button onClick={() => navigate('/mandates')} className="btn-primary mt-6 px-12">View Mandates</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Create Mandate" showBack />

      {/* Progress */}
      <div className="px-4 mt-4">
        <div className="flex gap-2">
          {[1, 2].map(s => (
            <div key={s} className={`flex-1 h-1 rounded-full ${step >= s ? 'bg-nupi-primary' : 'bg-gray-200'}`} />
          ))}
        </div>
        <p className="text-xs text-nupi-text-muted mt-1">Step {step} of 2</p>
      </div>

      <div className="px-4 mt-6 space-y-5">
        {step === 1 && (
          <>
            <h3 className="text-lg font-semibold">Merchant Details</h3>
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Merchant UPI ID</label>
              <input type="text" value={form.merchantVpa} onChange={e => updateField('merchantVpa', e.target.value)}
                placeholder="merchant@nchl" className={`input-field ${errors.merchantVpa ? 'border-red-400' : ''}`} />
              {errors.merchantVpa && <p className="text-xs text-red-500 mt-1">{errors.merchantVpa}</p>}
            </div>
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Purpose</label>
              <input type="text" value={form.purpose} onChange={e => updateField('purpose', e.target.value)}
                placeholder="e.g., Internet Bill, Subscription" className={`input-field ${errors.purpose ? 'border-red-400' : ''}`} />
              {errors.purpose && <p className="text-xs text-red-500 mt-1">{errors.purpose}</p>}
            </div>
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Frequency</label>
              <div className="grid grid-cols-3 gap-2">
                {frequencies.map(f => (
                  <button key={f.value} onClick={() => updateField('frequency', f.value)}
                    className={`py-2 rounded-xl text-sm font-medium border transition-all ${form.frequency === f.value ? 'border-nupi-primary bg-nupi-primary/10 text-nupi-primary' : 'border-gray-200 text-nupi-text-secondary'}`}>
                    {f.label}
                  </button>
                ))}
              </div>
            </div>
            <button onClick={validateStep1} className="btn-primary w-full mt-4">Continue</button>
          </>
        )}

        {step === 2 && (
          <>
            <h3 className="text-lg font-semibold">Payment Details</h3>
            <AmountInput value={form.amount} onChange={v => updateField('amount', v)}
              label="Debit Amount" error={errors.amount} />
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Start Date</label>
                <div className="relative">
                  <input type="date" value={form.startDate} onChange={e => updateField('startDate', e.target.value)}
                    className={`input-field pr-10 ${errors.startDate ? 'border-red-400' : ''}`} />
                  <IoCalendar className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                </div>
                {errors.startDate && <p className="text-xs text-red-500 mt-1">{errors.startDate}</p>}
              </div>
              <div>
                <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">End Date</label>
                <div className="relative">
                  <input type="date" value={form.endDate} onChange={e => updateField('endDate', e.target.value)}
                    className={`input-field pr-10 ${errors.endDate ? 'border-red-400' : ''}`} />
                  <IoCalendar className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                </div>
                {errors.endDate && <p className="text-xs text-red-500 mt-1">{errors.endDate}</p>}
              </div>
            </div>

            {/* Summary */}
            <div className="card bg-gray-50 space-y-2">
              <p className="text-sm font-semibold">Summary</p>
              {[['Merchant', form.merchantVpa], ['Purpose', form.purpose], ['Frequency', form.frequency]].map(([l, v]) => (
                <div key={l} className="flex justify-between text-sm">
                  <span className="text-nupi-text-muted">{l}</span>
                  <span className="font-medium">{v}</span>
                </div>
              ))}
            </div>

            <div className="flex gap-3">
              <button onClick={() => setStep(1)} className="flex-1 btn-secondary">Back</button>
              <button onClick={validateStep2} className="flex-1 btn-primary">Create Mandate</button>
            </div>
          </>
        )}
      </div>

      {showPin && (
        <div className="fixed inset-0 z-50 bg-white flex items-center justify-center">
          <div className="w-full max-w-sm px-6">
            <PinPad title="Enter MPIN" subtitle="To authorize mandate creation"
              onComplete={handlePinComplete} onCancel={() => setShowPin(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
