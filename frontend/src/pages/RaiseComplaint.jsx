import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { IoCheckmarkCircle, IoSearch, IoChevronDown } from 'react-icons/io5';

const recentTransactions = [
  { id: 'TXN20240615001', label: 'TXN...001 • Ram Kumar • Rs 2,500', amount: 250000 },
  { id: 'TXN20240614003', label: 'TXN...003 • Shop ABC • Rs 1,000', amount: 100000 },
  { id: 'TXN20240612007', label: 'TXN...007 • Electric Bill • Rs 500', amount: 50000 },
  { id: 'TXN20240610002', label: 'TXN...002 • Sita Devi • Rs 3,000', amount: 300000 },
];

const complaintTypes = [
  { value: 'TRANSACTION_FAILED', label: 'Transaction Failed' },
  { value: 'WRONG_AMOUNT', label: 'Wrong Amount Debited' },
  { value: 'DUPLICATE_TRANSACTION', label: 'Duplicate Transaction' },
  { value: 'NOT_RECEIVED', label: 'Amount Not Received' },
  { value: 'UNAUTHORIZED', label: 'Unauthorized Transaction' },
  { value: 'REFUND_NOT_RECEIVED', label: 'Refund Not Received' },
  { value: 'OTHER', label: 'Other Issue' },
];

export default function RaiseComplaint() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [selectedTxn, setSelectedTxn] = useState(null);
  const [complaintType, setComplaintType] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState({});
  const [success, setSuccess] = useState(false);
  const [txnSearch, setTxnSearch] = useState('');

  const filteredTxns = txnSearch
    ? recentTransactions.filter(t => t.label.toLowerCase().includes(txnSearch.toLowerCase()))
    : recentTransactions;

  const handleNext = () => {
    if (step === 1) {
      if (!selectedTxn) { setErrors({ txn: 'Please select a transaction' }); return; }
      setStep(2);
    } else if (step === 2) {
      const e = {};
      if (!complaintType) e.type = 'Please select complaint type';
      if (!description.trim()) e.description = 'Please describe the issue';
      if (description.trim().length < 10) e.description = 'Description must be at least 10 characters';
      if (Object.keys(e).length) { setErrors(e); return; }
      setStep(3);
    }
  };

  const handleSubmit = async () => {
    await new Promise(r => setTimeout(r, 1500));
    setSuccess(true);
  };

  if (success) {
    return (
      <div className="page-container flex items-center justify-center min-h-screen">
        <div className="text-center px-6">
          <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4 animate-bounce-in">
            <IoCheckmarkCircle className="w-12 h-12 text-green-500" />
          </div>
          <h2 className="text-xl font-bold text-green-700">Complaint Registered</h2>
          <p className="text-sm text-nupi-text-secondary mt-2">
            Your complaint has been registered. We'll resolve it within 7 working days.
          </p>
          <p className="text-xs text-nupi-text-muted mt-2">Complaint ID: CMP-{Date.now().toString().slice(-6)}</p>
          <button onClick={() => navigate('/complaints')} className="btn-primary mt-6 px-12">View Complaints</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Raise Complaint" showBack />

      {/* Progress */}
      <div className="px-4 mt-4">
        <div className="flex gap-2">
          {[1, 2, 3].map(s => (
            <div key={s} className={`flex-1 h-1 rounded-full ${step >= s ? 'bg-nupi-primary' : 'bg-gray-200'}`} />
          ))}
        </div>
        <p className="text-xs text-nupi-text-muted mt-1">
          Step {step}: {step === 1 ? 'Select Transaction' : step === 2 ? 'Complaint Details' : 'Review & Submit'}
        </p>
      </div>

      <div className="px-4 mt-6 space-y-5">
        {step === 1 && (
          <>
            <h3 className="text-lg font-semibold">Select Transaction</h3>
            <div className="relative">
              <IoSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input type="text" value={txnSearch} onChange={e => setTxnSearch(e.target.value)}
                placeholder="Search by transaction ID" className="input-field pl-9" />
            </div>
            <div className="space-y-2">
              {filteredTxns.map(txn => (
                <button key={txn.id} onClick={() => { setSelectedTxn(txn); setErrors({}); }}
                  className={`w-full text-left p-3 rounded-xl border-2 transition-all ${selectedTxn?.id === txn.id ? 'border-nupi-primary bg-nupi-primary/5' : 'border-gray-200'}`}>
                  <p className="text-sm font-medium">{txn.label}</p>
                  <p className="text-xs text-nupi-text-muted mt-0.5">ID: {txn.id}</p>
                </button>
              ))}
            </div>
            {errors.txn && <p className="text-xs text-red-500">{errors.txn}</p>}
            <button onClick={handleNext} className="btn-primary w-full">Continue</button>
          </>
        )}

        {step === 2 && (
          <>
            <h3 className="text-lg font-semibold">Complaint Details</h3>
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Complaint Type</label>
              <div className="space-y-2">
                {complaintTypes.map(ct => (
                  <button key={ct.value} onClick={() => { setComplaintType(ct.value); setErrors(p => ({ ...p, type: null })); }}
                    className={`w-full text-left p-3 rounded-xl border-2 transition-all text-sm ${complaintType === ct.value ? 'border-nupi-primary bg-nupi-primary/5 font-medium' : 'border-gray-200'}`}>
                    {ct.label}
                  </button>
                ))}
              </div>
              {errors.type && <p className="text-xs text-red-500 mt-1">{errors.type}</p>}
            </div>
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Describe the Issue</label>
              <textarea value={description} onChange={e => { setDescription(e.target.value); setErrors(p => ({ ...p, description: null })); }}
                placeholder="Please explain your issue in detail..." rows={4}
                className={`input-field resize-none ${errors.description ? 'border-red-400' : ''}`} maxLength={500} />
              <p className="text-xs text-nupi-text-muted text-right mt-1">{description.length}/500</p>
              {errors.description && <p className="text-xs text-red-500">{errors.description}</p>}
            </div>
            <div className="flex gap-3">
              <button onClick={() => setStep(1)} className="flex-1 btn-secondary">Back</button>
              <button onClick={handleNext} className="flex-1 btn-primary">Review</button>
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <h3 className="text-lg font-semibold">Review & Submit</h3>
            <div className="card bg-gray-50 space-y-3">
              {[
                ['Transaction', selectedTxn?.label],
                ['Complaint Type', complaintTypes.find(c => c.value === complaintType)?.label],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between text-sm">
                  <span className="text-nupi-text-muted">{label}</span>
                  <span className="font-medium text-right max-w-[60%]">{value}</span>
                </div>
              ))}
            </div>
            <div className="card bg-gray-50">
              <p className="text-xs text-nupi-text-muted mb-1">Description</p>
              <p className="text-sm">{description}</p>
            </div>
            <div className="card bg-yellow-50 border-yellow-200">
              <p className="text-sm text-yellow-800">
                <span className="font-semibold">Note:</span> Complaints are typically resolved within 7 working days. You'll receive updates via notifications.
              </p>
            </div>
            <div className="flex gap-3">
              <button onClick={() => setStep(2)} className="flex-1 btn-secondary">Back</button>
              <button onClick={handleSubmit} className="flex-1 btn-primary">Submit Complaint</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
