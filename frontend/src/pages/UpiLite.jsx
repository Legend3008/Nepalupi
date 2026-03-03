import { useState } from 'react';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import PinPad from '../components/common/PinPad';
import { TransactionList } from '../components/transaction/TransactionCard';
import { CURRENCY, LIMITS } from '../utils/constants';
import { IoFlash, IoAdd, IoWallet, IoChevronForward } from 'react-icons/io5';

const liteHistory = [
  { id: 'l1', type: 'UPI_LITE', status: 'SUCCESS', amountPaisa: 5000, receiverName: 'Tea Stall', receiverVpa: 'tea@nchl', note: 'Tea', createdAt: new Date(Date.now() - 600000) },
  { id: 'l2', type: 'UPI_LITE', status: 'SUCCESS', amountPaisa: 15000, receiverName: 'Bus', receiverVpa: 'bus@nchl', note: 'Bus fare', createdAt: new Date(Date.now() - 3600000) },
];

export default function UpiLite() {
  const [enabled, setEnabled] = useState(true);
  const [balance] = useState(175000); // In paisa
  const [showTopUp, setShowTopUp] = useState(false);
  const [topUpAmount, setTopUpAmount] = useState('');
  const [showPin, setShowPin] = useState(false);
  const maxBalance = LIMITS.UPI_LITE_WALLET;

  const handleTopUp = () => {
    setShowTopUp(false);
    setShowPin(true);
  };

  const handlePinComplete = async (pin) => {
    setShowPin(false);
    // Simulate top-up
  };

  if (!enabled) {
    return (
      <div className="page-container">
        <Header title="UPI Lite" showBack />
        <div className="px-4 mt-8 text-center">
          <div className="w-20 h-20 rounded-full bg-cyan-50 flex items-center justify-center mx-auto mb-4">
            <IoFlash className="w-10 h-10 text-cyan-500" />
          </div>
          <h2 className="text-xl font-bold">UPI Lite</h2>
          <p className="text-sm text-nupi-text-secondary mt-2 max-w-xs mx-auto">
            Make small payments instantly without entering MPIN. Payments up to {CURRENCY.symbol} {LIMITS.UPI_LITE_MAX / 100} per transaction.
          </p>
          <ul className="text-left mt-6 space-y-3 max-w-sm mx-auto">
            {['No MPIN required for small payments', 'Instant offline transactions', 'Auto top-up from linked bank', `Max wallet balance: ${CURRENCY.symbol} ${maxBalance / 100}`].map((f, i) => (
              <li key={i} className="flex items-center gap-2 text-sm text-nupi-text-secondary">
                <div className="w-5 h-5 rounded-full bg-cyan-100 flex items-center justify-center flex-shrink-0">
                  <span className="text-cyan-600 text-xs">✓</span>
                </div>
                {f}
              </li>
            ))}
          </ul>
          <button onClick={() => setEnabled(true)} className="btn-primary mt-8 px-12">Enable UPI Lite</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="UPI Lite" showBack />
      <div className="px-4 mt-4 space-y-4">
        {/* Balance Card */}
        <div className="card bg-gradient-to-br from-cyan-500 to-blue-600 text-white py-6">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <IoFlash className="w-5 h-5" />
                <span className="text-white/80 text-sm font-medium">UPI Lite Balance</span>
              </div>
              <p className="text-3xl font-bold">{CURRENCY.symbol} {(balance / 100).toFixed(2)}</p>
              <p className="text-white/60 text-xs mt-1">Max: {CURRENCY.symbol} {maxBalance / 100}</p>
            </div>
            <button onClick={() => setShowTopUp(true)} className="bg-white/20 p-3 rounded-xl">
              <IoAdd className="w-6 h-6 text-white" />
            </button>
          </div>
          {/* Progress bar */}
          <div className="mt-4 bg-white/20 rounded-full h-2">
            <div className="bg-white rounded-full h-2 transition-all" style={{ width: `${(balance / maxBalance) * 100}%` }} />
          </div>
        </div>

        {/* Quick Info */}
        <div className="flex gap-3">
          <div className="flex-1 card text-center py-3">
            <IoWallet className="w-5 h-5 text-cyan-600 mx-auto mb-1" />
            <p className="text-xs text-nupi-text-muted">Per Txn Limit</p>
            <p className="font-semibold text-sm">{CURRENCY.symbol} {LIMITS.UPI_LITE_MAX / 100}</p>
          </div>
          <div className="flex-1 card text-center py-3">
            <IoFlash className="w-5 h-5 text-green-600 mx-auto mb-1" />
            <p className="text-xs text-nupi-text-muted">No MPIN</p>
            <p className="font-semibold text-sm">Instant Pay</p>
          </div>
        </div>

        {/* Recent UPI Lite Transactions */}
        <TransactionList transactions={liteHistory} title="Recent Lite Payments" />

        {/* Disable */}
        <button onClick={() => setEnabled(false)} className="w-full text-sm text-nupi-danger font-medium py-3">
          Disable UPI Lite
        </button>
      </div>

      {/* Top-up Modal */}
      {showTopUp && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-end">
          <div className="bg-white rounded-t-3xl w-full p-6 animate-slide-up">
            <h3 className="text-lg font-semibold mb-4">Top Up UPI Lite</h3>
            <AmountInput
              value={topUpAmount} onChange={setTopUpAmount}
              max={maxBalance / 100} label="Top-up Amount"
            />
            <div className="flex gap-3 mt-6">
              <button onClick={() => setShowTopUp(false)} className="flex-1 btn-secondary">Cancel</button>
              <button onClick={handleTopUp} disabled={!topUpAmount} className="flex-1 btn-primary">Top Up</button>
            </div>
          </div>
        </div>
      )}

      {showPin && (
        <div className="fixed inset-0 z-50 bg-white flex items-center justify-center">
          <div className="w-full max-w-sm px-6">
            <PinPad title="Enter MPIN" subtitle="To top up UPI Lite" onComplete={handlePinComplete} onCancel={() => setShowPin(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
