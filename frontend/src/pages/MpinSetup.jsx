import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PinPad from '../components/common/PinPad';
import { validateMpin } from '../utils/validators';

export default function MpinSetup() {
  const navigate = useNavigate();
  const { setMpinComplete } = useAuth();
  const [step, setStep] = useState('create'); // create | confirm
  const [firstPin, setFirstPin] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreate = (pin) => {
    const err = validateMpin(pin);
    if (err) { setError(err); return; }
    setFirstPin(pin);
    setError('');
    setStep('confirm');
  };

  const handleConfirm = async (pin) => {
    if (pin !== firstPin) {
      setError('MPIN does not match. Try again.');
      return;
    }
    setLoading(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      setMpinComplete();
      navigate('/home', { replace: true });
    } catch (err) {
      setError(err.message || 'Failed to set MPIN');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <div className="gradient-primary px-6 pb-8 pt-16 rounded-b-[2rem]">
        <h2 className="text-white text-2xl font-bold">
          {step === 'create' ? 'Set Your MPIN' : 'Confirm MPIN'}
        </h2>
        <p className="text-white/70 text-sm mt-2">
          {step === 'create'
            ? 'Create a 6-digit MPIN to secure your transactions'
            : 'Re-enter your MPIN to confirm'}
        </p>
        {/* Progress */}
        <div className="flex gap-2 mt-4">
          <div className={`flex-1 h-1 rounded-full ${step === 'create' ? 'bg-white' : 'bg-white/30'}`} />
          <div className={`flex-1 h-1 rounded-full ${step === 'confirm' ? 'bg-white' : 'bg-white/30'}`} />
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center px-6 py-8">
        {step === 'create' ? (
          <PinPad
            title="Create MPIN"
            subtitle="Choose a 6-digit PIN you'll remember"
            onComplete={handleCreate}
            onCancel={() => navigate(-1)}
            error={error}
          />
        ) : (
          <PinPad
            title="Confirm MPIN"
            subtitle="Enter the same PIN again"
            onComplete={handleConfirm}
            onCancel={() => { setStep('create'); setFirstPin(''); setError(''); }}
            loading={loading}
            error={error}
          />
        )}
      </div>

      <div className="text-center pb-8 px-6">
        <p className="text-xs text-nupi-text-muted">
          Your MPIN is encrypted and stored securely. Never share it with anyone.
        </p>
      </div>
    </div>
  );
}
