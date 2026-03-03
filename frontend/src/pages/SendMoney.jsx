import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { IoSearch, IoPersonCircle, IoQrCode, IoCall, IoBusiness, IoChevronForward, IoTimeOutline } from 'react-icons/io5';

const recentContacts = [
  { name: 'Ram Sharma', upiId: 'ram@nchl', avatar: null },
  { name: 'Sita Thapa', upiId: 'sita@nabil', avatar: null },
  { name: 'Hari Bahadur', upiId: 'hari@global', avatar: null },
  { name: 'Gita Devi', upiId: 'gita@nic', avatar: null },
  { name: 'Prakash KC', upiId: 'prakash@nchl', avatar: null },
];

export default function SendMoney() {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('upi'); // upi | phone | contacts

  const tabs = [
    { id: 'upi', label: 'UPI ID', icon: IoBusiness },
    { id: 'phone', label: 'Phone', icon: IoCall },
    { id: 'qr', label: 'QR Code', icon: IoQrCode },
  ];

  const handleUpiSubmit = () => {
    if (searchTerm.includes('@')) {
      navigate(`/send/${encodeURIComponent(searchTerm)}`);
    }
  };

  const filteredContacts = recentContacts.filter(c =>
    c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.upiId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="page-container">
      <Header title="Send Money" showBack showHelp />

      {/* Tabs */}
      <div className="px-4 mt-4">
        <div className="flex bg-gray-100 rounded-xl p-1">
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => { if (tab.id === 'qr') navigate('/scan'); else setActiveTab(tab.id); }}
              className={`flex-1 flex items-center justify-center gap-1.5 py-2.5 rounded-lg text-sm font-medium transition-all
                ${activeTab === tab.id ? 'bg-white shadow-sm text-nupi-primary' : 'text-nupi-text-secondary'}`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Search / Input */}
      <div className="px-4 mt-4">
        <div className="flex items-center gap-3 input-field">
          <IoSearch className="w-5 h-5 text-nupi-text-muted flex-shrink-0" />
          <input
            type={activeTab === 'phone' ? 'tel' : 'text'}
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder={activeTab === 'phone' ? 'Enter phone number' : 'Enter UPI ID (e.g., name@bank)'}
            className="flex-1 bg-transparent outline-none text-nupi-text placeholder:text-nupi-text-muted"
            inputMode={activeTab === 'phone' ? 'numeric' : 'text'}
            onKeyDown={e => e.key === 'Enter' && handleUpiSubmit()}
          />
        </div>
        {searchTerm.includes('@') && (
          <button onClick={handleUpiSubmit} className="btn-primary w-full mt-3">
            Verify & Pay
          </button>
        )}
      </div>

      {/* Recent Contacts */}
      <div className="px-4 mt-6">
        <div className="flex items-center gap-2 mb-3">
          <IoTimeOutline className="w-4 h-4 text-nupi-text-secondary" />
          <h3 className="section-header mb-0">Recent</h3>
        </div>
        <div className="card p-0 divide-y divide-nupi-border/50">
          {filteredContacts.map(contact => (
            <button
              key={contact.upiId}
              onClick={() => navigate(`/send/${encodeURIComponent(contact.upiId)}`)}
              className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 active:bg-gray-100 transition-colors"
            >
              <div className="w-10 h-10 rounded-full bg-nupi-primary/10 flex items-center justify-center">
                <IoPersonCircle className="w-6 h-6 text-nupi-primary" />
              </div>
              <div className="flex-1 text-left">
                <p className="font-medium text-nupi-text">{contact.name}</p>
                <p className="text-xs text-nupi-text-muted">{contact.upiId}</p>
              </div>
              <IoChevronForward className="w-4 h-4 text-nupi-text-muted" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
