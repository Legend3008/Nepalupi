import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function OtpVerify() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const phone = location.state?.phone || '';
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(30);
  const inputs = useRef([]);

  useEffect(() => {
    if (!phone) navigate('/login', { replace: true });
    inputs.current[0]?.focus();
  }, [phone, navigate]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(c => c - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);
    setError('');

    if (value && index < 5) {
      inputs.current[index + 1]?.focus();
    }

    // Auto-submit when complete
    if (newOtp.every(d => d !== '') && newOtp.join('').length === 6) {
      handleVerify(newOtp.join(''));
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      setOtp(pasted.split(''));
      handleVerify(pasted);
    }
  };

  const handleVerify = async (otpStr) => {
    setLoading(true);
    setError('');
    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      // Simulate successful verification
      const userData = {
        id: 'usr_' + Date.now(),
        mobileNumber: phone,
        fullName: '',
        kycLevel: 'BASIC',
        createdAt: new Date().toISOString(),
      };
      const token = 'jwt_' + btoa(JSON.stringify({ sub: userData.id, phone }));
      login(userData, token);
      navigate('/mpin-setup', { replace: true });
    } catch (err) {
      setError(err.message || 'Invalid OTP. Please try again.');
      setOtp(['', '', '', '', '', '']);
      inputs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  };

  const handleResend = () => {
    setCountdown(30);
    setOtp(['', '', '', '', '', '']);
    setError('');
    // Simulate resend
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <div className="gradient-primary px-6 pb-10 pt-16 rounded-b-[2rem]">
        <button onClick={() => navigate(-1)} className="text-white/70 mb-4 text-sm">← Back</button>
        <h2 className="text-white text-2xl font-bold">Verify OTP</h2>
        <p className="text-white/70 text-sm mt-2">
          Enter the 6-digit code sent to <span className="text-white font-medium">+977 {phone}</span>
        </p>
      </div>

      <div className="flex-1 px-6 pt-10 flex flex-col items-center">
        {/* OTP Input */}
        <div className="flex gap-3 mb-4" onPaste={handlePaste}>
          {otp.map((digit, i) => (
            <input
              key={i}
              ref={el => inputs.current[i] = el}
              type="tel"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={e => handleChange(i, e.target.value)}
              onKeyDown={e => handleKeyDown(i, e)}
              className={`w-12 h-14 text-center text-2xl font-bold rounded-xl border-2 transition-all
                ${digit ? 'border-nupi-primary bg-nupi-primary/5' : 'border-nupi-border'}
                ${error ? 'border-nupi-danger' : ''}
                focus:border-nupi-primary focus:ring-2 focus:ring-nupi-primary/20 outline-none`}
            />
          ))}
        </div>

        {error && <p className="text-sm text-nupi-danger font-medium animate-fade-in">{error}</p>}

        {loading && (
          <div className="flex items-center gap-2 mt-4">
            <div className="w-5 h-5 border-2 border-nupi-primary border-t-transparent rounded-full animate-spin" />
            <span className="text-sm text-nupi-text-secondary">Verifying...</span>
          </div>
        )}

        {/* Resend */}
        <div className="mt-8 text-center">
          {countdown > 0 ? (
            <p className="text-sm text-nupi-text-secondary">
              Resend OTP in <span className="font-semibold text-nupi-primary">{countdown}s</span>
            </p>
          ) : (
            <button onClick={handleResend} className="text-sm font-semibold text-nupi-primary">
              Resend OTP
            </button>
          )}
        </div>

        <div className="flex-1" />

        <div className="text-center pb-8">
          <p className="text-xs text-nupi-text-muted">
            Didn't receive the code? Check your SMS inbox or try resending.
          </p>
        </div>
      </div>
    </div>
  );
}
