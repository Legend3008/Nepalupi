import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Header from '../components/layout/Header';
import { CURRENCY } from '../utils/constants';
import {
  IoPersonCircle, IoBusiness, IoQrCode, IoLockClosed, IoSettings,
  IoHelpCircle, IoLogOut, IoChevronForward, IoShield, IoNotifications,
  IoChatbubbles, IoWallet, IoCard, IoFingerPrint
} from 'react-icons/io5';

export default function Profile() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [showLogout, setShowLogout] = useState(false);

  const userName = user?.name || 'NUPI User';
  const phone = user?.phone || '98XXXXXXXX';
  const upiId = user?.vpa || 'user@nchl';

  const menuSections = [
    {
      title: 'Account',
      items: [
        { icon: IoBusiness, label: 'Bank Accounts', sub: '3 linked', path: '/bank-accounts', color: 'text-blue-600 bg-blue-100' },
        { icon: IoQrCode, label: 'My QR Code', sub: 'Receive payments', path: '/my-qr', color: 'text-purple-600 bg-purple-100' },
        { icon: IoWallet, label: 'UPI Lite', sub: 'Small payments', path: '/upi-lite', color: 'text-cyan-600 bg-cyan-100' },
        { icon: IoCard, label: 'Mandates', sub: 'Auto-pay setup', path: '/mandates', color: 'text-orange-600 bg-orange-100' },
      ],
    },
    {
      title: 'Security',
      items: [
        { icon: IoLockClosed, label: 'Change MPIN', sub: 'Update UPI PIN', path: '/change-mpin', color: 'text-red-600 bg-red-100' },
        { icon: IoFingerPrint, label: 'Biometric Login', sub: 'Face ID / Fingerprint', path: null, color: 'text-green-600 bg-green-100' },
        { icon: IoShield, label: 'KYC Status', sub: 'Full KYC', path: null, color: 'text-indigo-600 bg-indigo-100' },
      ],
    },
    {
      title: 'Support',
      items: [
        { icon: IoChatbubbles, label: 'Complaints', sub: 'Raise & track', path: '/complaints', color: 'text-yellow-600 bg-yellow-100' },
        { icon: IoNotifications, label: 'Notifications', sub: '3 unread', path: '/notifications', color: 'text-pink-600 bg-pink-100' },
        { icon: IoHelpCircle, label: 'Help & FAQ', sub: 'Get support', path: '/help', color: 'text-teal-600 bg-teal-100' },
        { icon: IoSettings, label: 'Settings', sub: 'App preferences', path: '/settings', color: 'text-gray-600 bg-gray-100' },
      ],
    },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="page-container pb-24">
      <Header title="Profile" />

      <div className="px-4 mt-4 space-y-5">
        {/* Profile Card */}
        <div className="card bg-gradient-to-br from-nupi-primary to-blue-700 text-white py-6">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center">
              <IoPersonCircle className="w-12 h-12 text-white/80" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-bold">{userName}</h2>
              <p className="text-white/70 text-sm">+977 {phone}</p>
              <p className="text-white/60 text-xs mt-0.5">{upiId}</p>
            </div>
          </div>
          <div className="flex gap-4 mt-4 pt-4 border-t border-white/20">
            <div className="flex-1 text-center">
              <p className="text-xl font-bold">127</p>
              <p className="text-white/60 text-xs">Transactions</p>
            </div>
            <div className="flex-1 text-center border-x border-white/20">
              <p className="text-xl font-bold">3</p>
              <p className="text-white/60 text-xs">Banks</p>
            </div>
            <div className="flex-1 text-center">
              <p className="text-xl font-bold">2</p>
              <p className="text-white/60 text-xs">UPI IDs</p>
            </div>
          </div>
        </div>

        {/* Menu Sections */}
        {menuSections.map(section => (
          <div key={section.title}>
            <p className="text-xs font-semibold text-nupi-text-muted uppercase tracking-wider mb-2 px-1">{section.title}</p>
            <div className="card divide-y divide-gray-100 p-0 overflow-hidden">
              {section.items.map(item => (
                <button key={item.label} onClick={() => item.path && navigate(item.path)}
                  className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-gray-50 transition-colors text-left">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${item.color}`}>
                    <item.icon className="w-4.5 h-4.5" />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">{item.label}</p>
                    <p className="text-xs text-nupi-text-muted">{item.sub}</p>
                  </div>
                  <IoChevronForward className="w-4 h-4 text-gray-400" />
                </button>
              ))}
            </div>
          </div>
        ))}

        {/* Logout */}
        <button onClick={() => setShowLogout(true)}
          className="w-full card flex items-center justify-center gap-2 text-red-600 font-medium py-3">
          <IoLogOut className="w-5 h-5" /> Log Out
        </button>

        {/* App Version */}
        <p className="text-center text-xs text-nupi-text-muted pb-4">NUPI v1.0.0 • Powered by NRB</p>
      </div>

      {/* Logout Confirmation */}
      {showLogout && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center" onClick={() => setShowLogout(false)}>
          <div className="bg-white rounded-2xl p-6 mx-6 max-w-sm w-full animate-bounce-in" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-bold text-center">Log Out?</h3>
            <p className="text-sm text-nupi-text-secondary text-center mt-2">
              Are you sure you want to log out of NUPI?
            </p>
            <div className="flex gap-3 mt-6">
              <button onClick={() => setShowLogout(false)} className="flex-1 btn-secondary">Cancel</button>
              <button onClick={handleLogout} className="flex-1 bg-red-600 text-white py-2.5 rounded-xl font-medium">Log Out</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
