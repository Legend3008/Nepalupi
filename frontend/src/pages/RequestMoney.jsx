import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import { validateAmount, validateUpiId } from '../utils/validators';
import { IoSearch, IoPersonCircle } from 'react-icons/io5';

export default function RequestMoney() {
  const navigate = useNavigate();
  const [upiId, setUpiId] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState('form'); // form | success

  const handleRequest = async () => {
    const newErrors = {};
    const upiErr = validateUpiId(upiId);
    if (upiErr) newErrors.upi = upiErr;
    const amtErr = validateAmount(amount);
    if (amtErr) newErrors.amount = amtErr;
    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }

    setLoading(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      setStep('success');
    } catch (err) {
      setErrors({ general: err.message });
    } finally {
      setLoading(false);
    }
  };

  if (step === 'success') {
    return (
      <div className="page-container flex flex-col items-center justify-center px-6">
        <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mb-4 animate-bounce-in">
          <svg className="w-10 h-10 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-nupi-text">Request Sent!</h2>
        <p className="text-sm text-nupi-text-secondary mt-2 text-center">
          Your payment request of रू {parseFloat(amount).toLocaleString()} has been sent to {upiId}
        </p>
        <button onClick={() => navigate('/home')} className="btn-primary mt-8 px-12">
          Done
        </button>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Request Money" showBack showHelp />

      <div className="px-4 mt-4 space-y-6">
        {/* UPI ID Input */}
        <div>
          <label className="text-sm font-medium text-nupi-text-secondary mb-2 block">Request from</label>
          <div className="flex items-center gap-3 input-field">
            <IoPersonCircle className="w-5 h-5 text-nupi-text-muted" />
            <input
              type="text"
              value={upiId}
              onChange={e => { setUpiId(e.target.value); setErrors({}); }}
              placeholder="Enter UPI ID (e.g., name@bank)"
              className="flex-1 bg-transparent outline-none"
            />
          </div>
          {errors.upi && <p className="text-sm text-nupi-danger mt-1">{errors.upi}</p>}
        </div>

        {/* Amount */}
        <AmountInput value={amount} onChange={v => { setAmount(v); setErrors({}); }} error={errors.amount} />

        {/* Note */}
        <div>
          <label className="text-sm font-medium text-nupi-text-secondary mb-2 block">Note</label>
          <input
            type="text"
            value={note}
            onChange={e => setNote(e.target.value)}
            placeholder="What's this for?"
            className="input-field"
            maxLength={50}
          />
        </div>

        {errors.general && <p className="text-sm text-nupi-danger">{errors.general}</p>}

        <button
          onClick={handleRequest}
          disabled={loading || !upiId || !amount}
          className="btn-primary w-full flex items-center justify-center gap-2"
        >
          {loading ? (
            <><div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />Sending...</>
          ) : 'Send Request'}
        </button>

        <p className="text-xs text-nupi-text-muted text-center">
          The recipient will receive a notification to approve or decline your request.
        </p>
      </div>
    </div>
  );
}
