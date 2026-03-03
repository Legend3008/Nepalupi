import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { BankCard } from '../components/bank/BankCard';
import { IoAdd, IoChevronForward, IoTrash, IoStar } from 'react-icons/io5';

const demoAccounts = [
  { id: 'acc1', bankCode: 'NCHL', accountNumber: '1234567890', isPrimary: true, isVerified: true },
  { id: 'acc2', bankCode: 'NABIL', accountNumber: '9876543210', isPrimary: false, isVerified: true },
  { id: 'acc3', bankCode: 'NIC', accountNumber: '5555666677', isPrimary: false, isVerified: true },
];

export default function BankAccounts() {
  const navigate = useNavigate();
  const [accounts] = useState(demoAccounts);

  return (
    <div className="page-container">
      <Header title="Bank Accounts" showBack />

      <div className="px-4 mt-4 space-y-4">
        {/* Linked Accounts */}
        <div>
          <h3 className="section-header">Linked Accounts ({accounts.length})</h3>
          <div className="space-y-3">
            {accounts.map(acc => (
              <div key={acc.id} className="card p-0">
                <BankCard account={acc} onClick={() => {}} />
                <div className="flex border-t border-nupi-border/50">
                  <button className="flex-1 text-center py-2.5 text-sm font-medium text-nupi-primary flex items-center justify-center gap-1">
                    <IoStar className="w-4 h-4" /> Set Primary
                  </button>
                  <div className="w-px bg-nupi-border/50" />
                  <button className="flex-1 text-center py-2.5 text-sm font-medium text-nupi-text-secondary flex items-center justify-center gap-1"
                    onClick={() => navigate('/balance')}>
                    Check Balance
                  </button>
                  <div className="w-px bg-nupi-border/50" />
                  <button className="flex-1 text-center py-2.5 text-sm font-medium text-nupi-danger flex items-center justify-center gap-1">
                    <IoTrash className="w-4 h-4" /> Remove
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Add New Bank */}
        <button
          onClick={() => navigate('/link-bank')}
          className="w-full card-hover flex items-center gap-3 border-2 border-dashed border-nupi-primary/30"
        >
          <div className="w-12 h-12 rounded-xl bg-nupi-primary/10 flex items-center justify-center">
            <IoAdd className="w-6 h-6 text-nupi-primary" />
          </div>
          <div className="flex-1 text-left">
            <p className="font-medium text-nupi-primary">Add New Bank Account</p>
            <p className="text-xs text-nupi-text-muted">Link your bank for UPI payments</p>
          </div>
          <IoChevronForward className="w-5 h-5 text-nupi-text-muted" />
        </button>

        {/* UPI IDs */}
        <div>
          <h3 className="section-header mt-6">Your UPI IDs</h3>
          <div className="card">
            {accounts.map(acc => (
              <div key={acc.id} className="flex items-center justify-between py-2 border-b border-nupi-border/50 last:border-0">
                <div>
                  <p className="font-medium text-nupi-text text-sm">user@{acc.bankCode.toLowerCase()}</p>
                  <p className="text-xs text-nupi-text-muted">XXXX {acc.accountNumber.slice(-4)}</p>
                </div>
                {acc.isPrimary && <span className="text-[10px] bg-nupi-primary text-white px-2 py-0.5 rounded-full">PRIMARY</span>}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
