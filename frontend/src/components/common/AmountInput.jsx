import { useState } from 'react';
import { CURRENCY, QUICK_AMOUNTS } from '../../utils/constants';

export default function AmountInput({ value, onChange, error, label = 'Enter Amount', max = 100000, showQuick = true }) {
  const [focused, setFocused] = useState(false);

  return (
    <div className="w-full">
      <label className="text-sm font-medium text-nupi-text-secondary mb-2 block">{label}</label>

      <div className={`flex items-center gap-2 px-4 py-3 rounded-xl border-2 transition-all duration-200
        ${focused ? 'border-nupi-primary bg-nupi-primary/5' : 'border-nupi-border bg-white'}
        ${error ? 'border-nupi-danger' : ''}`}>
        <span className="text-xl font-semibold text-nupi-text-secondary">{CURRENCY.symbol}</span>
        <input
          type="number"
          inputMode="decimal"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          placeholder="0.00"
          className="flex-1 text-2xl font-bold text-nupi-text bg-transparent outline-none placeholder:text-nupi-text-muted"
          max={max}
          min={0}
          step="0.01"
        />
      </div>

      {error && <p className="text-sm text-nupi-danger mt-1.5 ml-1">{error}</p>}

      {showQuick && (
        <div className="flex flex-wrap gap-2 mt-3">
          {QUICK_AMOUNTS.map(amt => (
            <button
              key={amt}
              onClick={() => onChange(amt.toString())}
              className={`px-3 py-1.5 rounded-full text-sm font-medium transition-all
                ${value === amt.toString()
                  ? 'bg-nupi-primary text-white'
                  : 'bg-gray-100 text-nupi-text-secondary hover:bg-gray-200'}`}
            >
              {CURRENCY.symbol} {amt.toLocaleString()}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
