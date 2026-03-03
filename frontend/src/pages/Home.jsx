import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useApp } from '../context/AppContext';
import Header from '../components/layout/Header';
import { TransactionList } from '../components/transaction/TransactionCard';
import { formatCurrency } from '../utils/formatters';
import { CURRENCY } from '../utils/constants';
import {
  IoSend, IoArrowDown, IoQrCode, IoSwapHorizontal,
  IoReceiptOutline, IoCardOutline, IoFlash, IoCalendarOutline,
  IoWalletOutline, IoArrowForward, IoChevronForward,
  IoShieldCheckmark, IoEyeOutline, IoEyeOffOutline
} from 'react-icons/io5';
import { HiOutlineBanknotes } from 'react-icons/hi2';

// Demo data
const DEMO_TRANSACTIONS = [
  { id: '1', type: 'SEND', status: 'SUCCESS', amountPaisa: 250000, senderVpa: 'me@nupi', receiverVpa: 'ram@nchl', receiverName: 'Ram Sharma', note: 'Lunch', createdAt: new Date(Date.now() - 3600000) },
  { id: '2', type: 'RECEIVE', status: 'SUCCESS', amountPaisa: 500000, senderVpa: 'sita@nabil', senderName: 'Sita Thapa', receiverVpa: 'me@nupi', note: 'Rent share', createdAt: new Date(Date.now() - 7200000) },
  { id: '3', type: 'BILL_PAY', status: 'SUCCESS', amountPaisa: 120000, senderVpa: 'me@nupi', receiverVpa: 'nea@nchl', receiverName: 'NEA Electricity', note: 'Electricity Bill', createdAt: new Date(Date.now() - 86400000) },
  { id: '4', type: 'QR_PAY', status: 'PENDING', amountPaisa: 35000, senderVpa: 'me@nupi', receiverVpa: 'cafe@nchl', receiverName: 'Himalayan Java', note: 'Coffee', createdAt: new Date(Date.now() - 172800000) },
];

const quickActions = [
  { icon: IoSend, label: 'Send Money', path: '/send', color: 'bg-blue-500' },
  { icon: IoArrowDown, label: 'Request', path: '/request', color: 'bg-green-500' },
  { icon: IoQrCode, label: 'Scan & Pay', path: '/scan', color: 'bg-purple-500' },
  { icon: IoSwapHorizontal, label: 'Self Transfer', path: '/self-transfer', color: 'bg-orange-500' },
];

const services = [
  { icon: IoReceiptOutline, label: 'Bill Payment', path: '/bills', color: 'text-amber-600' },
  { icon: IoCardOutline, label: 'Bank Account', path: '/banks', color: 'text-blue-600' },
  { icon: IoFlash, label: 'UPI Lite', path: '/upi-lite', color: 'text-cyan-600' },
  { icon: IoCalendarOutline, label: 'Mandates', path: '/mandates', color: 'text-purple-600' },
  { icon: IoWalletOutline, label: 'Balance', path: '/balance', color: 'text-green-600' },
  { icon: HiOutlineBanknotes, label: 'My QR', path: '/my-qr', color: 'text-rose-600' },
];

export default function Home() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showToast } = useApp();
  const [showBalance, setShowBalance] = useState(false);
  const [balance] = useState(15847500); // Demo balance in paisa
  const [pendingCount] = useState(2);

  return (
    <div className="page-container">
      {/* Top Header - Gradient */}
      <div className="gradient-primary px-4 pb-6 pt-4 rounded-b-3xl">
        {/* Top bar */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center">
              <span className="text-white font-bold text-lg">
                {user?.fullName?.[0] || 'U'}
              </span>
            </div>
            <div>
              <p className="text-white/70 text-xs">Welcome back</p>
              <p className="text-white font-semibold">{user?.fullName || 'User'}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => navigate('/notifications')} className="p-2 rounded-full bg-white/10">
              <IoShieldCheckmark className="w-5 h-5 text-white" />
            </button>
          </div>
        </div>

        {/* Balance Card */}
        <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-white/60 text-xs font-medium mb-1">Available Balance</p>
              <div className="flex items-center gap-2">
                {showBalance ? (
                  <p className="text-white text-2xl font-bold">{formatCurrency(balance)}</p>
                ) : (
                  <p className="text-white text-2xl font-bold">{CURRENCY.symbol} ••••••</p>
                )}
                <button onClick={() => setShowBalance(!showBalance)} className="p-1">
                  {showBalance ? (
                    <IoEyeOffOutline className="w-5 h-5 text-white/60" />
                  ) : (
                    <IoEyeOutline className="w-5 h-5 text-white/60" />
                  )}
                </button>
              </div>
            </div>
            <button
              onClick={() => navigate('/balance')}
              className="bg-white text-nupi-primary text-sm font-semibold px-4 py-2 rounded-xl"
            >
              Check
            </button>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="px-4 -mt-5">
        <div className="bg-white rounded-2xl shadow-card p-4">
          <div className="grid grid-cols-4 gap-3">
            {quickActions.map(action => (
              <button
                key={action.path}
                onClick={() => navigate(action.path)}
                className="flex flex-col items-center gap-2 py-2 ripple rounded-xl"
              >
                <div className={`w-12 h-12 rounded-xl ${action.color} flex items-center justify-center shadow-sm`}>
                  <action.icon className="w-6 h-6 text-white" />
                </div>
                <span className="text-[11px] font-medium text-nupi-text-secondary text-center leading-tight">
                  {action.label}
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Pending Requests */}
      {pendingCount > 0 && (
        <div className="px-4 mt-4">
          <button
            onClick={() => navigate('/history?filter=pending')}
            className="w-full flex items-center justify-between bg-amber-50 border border-amber-200 rounded-2xl p-4"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
                <IoArrowDown className="w-5 h-5 text-amber-600" />
              </div>
              <div className="text-left">
                <p className="font-medium text-amber-900">{pendingCount} Pending Request{pendingCount > 1 ? 's' : ''}</p>
                <p className="text-xs text-amber-700">Tap to view and approve</p>
              </div>
            </div>
            <IoChevronForward className="w-5 h-5 text-amber-600" />
          </button>
        </div>
      )}

      {/* Services */}
      <div className="px-4 mt-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="section-header mb-0">Services</h3>
        </div>
        <div className="grid grid-cols-3 gap-3">
          {services.map(service => (
            <button
              key={service.path}
              onClick={() => navigate(service.path)}
              className="card-hover flex flex-col items-center gap-2 py-4"
            >
              <service.icon className={`w-7 h-7 ${service.color}`} />
              <span className="text-xs font-medium text-nupi-text-secondary text-center">{service.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Promotions Banner */}
      <div className="px-4 mt-6">
        <div className="gradient-accent rounded-2xl p-4 flex items-center gap-4">
          <div className="flex-1">
            <p className="text-white font-semibold">Invite & Earn</p>
            <p className="text-white/80 text-xs mt-1">Refer friends to NUPI and earn रू 100 reward each</p>
          </div>
          <button className="bg-white/20 text-white text-sm font-medium px-4 py-2 rounded-xl whitespace-nowrap">
            Invite Now
          </button>
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="px-4 mt-6 mb-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="section-header mb-0">Recent Transactions</h3>
          <button
            onClick={() => navigate('/history')}
            className="text-sm text-nupi-primary font-medium flex items-center gap-1"
          >
            View All <IoArrowForward className="w-4 h-4" />
          </button>
        </div>
        <TransactionList transactions={DEMO_TRANSACTIONS} />
      </div>

      {/* Security Badge */}
      <div className="px-4 mb-6">
        <div className="flex items-center justify-center gap-2 py-3">
          <IoShieldCheckmark className="w-4 h-4 text-green-600" />
          <span className="text-xs text-nupi-text-muted">
            Secured by 256-bit encryption | Regulated by NRB
          </span>
        </div>
      </div>
    </div>
  );
}
