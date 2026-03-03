import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import PinPad from '../components/common/PinPad';
import { BankSelector } from '../components/bank/BankCard';
import { validateAmount } from '../utils/validators';
import { CURRENCY } from '../utils/constants';
import { IoSwapHorizontal } from 'react-icons/io5';

const demoAccounts = [
  { id: 'acc1', bankCode: 'NCHL', accountNumber: '1234567890', isPrimary: true },
  { id: 'acc2', bankCode: 'NABIL', accountNumber: '9876543210', isPrimary: false },
];

export default function SelfTransfer() {
  const navigate = useNavigate();
  const [fromAccount, setFromAccount] = useState(demoAccounts[0]);
  const [toAccount, setToAccount] = useState(demoAccounts[1]);
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [step, setStep] = useState('form');

  const handleSwap = () => {
    setFromAccount(toAccount);
    setToAccount(fromAccount);
  };

  const handleProceed = () => {
    const err = validateAmount(amount);
    if (err) { setError(err); return; }
    if (fromAccount.id === toAccount.id) { setError('Source and destination must be different'); return; }
    setStep('pin');
  };

  const handlePinComplete = async (pin) => {
    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      navigate('/transaction-success', {
        replace: true,
        state: {
          type: 'SELF_TRANSFER',
          amount: parseFloat(amount) * 100,
          from: `${fromAccount.bankCode} - XXXX${fromAccount.accountNumber.slice(-4)}`,
          to: `${toAccount.bankCode} - XXXX${toAccount.accountNumber.slice(-4)}`,
          txnId: 'TXN' + Date.now(),
          timestamp: new Date().toISOString(),
        }
      });
    } catch (err) {
      setStep('form');
      setError(err.message);
    }
  };

  if (step === 'pin') {
    return (
      <div className="min-h-screen bg-white flex flex-col">
        <Header title="Enter MPIN" showBack />
        <div className="flex-1 flex items-center justify-center px-6">
          <PinPad
            title="Enter UPI MPIN"
            subtitle={`Transfer ${CURRENCY.symbol} ${amount}`}
            onComplete={handlePinComplete}
            onCancel={() => setStep('form')}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Self Transfer" showBack />
      <div className="px-4 mt-4 space-y-6">
        {/* From Account */}
        <div>
          <label className="section-header">From</label>
          <BankSelector accounts={demoAccounts} selected={fromAccount} onSelect={setFromAccount} />
        </div>

        {/* Swap Button */}
        <div className="flex justify-center">
          <button onClick={handleSwap} className="p-3 rounded-full bg-nupi-primary/10 active:bg-nupi-primary/20 transition-colors">
            <IoSwapHorizontal className="w-6 h-6 text-nupi-primary rotate-90" />
          </button>
        </div>

        {/* To Account */}
        <div>
          <label className="section-header">To</label>
          <BankSelector accounts={demoAccounts} selected={toAccount} onSelect={setToAccount} />
        </div>

        {/* Amount */}
        <AmountInput value={amount} onChange={v => { setAmount(v); setError(''); }} error={error} />

        <button onClick={handleProceed} disabled={!amount} className="btn-primary w-full">
          Transfer
        </button>
      </div>
    </div>
  );
}
