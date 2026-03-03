import { useState } from 'react';
import Header from '../components/layout/Header';
import PinPad from '../components/common/PinPad';
import { BankCard } from '../components/bank/BankCard';
import { CURRENCY } from '../utils/constants';
import { IoRefresh, IoEyeOutline, IoEyeOffOutline } from 'react-icons/io5';

const demoAccounts = [
  { id: 'acc1', bankCode: 'NCHL', accountNumber: '1234567890', isPrimary: true },
  { id: 'acc2', bankCode: 'NABIL', accountNumber: '9876543210', isPrimary: false },
];

export default function BalanceCheck() {
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [balance, setBalance] = useState(null);
  const [showPin, setShowPin] = useState(false);
  const [showBalance, setShowBalance] = useState(true);
  const [loading, setLoading] = useState(false);

  const handleAccountSelect = (acc) => {
    setSelectedAccount(acc);
    setBalance(null);
    setShowPin(true);
  };

  const handlePinComplete = async (pin) => {
    setShowPin(false);
    setLoading(true);
    await new Promise(r => setTimeout(r, 1500));
    setBalance(Math.floor(Math.random() * 50000000) + 100000);
    setLoading(false);
  };

  return (
    <div className="page-container">
      <Header title="Check Balance" showBack />

      <div className="px-4 mt-4 space-y-4">
        {/* Balance Display */}
        {balance !== null && (
          <div className="card gradient-primary text-center py-6">
            <p className="text-white/60 text-sm mb-2">Available Balance</p>
            <div className="flex items-center justify-center gap-2">
              {showBalance ? (
                <p className="text-3xl font-bold text-white">
                  {CURRENCY.symbol} {(balance / 100).toLocaleString('en-NP', { minimumFractionDigits: 2 })}
                </p>
              ) : (
                <p className="text-3xl font-bold text-white">{CURRENCY.symbol} ••••••</p>
              )}
              <button onClick={() => setShowBalance(!showBalance)}>
                {showBalance ? <IoEyeOffOutline className="w-5 h-5 text-white/60" /> : <IoEyeOutline className="w-5 h-5 text-white/60" />}
              </button>
            </div>
            <p className="text-white/50 text-xs mt-2">
              {selectedAccount?.bankCode} - XXXX {selectedAccount?.accountNumber?.slice(-4)}
            </p>
            <button onClick={() => setShowPin(true)} className="mt-3 bg-white/20 text-white text-sm px-4 py-2 rounded-lg flex items-center gap-1 mx-auto">
              <IoRefresh className="w-4 h-4" /> Refresh
            </button>
          </div>
        )}

        {/* Account Selection */}
        <div>
          <h3 className="section-header">Select Account</h3>
          <div className="space-y-2">
            {demoAccounts.map(acc => (
              <BankCard
                key={acc.id}
                account={acc}
                selected={selectedAccount?.id === acc.id}
                onClick={handleAccountSelect}
              />
            ))}
          </div>
        </div>

        {/* PIN Pad Modal */}
        {showPin && (
          <div className="fixed inset-0 z-50 bg-white flex items-center justify-center">
            <div className="w-full max-w-sm px-6">
              <PinPad
                title="Enter UPI MPIN"
                subtitle="To check balance securely"
                onComplete={handlePinComplete}
                onCancel={() => setShowPin(false)}
                loading={loading}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
