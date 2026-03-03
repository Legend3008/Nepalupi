import { useState } from 'react';
import Header from '../components/layout/Header';
import { TransactionList } from '../components/transaction/TransactionCard';
import { IoFilter, IoSearch, IoCalendarOutline } from 'react-icons/io5';

const DEMO_TRANSACTIONS = [
  { id: '1', type: 'SEND', status: 'SUCCESS', amountPaisa: 250000, receiverVpa: 'ram@nchl', receiverName: 'Ram Sharma', note: 'Lunch', createdAt: new Date(Date.now() - 3600000) },
  { id: '2', type: 'RECEIVE', status: 'SUCCESS', amountPaisa: 500000, senderVpa: 'sita@nabil', senderName: 'Sita Thapa', note: 'Rent share', createdAt: new Date(Date.now() - 7200000) },
  { id: '3', type: 'BILL_PAY', status: 'SUCCESS', amountPaisa: 120000, receiverVpa: 'nea@nchl', receiverName: 'NEA Electricity', note: 'Electricity', createdAt: new Date(Date.now() - 86400000) },
  { id: '4', type: 'QR_PAY', status: 'PENDING', amountPaisa: 35000, receiverVpa: 'cafe@nchl', receiverName: 'Himalayan Java', note: 'Coffee', createdAt: new Date(Date.now() - 172800000) },
  { id: '5', type: 'SEND', status: 'FAILED', amountPaisa: 1000000, receiverVpa: 'hari@global', receiverName: 'Hari Bahadur', note: 'Payment', createdAt: new Date(Date.now() - 259200000) },
  { id: '6', type: 'SELF_TRANSFER', status: 'SUCCESS', amountPaisa: 5000000, receiverVpa: 'self@nabil', receiverName: 'Self', note: 'Transfer', createdAt: new Date(Date.now() - 345600000) },
  { id: '7', type: 'RECEIVE', status: 'SUCCESS', amountPaisa: 2500000, senderVpa: 'office@nchl', senderName: 'Nepal Corp Ltd', note: 'Salary', createdAt: new Date(Date.now() - 604800000) },
  { id: '8', type: 'UPI_LITE', status: 'SUCCESS', amountPaisa: 10000, receiverVpa: 'tea@nchl', receiverName: 'Chiya Pasal', note: 'Tea', createdAt: new Date(Date.now() - 691200000) },
  { id: '9', type: 'MANDATE', status: 'SUCCESS', amountPaisa: 300000, receiverVpa: 'netflix@nchl', receiverName: 'Netflix', note: 'Monthly subscription', createdAt: new Date(Date.now() - 864000000) },
  { id: '10', type: 'COLLECT', status: 'SUCCESS', amountPaisa: 150000, senderVpa: 'gita@nic', senderName: 'Gita Devi', note: 'Dinner split', createdAt: new Date(Date.now() - 950400000) },
];

const filters = ['All', 'Sent', 'Received', 'Bills', 'Failed'];

export default function TransactionHistory() {
  const [activeFilter, setActiveFilter] = useState('All');
  const [searchTerm, setSearchTerm] = useState('');
  const [showSearch, setShowSearch] = useState(false);

  const filtered = DEMO_TRANSACTIONS.filter(txn => {
    if (activeFilter === 'Sent') return ['SEND', 'QR_PAY', 'UPI_LITE'].includes(txn.type);
    if (activeFilter === 'Received') return ['RECEIVE', 'COLLECT'].includes(txn.type);
    if (activeFilter === 'Bills') return ['BILL_PAY', 'MANDATE'].includes(txn.type);
    if (activeFilter === 'Failed') return txn.status === 'FAILED';
    return true;
  }).filter(txn =>
    !searchTerm ||
    txn.receiverName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    txn.senderName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    txn.note?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="page-container">
      <Header
        title="Transaction History"
        showBack
        rightAction={
          <div className="flex gap-1">
            <button onClick={() => setShowSearch(!showSearch)} className="p-2 rounded-full hover:bg-gray-100">
              <IoSearch className="w-5 h-5 text-nupi-text-secondary" />
            </button>
            <button className="p-2 rounded-full hover:bg-gray-100">
              <IoCalendarOutline className="w-5 h-5 text-nupi-text-secondary" />
            </button>
          </div>
        }
      />

      {/* Search bar */}
      {showSearch && (
        <div className="px-4 mt-3 animate-slide-down">
          <input
            type="text" value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
            placeholder="Search transactions..." className="input-field" autoFocus
          />
        </div>
      )}

      {/* Filters */}
      <div className="px-4 mt-3">
        <div className="flex gap-2 overflow-x-auto pb-2 no-scrollbar">
          {filters.map(f => (
            <button
              key={f}
              onClick={() => setActiveFilter(f)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all
                ${activeFilter === f ? 'bg-nupi-primary text-white' : 'bg-gray-100 text-nupi-text-secondary'}`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* Summary */}
      <div className="px-4 mt-4 flex gap-3">
        <div className="flex-1 card text-center py-3">
          <p className="text-xs text-nupi-text-muted">Total Sent</p>
          <p className="font-bold text-nupi-text">रू 18,500</p>
        </div>
        <div className="flex-1 card text-center py-3">
          <p className="text-xs text-nupi-text-muted">Total Received</p>
          <p className="font-bold text-green-600">रू 32,000</p>
        </div>
      </div>

      {/* Transaction List */}
      <div className="px-4 mt-4">
        <TransactionList
          transactions={filtered}
          emptyMessage={searchTerm ? 'No matching transactions' : 'No transactions in this category'}
        />
      </div>
    </div>
  );
}
