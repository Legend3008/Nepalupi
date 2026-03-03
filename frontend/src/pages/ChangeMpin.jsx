import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import PinPad from '../components/common/PinPad';
import { IoCheckmarkCircle, IoLockClosed } from 'react-icons/io5';

export default function ChangeMpin() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1: old, 2: new, 3: confirm
  const [oldPin, setOldPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleOldPin = async (pin) => {
    // Verify old PIN
    await new Promise(r => setTimeout(r, 800));
    setOldPin(pin);
    setStep(2);
  };

  const handleNewPin = (pin) => {
    if (pin === oldPin) {
      setError('New MPIN must be different from old MPIN');
      return;
    }
    setNewPin(pin);
    setError('');
    setStep(3);
  };

  const handleConfirmPin = async (pin) => {
    if (pin !== newPin) {
      setError('MPINs do not match. Try again.');
      return;
    }
    // API call to change MPIN
    await new Promise(r => setTimeout(r, 1000));
    setSuccess(true);
  };

  if (success) {
    return (
      <div className="page-container flex items-center justify-center min-h-screen">
        <div className="text-center px-6">
          <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4 animate-bounce-in">
            <IoCheckmarkCircle className="w-12 h-12 text-green-500" />
          </div>
          <h2 className="text-xl font-bold text-green-700">MPIN Changed!</h2>
          <p className="text-sm text-nupi-text-secondary mt-2">
            Your UPI MPIN has been changed successfully. Use your new MPIN for future transactions.
          </p>
          <button onClick={() => navigate('/profile')} className="btn-primary mt-6 px-12">Done</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Change MPIN" showBack />

      {/* Progress */}
      <div className="px-4 mt-4">
        <div className="flex gap-2">
          {[1, 2, 3].map(s => (
            <div key={s} className={`flex-1 h-1 rounded-full ${step >= s ? 'bg-nupi-primary' : 'bg-gray-200'}`} />
          ))}
        </div>
        <p className="text-xs text-nupi-text-muted mt-1">
          Step {step}: {step === 1 ? 'Current MPIN' : step === 2 ? 'New MPIN' : 'Confirm New MPIN'}
        </p>
      </div>

      <div className="flex items-center justify-center mt-8 px-6">
        <div className="w-full max-w-sm">
          {/* Icon */}
          <div className="text-center mb-6">
            <div className="w-16 h-16 rounded-full bg-nupi-primary/10 flex items-center justify-center mx-auto mb-3">
              <IoLockClosed className="w-8 h-8 text-nupi-primary" />
            </div>
          </div>

          {step === 1 && (
            <PinPad
              title="Enter Current MPIN"
              subtitle="Verify your identity"
              onComplete={handleOldPin}
              onCancel={() => navigate(-1)}
              error={error}
            />
          )}

          {step === 2 && (
            <PinPad
              title="Enter New MPIN"
              subtitle="Choose a 6-digit MPIN"
              onComplete={handleNewPin}
              onCancel={() => { setStep(1); setError(''); }}
              error={error}
            />
          )}

          {step === 3 && (
            <PinPad
              title="Confirm New MPIN"
              subtitle="Re-enter to confirm"
              onComplete={handleConfirmPin}
              onCancel={() => { setStep(2); setError(''); }}
              error={error}
            />
          )}
        </div>
      </div>
    </div>
  );
}
