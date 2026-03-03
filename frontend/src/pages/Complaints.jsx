import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import StatusBadge from '../components/common/StatusBadge';
import { formatDate, formatCurrency } from '../utils/formatters';
import { IoAdd, IoChatbubbles, IoChevronForward, IoTime, IoCheckmarkCircle, IoCloseCircle } from 'react-icons/io5';

const demoComplaints = [
  { id: 'c1', txnId: 'TXN20240615001', type: 'TRANSACTION_FAILED', status: 'OPEN', amount: 250000, receiverName: 'Ram Kumar', description: 'Amount debited but not credited to receiver', createdAt: new Date(Date.now() - 86400000 * 2), updatedAt: new Date(Date.now() - 86400000) },
  { id: 'c2', txnId: 'TXN20240610005', type: 'WRONG_AMOUNT', status: 'IN_PROGRESS', amount: 100000, receiverName: 'Shop ABC', description: 'Incorrect amount charged', createdAt: new Date(Date.now() - 86400000 * 5), updatedAt: new Date(Date.now() - 86400000 * 3) },
  { id: 'c3', txnId: 'TXN20240601002', type: 'DUPLICATE_TRANSACTION', status: 'RESOLVED', amount: 50000, receiverName: 'Electric Bill', description: 'Charged twice for same bill', createdAt: new Date(Date.now() - 86400000 * 15), updatedAt: new Date(Date.now() - 86400000 * 10), resolution: 'Amount refunded to source account' },
];

const complaintStatusColors = {
  OPEN: 'bg-red-100 text-red-700',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
  RESOLVED: 'bg-green-100 text-green-700',
  CLOSED: 'bg-gray-100 text-gray-600',
};

const complaintStatusIcons = {
  OPEN: IoTime,
  IN_PROGRESS: IoChatbubbles,
  RESOLVED: IoCheckmarkCircle,
  CLOSED: IoCloseCircle,
};

export default function Complaints() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('ALL');
  const [selected, setSelected] = useState(null);

  const filters = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED'];
  const filtered = filter === 'ALL' ? demoComplaints : demoComplaints.filter(c => c.status === filter);

  return (
    <div className="page-container pb-24">
      <Header title="Complaints" showBack rightAction={
        <button onClick={() => navigate('/complaints/raise')} className="p-2 rounded-full hover:bg-gray-100">
          <IoAdd className="w-5 h-5 text-nupi-primary" />
        </button>
      } />

      <div className="px-4 mt-4 space-y-4">
        {/* Summary */}
        <div className="flex gap-3">
          <div className="flex-1 card text-center py-3 bg-red-50 border-red-200">
            <p className="text-2xl font-bold text-red-700">{demoComplaints.filter(c => c.status === 'OPEN').length}</p>
            <p className="text-xs text-red-600">Open</p>
          </div>
          <div className="flex-1 card text-center py-3 bg-yellow-50 border-yellow-200">
            <p className="text-2xl font-bold text-yellow-700">{demoComplaints.filter(c => c.status === 'IN_PROGRESS').length}</p>
            <p className="text-xs text-yellow-600">In Progress</p>
          </div>
          <div className="flex-1 card text-center py-3 bg-green-50 border-green-200">
            <p className="text-2xl font-bold text-green-700">{demoComplaints.filter(c => c.status === 'RESOLVED').length}</p>
            <p className="text-xs text-green-600">Resolved</p>
          </div>
        </div>

        {/* Filters */}
        <div className="flex gap-2 overflow-x-auto scrollbar-none">
          {filters.map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-all ${filter === f ? 'bg-nupi-primary text-white' : 'bg-gray-100 text-nupi-text-secondary'}`}>
              {f === 'ALL' ? 'All' : f.replace('_', ' ').replace(/\b\w/g, c => c.toUpperCase()).replace('In Progress', 'In Progress')}
            </button>
          ))}
        </div>

        {/* Complaints List */}
        <div className="space-y-3">
          {filtered.map(complaint => {
            const StatusIcon = complaintStatusIcons[complaint.status];
            return (
              <div key={complaint.id} onClick={() => setSelected(complaint)}
                className="card cursor-pointer hover:shadow-md transition-shadow">
                <div className="flex justify-between items-start">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${complaintStatusColors[complaint.status]}`}>
                      <StatusIcon className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="font-semibold text-sm">{complaint.type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}</p>
                      <p className="text-xs text-nupi-text-muted">{complaint.receiverName}</p>
                    </div>
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${complaintStatusColors[complaint.status]}`}>
                    {complaint.status.replace('_', ' ')}
                  </span>
                </div>
                <div className="flex justify-between items-center mt-3 pt-3 border-t border-gray-100">
                  <p className="text-xs text-nupi-text-muted">{formatDate(complaint.createdAt)}</p>
                  <p className="font-bold text-sm">{formatCurrency(complaint.amount / 100)}</p>
                </div>
              </div>
            );
          })}
          {filtered.length === 0 && (
            <div className="text-center py-12">
              <IoChatbubbles className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-nupi-text-muted">No complaints found</p>
            </div>
          )}
        </div>

        {/* Raise New */}
        <button onClick={() => navigate('/complaints/raise')}
          className="w-full border-2 border-dashed border-nupi-primary/30 rounded-xl py-3 flex items-center justify-center gap-2 text-nupi-primary font-medium">
          <IoAdd className="w-5 h-5" /> Raise New Complaint
        </button>
      </div>

      {/* Detail Modal */}
      {selected && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-end" onClick={() => setSelected(null)}>
          <div className="bg-white rounded-t-3xl w-full p-6 animate-slide-up max-h-[80vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-4">Complaint Details</h3>
            <div className="space-y-3">
              {[
                ['Complaint ID', selected.id],
                ['Transaction ID', selected.txnId],
                ['Type', selected.type.replace(/_/g, ' ')],
                ['Amount', formatCurrency(selected.amount / 100)],
                ['Receiver', selected.receiverName],
                ['Status', selected.status.replace('_', ' ')],
                ['Filed On', formatDate(selected.createdAt)],
                ['Last Updated', formatDate(selected.updatedAt)],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between py-2 border-b border-gray-100">
                  <span className="text-sm text-nupi-text-muted">{label}</span>
                  <span className="text-sm font-medium">{value}</span>
                </div>
              ))}
            </div>
            <div className="mt-4 p-3 bg-gray-50 rounded-xl">
              <p className="text-xs text-nupi-text-muted">Description</p>
              <p className="text-sm mt-1">{selected.description}</p>
            </div>
            {selected.resolution && (
              <div className="mt-3 p-3 bg-green-50 rounded-xl">
                <p className="text-xs text-green-600">Resolution</p>
                <p className="text-sm text-green-800 mt-1">{selected.resolution}</p>
              </div>
            )}
            <button onClick={() => setSelected(null)} className="btn-primary w-full mt-6">Close</button>
          </div>
        </div>
      )}
    </div>
  );
}
