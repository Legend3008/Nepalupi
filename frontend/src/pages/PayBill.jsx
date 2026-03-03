import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import PinPad from '../components/common/PinPad';
import { CURRENCY, BILL_CATEGORIES } from '../utils/constants';

export default function PayBill() {
  const navigate = useNavigate();
  const { billerId } = useParams();
  const [consumerId, setConsumerId] = useState('');
  const [amount, setAmount] = useState('');
  const [step, setStep] = useState('form'); // form | confirm | pin | processing
  const [billData, setBillData] = useState(null);

  const category = BILL_CATEGORIES.find(c => c.id === billerId);

  const handleFetchBill = async () => {
    await new Promise(r => setTimeout(r, 1000));
    setBillData({
      name: `${category?.name || 'Bill'} - ${consumerId}`,
      amount: Math.floor(Math.random() * 5000) + 200,
      dueDate: '2024-03-15',
      billerName: category?.name || 'Biller',
    });
    setAmount(String(Math.floor(Math.random() * 5000) + 200));
    setStep('confirm');
  };

  const handlePinComplete = async (pin) => {
    setStep('processing');
    await new Promise(r => setTimeout(r, 2000));
    navigate('/transaction-success', {
      replace: true,
      state: {
        type: 'BILL_PAY',
        amount: parseFloat(amount) * 100,
        receiverName: billData?.billerName,
        txnId: 'BILL' + Date.now(),
        rrn: Math.random().toString().slice(2, 14),
        timestamp: new Date().toISOString(),
      }
    });
  };

  if (step === 'processing') {
    return (
      <div className="fixed inset-0 bg-white flex flex-col items-center justify-center">
        <div className="w-16 h-16 border-4 border-nupi-primary border-t-transparent rounded-full animate-spin mb-6" />
        <p className="text-lg font-semibold">Paying Bill...</p>
      </div>
    );
  }

  if (step === 'pin') {
    return (
      <div className="min-h-screen bg-white flex flex-col">
        <Header title="Enter MPIN" showBack />
        <div className="flex-1 flex items-center justify-center px-6">
          <PinPad
            title="Enter UPI MPIN"
            subtitle={`Pay ${CURRENCY.symbol} ${amount} for ${category?.name}`}
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
        <Header title="Confirm Bill Payment" showBack />
        <div className="px-4 mt-4 space-y-4">
          <div className="card text-center py-6">
            <span className="text-4xl mb-3 block">{category?.icon}</span>
            <p className="font-semibold text-lg">{billData?.billerName}</p>
            <p className="text-sm text-nupi-text-secondary">ID: {consumerId}</p>
            <p className="text-3xl font-bold mt-4">{CURRENCY.symbol} {parseFloat(amount).toLocaleString()}</p>
            <p className="text-xs text-nupi-text-muted mt-1">Due: {billData?.dueDate}</p>
          </div>
          <AmountInput value={amount} onChange={setAmount} showQuick={false} label="Bill Amount" />
          <button onClick={() => setStep('pin')} className="btn-primary w-full">
            Pay {CURRENCY.symbol} {parseFloat(amount || 0).toLocaleString()}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title={category?.name || 'Pay Bill'} showBack />
      <div className="px-4 mt-4 space-y-6">
        <div className="text-center py-4">
          <span className="text-5xl">{category?.icon || '📋'}</span>
          <h3 className="font-semibold text-lg mt-3">{category?.name || 'Bill Payment'}</h3>
        </div>
        <div>
          <label className="text-sm font-medium text-nupi-text-secondary mb-2 block">Consumer / Account Number</label>
          <input
            type="text" value={consumerId} onChange={e => setConsumerId(e.target.value)}
            placeholder="Enter your consumer ID" className="input-field"
          />
        </div>
        <button onClick={handleFetchBill} disabled={!consumerId} className="btn-primary w-full">
          Fetch Bill Details
        </button>
      </div>
    </div>
  );
}
