import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { BANKS } from '../utils/constants';
import { IoSearch, IoCheckmarkCircle } from 'react-icons/io5';

export default function LinkBank() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [selectedBank, setSelectedBank] = useState(null);
  const [phone, setPhone] = useState('');
  const [step, setStep] = useState('select'); // select | verify | success
  const [loading, setLoading] = useState(false);

  const filtered = BANKS.filter(b =>
    b.name.toLowerCase().includes(search.toLowerCase()) ||
    b.short.toLowerCase().includes(search.toLowerCase())
  );

  const handleVerify = async () => {
    setLoading(true);
    await new Promise(r => setTimeout(r, 2000));
    setStep('success');
    setLoading(false);
  };

  if (step === 'success') {
    return (
      <div className="page-container flex flex-col items-center justify-center px-6">
        <IoCheckmarkCircle className="w-20 h-20 text-green-500 mb-4 animate-bounce-in" />
        <h2 className="text-xl font-bold">Bank Linked!</h2>
        <p className="text-nupi-text-secondary text-sm mt-2 text-center">
          Your {selectedBank?.name} account has been successfully linked
        </p>
        <button onClick={() => navigate('/banks', { replace: true })} className="btn-primary mt-8 px-12">Done</button>
      </div>
    );
  }

  if (step === 'verify') {
    return (
      <div className="page-container">
        <Header title="Verify Account" showBack />
        <div className="px-4 mt-6 space-y-6 text-center">
          <div className="w-16 h-16 rounded-xl mx-auto flex items-center justify-center text-white font-bold text-xl"
               style={{ backgroundColor: selectedBank?.color }}>
            {selectedBank?.short?.slice(0, 3)}
          </div>
          <h3 className="font-semibold text-lg">{selectedBank?.name}</h3>
          <p className="text-sm text-nupi-text-secondary">
            Enter your registered mobile number to discover accounts
          </p>
          <input
            type="tel" value={phone} onChange={e => setPhone(e.target.value.replace(/\D/g, ''))}
            maxLength={10} placeholder="Enter mobile number"
            className="input-field text-center text-lg"
            inputMode="numeric"
          />
          <button onClick={handleVerify} disabled={phone.length < 10 || loading} className="btn-primary w-full">
            {loading ? 'Discovering Accounts...' : 'Find Accounts'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <Header title="Link Bank Account" showBack />
      <div className="px-4 mt-4">
        <div className="flex items-center gap-3 input-field mb-4">
          <IoSearch className="w-5 h-5 text-nupi-text-muted" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search bank" className="flex-1 bg-transparent outline-none" />
        </div>
        <p className="section-header">Select Your Bank ({filtered.length})</p>
        <div className="space-y-2">
          {filtered.map(bank => (
            <button
              key={bank.code}
              onClick={() => { setSelectedBank(bank); setStep('verify'); }}
              className="w-full flex items-center gap-3 card-hover"
            >
              <div className="w-11 h-11 rounded-xl flex items-center justify-center text-white font-bold text-xs"
                   style={{ backgroundColor: bank.color }}>
                {bank.short.slice(0, 3)}
              </div>
              <div className="flex-1 text-left">
                <p className="font-medium text-nupi-text text-sm">{bank.name}</p>
                <p className="text-xs text-nupi-text-muted">{bank.code}</p>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
