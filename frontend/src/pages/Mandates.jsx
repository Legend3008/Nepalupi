import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import StatusBadge from '../components/common/StatusBadge';
import { CURRENCY } from '../utils/constants';
import { formatDate, formatCurrency } from '../utils/formatters';
import { IoAdd, IoCalendar, IoPause, IoPlay, IoClose, IoChevronForward } from 'react-icons/io5';

const demoMandates = [
  { id: 'm1', merchantName: 'Nepal Telecom', merchantVpa: 'ntc@nchl', amount: 99900, frequency: 'MONTHLY', status: 'ACTIVE', startDate: '2024-01-15', endDate: '2025-01-15', lastDebit: '2024-06-15', nextDebit: '2024-07-15', purpose: 'Mobile Recharge' },
  { id: 'm2', merchantName: 'WorldLink', merchantVpa: 'worldlink@nchl', amount: 149900, frequency: 'MONTHLY', status: 'ACTIVE', startDate: '2024-02-01', endDate: '2025-02-01', lastDebit: '2024-06-01', nextDebit: '2024-07-01', purpose: 'Internet' },
  { id: 'm3', merchantName: 'Dish Home', merchantVpa: 'dish@nchl', amount: 65000, frequency: 'MONTHLY', status: 'PAUSED', startDate: '2024-03-10', endDate: '2025-03-10', lastDebit: '2024-05-10', nextDebit: null, purpose: 'TV Subscription' },
  { id: 'm4', merchantName: 'SBI Life Insurance', merchantVpa: 'sbi-life@nchl', amount: 500000, frequency: 'YEARLY', status: 'ACTIVE', startDate: '2024-01-01', endDate: '2029-01-01', lastDebit: '2024-01-01', nextDebit: '2025-01-01', purpose: 'Life Insurance Premium' },
];

const statusColors = {
  ACTIVE: 'bg-green-100 text-green-700',
  PAUSED: 'bg-yellow-100 text-yellow-700',
  REVOKED: 'bg-red-100 text-red-700',
  EXPIRED: 'bg-gray-100 text-gray-600',
  PENDING: 'bg-blue-100 text-blue-700',
};

const frequencyLabels = { MONTHLY: 'Monthly', WEEKLY: 'Weekly', DAILY: 'Daily', YEARLY: 'Yearly', ONE_TIME: 'One-time' };

export default function Mandates() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('ALL');
  const [selectedMandate, setSelectedMandate] = useState(null);

  const filters = ['ALL', 'ACTIVE', 'PAUSED', 'EXPIRED'];
  const filtered = filter === 'ALL' ? demoMandates : demoMandates.filter(m => m.status === filter);

  const handleAction = (action, mandate) => {
    setSelectedMandate(null);
    // API call would go here
  };

  return (
    <div className="page-container">
      <Header title="Mandates" showBack rightAction={
        <button onClick={() => navigate('/mandates/create')} className="p-2 rounded-full hover:bg-gray-100">
          <IoAdd className="w-5 h-5 text-nupi-primary" />
        </button>
      } />

      <div className="px-4 mt-4 space-y-4">
        {/* Summary */}
        <div className="flex gap-3">
          <div className="flex-1 card text-center py-3 bg-green-50 border-green-200">
            <p className="text-2xl font-bold text-green-700">{demoMandates.filter(m => m.status === 'ACTIVE').length}</p>
            <p className="text-xs text-green-600">Active</p>
          </div>
          <div className="flex-1 card text-center py-3 bg-yellow-50 border-yellow-200">
            <p className="text-2xl font-bold text-yellow-700">{demoMandates.filter(m => m.status === 'PAUSED').length}</p>
            <p className="text-xs text-yellow-600">Paused</p>
          </div>
          <div className="flex-1 card text-center py-3 bg-gray-50 border-gray-200">
            <p className="text-2xl font-bold text-gray-600">{demoMandates.length}</p>
            <p className="text-xs text-gray-500">Total</p>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-2 overflow-x-auto scrollbar-none">
          {filters.map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-all ${filter === f ? 'bg-nupi-primary text-white' : 'bg-gray-100 text-nupi-text-secondary'}`}>
              {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {/* Mandates List */}
        <div className="space-y-3">
          {filtered.map(mandate => (
            <div key={mandate.id} onClick={() => setSelectedMandate(mandate)}
              className="card cursor-pointer hover:shadow-md transition-shadow">
              <div className="flex justify-between items-start mb-2">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-nupi-primary/10 flex items-center justify-center">
                    <IoCalendar className="w-5 h-5 text-nupi-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm">{mandate.merchantName}</p>
                    <p className="text-xs text-nupi-text-muted">{mandate.purpose}</p>
                  </div>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColors[mandate.status]}`}>
                  {mandate.status}
                </span>
              </div>
              <div className="flex justify-between items-center mt-3 pt-3 border-t border-gray-100">
                <div>
                  <p className="text-xs text-nupi-text-muted">{frequencyLabels[mandate.frequency]}</p>
                  <p className="font-bold text-nupi-primary">{formatCurrency(mandate.amount / 100)}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-nupi-text-muted">Next Debit</p>
                  <p className="text-xs font-medium">{mandate.nextDebit ? formatDate(new Date(mandate.nextDebit)) : 'N/A'}</p>
                </div>
              </div>
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="text-center py-12">
              <IoCalendar className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-nupi-text-muted">No mandates found</p>
            </div>
          )}
        </div>

        {/* Create New */}
        <button onClick={() => navigate('/mandates/create')}
          className="w-full border-2 border-dashed border-nupi-primary/30 rounded-xl py-3 flex items-center justify-center gap-2 text-nupi-primary font-medium">
          <IoAdd className="w-5 h-5" /> Create New Mandate
        </button>
      </div>

      {/* Mandate Detail Modal */}
      {selectedMandate && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-end" onClick={() => setSelectedMandate(null)}>
          <div className="bg-white rounded-t-3xl w-full p-6 animate-slide-up max-h-[80vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">{selectedMandate.merchantName}</h3>
              <button onClick={() => setSelectedMandate(null)} className="p-1"><IoClose className="w-5 h-5" /></button>
            </div>
            <div className="space-y-3">
              {[
                ['Purpose', selectedMandate.purpose],
                ['UPI ID', selectedMandate.merchantVpa],
                ['Amount', formatCurrency(selectedMandate.amount / 100)],
                ['Frequency', frequencyLabels[selectedMandate.frequency]],
                ['Start Date', formatDate(new Date(selectedMandate.startDate))],
                ['End Date', formatDate(new Date(selectedMandate.endDate))],
                ['Last Debit', selectedMandate.lastDebit ? formatDate(new Date(selectedMandate.lastDebit)) : 'N/A'],
                ['Next Debit', selectedMandate.nextDebit ? formatDate(new Date(selectedMandate.nextDebit)) : 'N/A'],
                ['Status', selectedMandate.status],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between py-2 border-b border-gray-100">
                  <span className="text-sm text-nupi-text-muted">{label}</span>
                  <span className="text-sm font-medium">{value}</span>
                </div>
              ))}
            </div>
            <div className="flex gap-3 mt-6">
              {selectedMandate.status === 'ACTIVE' && (
                <>
                  <button onClick={() => handleAction('pause', selectedMandate)} className="flex-1 btn-secondary flex items-center justify-center gap-2">
                    <IoPause className="w-4 h-4" /> Pause
                  </button>
                  <button onClick={() => handleAction('revoke', selectedMandate)} className="flex-1 bg-red-50 text-red-600 py-2.5 rounded-xl font-medium flex items-center justify-center gap-2">
                    <IoClose className="w-4 h-4" /> Revoke
                  </button>
                </>
              )}
              {selectedMandate.status === 'PAUSED' && (
                <>
                  <button onClick={() => handleAction('resume', selectedMandate)} className="flex-1 btn-primary flex items-center justify-center gap-2">
                    <IoPlay className="w-4 h-4" /> Resume
                  </button>
                  <button onClick={() => handleAction('revoke', selectedMandate)} className="flex-1 bg-red-50 text-red-600 py-2.5 rounded-xl font-medium flex items-center justify-center gap-2">
                    <IoClose className="w-4 h-4" /> Revoke
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
