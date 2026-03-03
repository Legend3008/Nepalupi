import { BANKS } from '../../utils/constants';

export function BankCard({ account, selected, onClick, showBalance = false }) {
  const bankInfo = BANKS.find(b => b.code === account.bankCode) || { name: account.bankCode, color: '#003893', short: account.bankCode };

  return (
    <button
      onClick={() => onClick?.(account)}
      className={`w-full flex items-center gap-3 p-4 rounded-xl border-2 transition-all duration-200
        ${selected ? 'border-nupi-primary bg-nupi-primary/5' : 'border-nupi-border bg-white hover:border-gray-300'}`}
    >
      <div className="w-12 h-12 rounded-xl flex items-center justify-center text-white font-bold text-sm"
           style={{ backgroundColor: bankInfo.color }}>
        {bankInfo.short?.slice(0, 3)}
      </div>
      <div className="flex-1 text-left">
        <p className="font-medium text-nupi-text">{bankInfo.name}</p>
        <p className="text-sm text-nupi-text-secondary">
          A/C: XXXX {account.accountNumber?.slice(-4)}
        </p>
        {showBalance && account.balance !== undefined && (
          <p className="text-sm font-semibold text-nupi-primary mt-0.5">
            Balance: रू {(account.balance / 100).toLocaleString()}
          </p>
        )}
      </div>
      <div className="flex flex-col items-end gap-1">
        {account.isPrimary && (
          <span className="text-[10px] font-medium bg-nupi-primary text-white px-2 py-0.5 rounded-full">PRIMARY</span>
        )}
        {selected && (
          <div className="w-5 h-5 rounded-full bg-nupi-primary flex items-center justify-center">
            <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
          </div>
        )}
      </div>
    </button>
  );
}

export function BankSelector({ accounts, selected, onSelect }) {
  return (
    <div className="space-y-2">
      {accounts.map(acc => (
        <BankCard
          key={acc.id}
          account={acc}
          selected={selected?.id === acc.id}
          onClick={onSelect}
        />
      ))}
    </div>
  );
}
