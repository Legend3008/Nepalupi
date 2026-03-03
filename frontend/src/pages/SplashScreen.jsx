import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function SplashScreen() {
  const navigate = useNavigate();
  const { isAuthenticated, mpinSet } = useAuth();
  const [show, setShow] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setShow(false);
      if (isAuthenticated && mpinSet) {
        navigate('/mpin-login', { replace: true });
      } else if (isAuthenticated) {
        navigate('/home', { replace: true });
      } else {
        navigate('/login', { replace: true });
      }
    }, 2500);
    return () => clearTimeout(timer);
  }, [isAuthenticated, mpinSet, navigate]);

  return (
    <div className="fixed inset-0 gradient-primary flex flex-col items-center justify-center">
      {/* Logo */}
      <div className="animate-bounce-in flex flex-col items-center">
        <div className="w-28 h-28 bg-white rounded-3xl shadow-2xl flex items-center justify-center mb-6">
          <div className="text-center">
            <span className="text-3xl font-extrabold text-nupi-primary tracking-tight">NUPI</span>
            <div className="w-10 h-0.5 bg-nupi-accent mx-auto mt-1" />
          </div>
        </div>
        <h1 className="text-white text-3xl font-bold tracking-wide">NUPI</h1>
        <p className="text-white/70 text-sm mt-2 font-medium tracking-widest uppercase">
          Nepal Unified Payment Interface
        </p>
      </div>

      {/* Loading indicator */}
      <div className="absolute bottom-24 flex flex-col items-center gap-3">
        <div className="flex gap-1.5">
          {[0, 1, 2].map(i => (
            <div
              key={i}
              className="w-2 h-2 rounded-full bg-white/60 animate-pulse"
              style={{ animationDelay: `${i * 200}ms` }}
            />
          ))}
        </div>
      </div>

      {/* Footer */}
      <div className="absolute bottom-8 text-center">
        <p className="text-white/50 text-xs">Powered by</p>
        <p className="text-white/70 text-sm font-semibold">Nepal Rastra Bank</p>
        <p className="text-white/40 text-[10px] mt-1">Secure • Fast • Reliable</p>
      </div>
    </div>
  );
}
