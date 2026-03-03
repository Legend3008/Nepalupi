import { useState, useCallback } from 'react';
import { IoBackspace } from 'react-icons/io5';

export default function PinPad({ length = 6, title = 'Enter MPIN', subtitle, onComplete, onCancel, loading = false, error }) {
  const [pin, setPin] = useState('');

  const handlePress = useCallback((digit) => {
    if (pin.length >= length) return;
    const newPin = pin + digit;
    setPin(newPin);
    if (newPin.length === length) {
      setTimeout(() => onComplete(newPin), 150);
    }
  }, [pin, length, onComplete]);

  const handleDelete = useCallback(() => {
    setPin(prev => prev.slice(0, -1));
  }, []);

  const handleClear = useCallback(() => {
    setPin('');
  }, []);

  const digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del'];

  return (
    <div className="flex flex-col items-center w-full">
      {/* Title */}
      <div className="text-center mb-6">
        <h2 className="text-xl font-semibold text-nupi-text">{title}</h2>
        {subtitle && <p className="text-sm text-nupi-text-secondary mt-1">{subtitle}</p>}
      </div>

      {/* PIN Dots */}
      <div className="flex gap-3 mb-3">
        {Array.from({ length }, (_, i) => (
          <div key={i} className={i < pin.length ? 'pin-dot-filled' : 'pin-dot'} />
        ))}
      </div>

      {/* Error */}
      {error && (
        <p className="text-sm text-nupi-danger font-medium mb-3 animate-fade-in">{error}</p>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex items-center gap-2 mb-3">
          <div className="w-4 h-4 border-2 border-nupi-primary border-t-transparent rounded-full animate-spin" />
          <span className="text-sm text-nupi-text-secondary">Verifying...</span>
        </div>
      )}

      {/* Keypad */}
      <div className="grid grid-cols-3 gap-3 w-full max-w-[280px]">
        {digits.map((digit, idx) => {
          if (digit === '') {
            return (
              <button key={idx} onClick={onCancel} className="h-16 flex items-center justify-center text-sm text-nupi-text-secondary font-medium">
                Cancel
              </button>
            );
          }
          if (digit === 'del') {
            return (
              <button
                key={idx}
                onClick={handleDelete}
                onLongPress={handleClear}
                className="h-16 flex items-center justify-center rounded-2xl active:bg-gray-100 transition-colors"
              >
                <IoBackspace className="w-7 h-7 text-nupi-text-secondary" />
              </button>
            );
          }
          return (
            <button
              key={idx}
              onClick={() => handlePress(digit)}
              disabled={loading}
              className="h-16 flex items-center justify-center rounded-2xl text-2xl font-semibold text-nupi-text
                         active:bg-nupi-primary active:text-white transition-all duration-100 hover:bg-gray-50"
            >
              {digit}
            </button>
          );
        })}
      </div>
    </div>
  );
}
