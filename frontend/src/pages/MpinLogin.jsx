import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PinPad from '../components/common/PinPad';
import { useAuth } from '../context/AuthContext';

export default function MpinLogin() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [attempts, setAttempts] = useState(0);

  const handleVerify = async (pin) => {
    setLoading(true);
    setError('');
    try {
      await new Promise(resolve => setTimeout(resolve, 800));
      // In production, verify via API
      navigate('/home', { replace: true });
    } catch (err) {
      setAttempts(prev => prev + 1);
      if (attempts >= 2) {
        setError('Too many attempts. Please try again later.');
        setTimeout(() => logout(), 2000);
      } else {
        setError(`Incorrect MPIN. ${3 - attempts - 1} attempts remaining.`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* Header */}
      <div className="gradient-primary px-6 pb-10 pt-16 rounded-b-[2rem] text-center">
        <div className="w-20 h-20 bg-white/20 rounded-2xl mx-auto flex items-center justify-center mb-4">
          <span className="text-2xl font-extrabold text-white">NUPI</span>
        </div>
        <h2 className="text-white text-xl font-bold">Welcome Back</h2>
        <p className="text-white/60 text-sm mt-1">Enter your MPIN to continue</p>
      </div>

      <div className="flex-1 flex items-center justify-center px-6 py-8">
        <PinPad
          title="Enter MPIN"
          onComplete={handleVerify}
          onCancel={() => {}}
          loading={loading}
          error={error}
        />
      </div>

      <div className="text-center pb-8 px-6 space-y-3">
        <button
          onClick={() => navigate('/login', { replace: true })}
          className="text-sm text-nupi-primary font-medium"
        >
          Forgot MPIN?
        </button>
        <p className="text-xs text-nupi-text-muted">
          <button onClick={logout} className="text-nupi-accent font-medium">
            Use a different number
          </button>
        </p>
      </div>
    </div>
  );
}
