import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { BILL_CATEGORIES } from '../utils/constants';
import { IoChevronForward, IoTimeOutline, IoSearch } from 'react-icons/io5';
import { useState } from 'react';

const savedBillers = [
  { id: '1', name: 'NEA - Kathmandu', category: 'electricity', consumerId: '01-23456', lastPaid: '2024-02-15' },
  { id: '2', name: 'NTC Postpaid', category: 'mobile', consumerId: '98XXXXXXXX', lastPaid: '2024-02-20' },
];

export default function BillPayments() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const filtered = BILL_CATEGORIES.filter(c =>
    c.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="page-container">
      <Header title="Bill Payments" showBack />

      <div className="px-4 mt-4 space-y-6">
        {/* Search */}
        <div className="flex items-center gap-3 input-field">
          <IoSearch className="w-5 h-5 text-nupi-text-muted" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search bills" className="flex-1 bg-transparent outline-none" />
        </div>

        {/* Saved Billers */}
        {savedBillers.length > 0 && (
          <div>
            <h3 className="section-header flex items-center gap-1">
              <IoTimeOutline className="w-4 h-4" /> Saved Billers
            </h3>
            <div className="card p-0 divide-y divide-nupi-border/50">
              {savedBillers.map(biller => (
                <button
                  key={biller.id}
                  onClick={() => navigate(`/pay-bill/${biller.id}`)}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition-colors"
                >
                  <span className="text-2xl">{BILL_CATEGORIES.find(c => c.id === biller.category)?.icon || '📋'}</span>
                  <div className="flex-1 text-left">
                    <p className="font-medium text-nupi-text text-sm">{biller.name}</p>
                    <p className="text-xs text-nupi-text-muted">ID: {biller.consumerId}</p>
                  </div>
                  <IoChevronForward className="w-4 h-4 text-nupi-text-muted" />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Categories */}
        <div>
          <h3 className="section-header">All Categories</h3>
          <div className="grid grid-cols-3 gap-3">
            {filtered.map(cat => (
              <button
                key={cat.id}
                onClick={() => navigate(`/pay-bill/${cat.id}`)}
                className="card-hover flex flex-col items-center gap-2 py-4"
              >
                <span className="text-3xl">{cat.icon}</span>
                <span className="text-xs font-medium text-nupi-text-secondary text-center">{cat.name}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
