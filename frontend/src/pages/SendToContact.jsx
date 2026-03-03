import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import PinPad from '../components/common/PinPad';
import Modal from '../components/common/Modal';
import { BankSelector } from '../components/bank/BankCard';
import { validateAmount } from '../utils/validators';
import { CURRENCY } from '../utils/constants';
import { formatCurrency } from '../utils/formatters';
import { IoPersonCircle, IoShieldCheckmark } from 'react-icons/io5';

const demoAccounts = [
  { id: 'acc1', bankCode: 'NCHL', accountNumber: '1234567890', isPrimary: true },
  { id: 'acc2', bankCode: 'NABIL', accountNumber: '9876543210', isPrimary: false },
];

export default function SendToContact() {
  const navigate = useNavigate();
  const { upiId } = useParams();
  const decodedUpi = decodeURIComponent(upiId || '');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [step, setStep] = useState('amount'); // amount | confirm | pin | processing
  const [selectedAccount, setSelectedAccount] = useState(demoAccounts[0]);
  const [showBankSelect, setShowBankSelect] = useState(false);

  const receiverName = decodedUpi.split('@')[0] || 'User';

  const handleProceed = () => {
    const err = validateAmount(amount);
    if (err) { setError(err); return; }
    setStep('confirm');
  };

  const handleConfirm = () => {
    setStep('pin');
  };

  const handlePinComplete = async (pin) => {
    setStep('processing');
    try {
      await new Promise(resolve => setTimeout(resolve, 2000));
      navigate('/transaction-success', {
        replace: true,
        state: {
          type: 'SEND',
          amount: parseFloat(amount) * 100,
          receiverName,
          receiverVpa: decodedUpi,
          txnId: 'TXN' + Date.now(),
          rrn: Math.random().toString().slice(2, 14),
          timestamp: new Date().toISOString(),
        }
      });
    } catch (err) {
      setStep('amount');
      setError(err.message || 'Transaction failed');
    }
  };

  if (step === 'processing') {
    return (
      <div className="fixed inset-0 bg-white flex flex-col items-center justify-center">
        <div className="w-16 h-16 border-4 border-nupi-primary border-t-transparent rounded-full animate-spin mb-6" />
        <p className="text-lg font-semibold text-nupi-text">Processing Payment</p>
        <p className="text-sm text-nupi-text-secondary mt-2">Please wait, do not close the app...</p>
        <div className="flex items-center gap-2 mt-4">
          <IoShieldCheckmark className="w-4 h-4 text-green-600" />
          <span className="text-xs text-nupi-text-muted">Secured by NUPI</span>
        </div>
      </div>
    );
  }

  if (step === 'pin') {
    return (
      <div className="min-h-screen bg-white flex flex-col">
        <Header title="Enter MPIN" showBack />
        <div className="px-4 py-4">
          <div className="card bg-gray-50 text-center">
            <p className="text-sm text-nupi-text-secondary">Paying</p>
            <p className="text-2xl font-bold text-nupi-text mt-1">{CURRENCY.symbol} {parseFloat(amount).toLocaleString()}</p>
            <p className="text-sm text-nupi-text-secondary mt-1">to {decodedUpi}</p>
          </div>
        </div>
        <div className="flex-1 flex items-center justify-center px-6">
          <PinPad
            title="Enter UPI MPIN"
            subtitle={`Paying ${CURRENCY.symbol} ${amount} to ${receiverName}`}
            onComplete={handlePinComplete}
            onCancel={() => setStep('confirm')}
          />
        </div>
      </div>
    );
  }

  if (step === 'confirm') {
    return (
      <div className="page-container">
        <Header title="Confirm Payment" showBack />
        <div className="px-4 mt-4 space-y-4">
          {/* Summary Card */}
          <div className="card text-center py-6">
            <div className="w-16 h-16 rounded-full bg-nupi-primary/10 flex items-center justify-center mx-auto mb-3">
              <IoPersonCircle className="w-10 h-10 text-nupi-primary" />
            </div>
            <p className="font-semibold text-nupi-text text-lg">{receiverName}</p>
            <p className="text-sm text-nupi-text-secondary">{decodedUpi}</p>
            <div className="mt-4 pt-4 border-t border-nupi-border">
              <p className="text-3xl font-bold text-nupi-text">
                {CURRENCY.symbol} {parseFloat(amount).toLocaleString()}
              </p>
              {note && <p className="text-sm text-nupi-text-muted mt-1">"{note}"</p>}
            </div>
          </div>

          {/* From Account */}
          <div className="card">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-nupi-text-secondary">Paying from</p>
              <button onClick={() => setShowBankSelect(true)} className="text-sm text-nupi-primary font-medium">
                Change
              </button>
            </div>
            <p className="font-medium text-nupi-text mt-1">
              {selectedAccount.bankCode} - XXXX {selectedAccount.accountNumber.slice(-4)}
            </p>
          </div>

          <button onClick={handleConfirm} className="btn-primary w-full">
            Pay {CURRENCY.symbol} {parseFloat(amount).toLocaleString()}
          </button>
        </div>

        <Modal isOpen={showBankSelect} onClose={() => setShowBankSelect(false)} title="Select Account">
          <BankSelector accounts={demoAccounts} selected={selectedAccount} onSelect={(acc) => { setSelectedAccount(acc); setShowBankSelect(false); }} />
        </Modal>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Send Money" showBack />
      <div className="px-4 mt-4 space-y-6">
        {/* Receiver Info */}
        <div className="card flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-nupi-primary/10 flex items-center justify-center">
            <IoPersonCircle className="w-8 h-8 text-nupi-primary" />
          </div>
          <div>
            <p className="font-semibold text-nupi-text">{receiverName}</p>
            <p className="text-sm text-nupi-text-secondary">{decodedUpi}</p>
          </div>
        </div>

        {/* Amount */}
        <AmountInput
          value={amount}
          onChange={(v) => { setAmount(v); setError(''); }}
          error={error}
        />

        {/* Note */}
        <div>
          <label className="text-sm font-medium text-nupi-text-secondary mb-2 block">Add Note (Optional)</label>
          <input
            type="text"
            value={note}
            onChange={e => setNote(e.target.value)}
            placeholder="e.g., Lunch money"
            className="input-field"
            maxLength={50}
          />
        </div>

        {/* Pay Button */}
        <button
          onClick={handleProceed}
          disabled={!amount || parseFloat(amount) <= 0}
          className="btn-primary w-full"
        >
          Proceed to Pay
        </button>
      </div>
    </div>
  );
}
