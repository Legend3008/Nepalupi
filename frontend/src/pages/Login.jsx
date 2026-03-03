import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { validatePhone } from '../utils/validators';
import { IoPhonePortraitOutline, IoShieldCheckmarkOutline, IoLockClosedOutline } from 'react-icons/io5';

export default function Login() {
  const navigate = useNavigate();
  const { savePhone } = useAuth();
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [agreed, setAgreed] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const err = validatePhone(phone);
    if (err) { setError(err); return; }
    if (!agreed) { setError('Please agree to the terms & conditions'); return; }

    setLoading(true);
    setError('');
    try {
      // Simulate OTP sending
      await new Promise(resolve => setTimeout(resolve, 1000));
      savePhone(phone);
      navigate('/otp-verify', { state: { phone } });
    } catch (err) {
      setError(err.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* Header */}
      <div className="gradient-primary px-6 pb-12 pt-16 rounded-b-[2rem]">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
            <span className="text-xl font-extrabold text-white">N</span>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">NUPI</h1>
            <p className="text-white/60 text-xs">Nepal Unified Payment Interface</p>
          </div>
        </div>
        <h2 className="text-white text-xl font-semibold">Welcome!</h2>
        <p className="text-white/70 text-sm mt-1">Login with your mobile number to get started</p>
      </div>

      {/* Features */}
      <div className="flex justify-around px-6 -mt-6">
        {[
          { icon: IoPhonePortraitOutline, label: 'Easy Setup' },
          { icon: IoShieldCheckmarkOutline, label: 'Secure' },
          { icon: IoLockClosedOutline, label: 'Encrypted' },
        ].map((f, i) => (
          <div key={i} className="flex flex-col items-center bg-white rounded-2xl shadow-card px-4 py-3">
            <f.icon className="w-6 h-6 text-nupi-primary mb-1" />
            <span className="text-xs font-medium text-nupi-text-secondary">{f.label}</span>
          </div>
        ))}
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="flex-1 px-6 pt-8 flex flex-col">
        <label className="text-sm font-medium text-nupi-text mb-2">Mobile Number</label>
        <div className={`flex items-center border-2 rounded-xl transition-colors ${error ? 'border-nupi-danger' : 'border-nupi-border focus-within:border-nupi-primary'}`}>
          <span className="pl-4 pr-2 text-nupi-text-secondary font-medium border-r border-nupi-border mr-2 py-3">+977</span>
          <input
            type="tel"
            inputMode="numeric"
            maxLength={10}
            value={phone}
            onChange={e => { setPhone(e.target.value.replace(/\D/g, '')); setError(''); }}
            placeholder="98XXXXXXXX"
            className="flex-1 py-3 pr-4 text-lg font-medium bg-transparent outline-none placeholder:text-nupi-text-muted"
            autoFocus
          />
        </div>
        {error && <p className="text-sm text-nupi-danger mt-2">{error}</p>}

        {/* Terms */}
        <label className="flex items-start gap-3 mt-6 cursor-pointer">
          <input
            type="checkbox"
            checked={agreed}
            onChange={e => { setAgreed(e.target.checked); setError(''); }}
            className="mt-0.5 w-5 h-5 rounded border-nupi-border text-nupi-primary focus:ring-nupi-primary"
          />
          <span className="text-xs text-nupi-text-secondary leading-relaxed">
            I agree to the <a className="text-nupi-primary font-medium">Terms of Service</a> and{' '}
            <a className="text-nupi-primary font-medium">Privacy Policy</a> of NUPI. By proceeding, I authorize NUPI to verify my mobile number.
          </span>
        </label>

        <div className="flex-1" />

        <button
          type="submit"
          disabled={loading || phone.length < 10}
          className="btn-primary w-full mb-8 flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              Sending OTP...
            </>
          ) : (
            'Get OTP'
          )}
        </button>
      </form>

      {/* Footer */}
      <div className="text-center pb-6 px-6">
        <p className="text-xs text-nupi-text-muted">
          By continuing you agree to receive SMS. Standard rates may apply.
        </p>
      </div>
    </div>
  );
}
