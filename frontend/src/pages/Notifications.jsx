import { useState } from 'react';
import Header from '../components/layout/Header';
import { useApp } from '../context/AppContext';
import { timeAgo } from '../utils/formatters';
import {
  IoCheckmarkDone, IoTrash, IoNotifications, IoCard, IoShield,
  IoMegaphone, IoPricetag, IoAlertCircle, IoSwapHorizontal
} from 'react-icons/io5';

const demoNotifications = [
  { id: 'n1', type: 'TRANSACTION', title: 'Payment Successful', message: 'Rs 2,500 sent to Ram Kumar via UPI', read: false, createdAt: new Date(Date.now() - 300000) },
  { id: 'n2', type: 'COLLECT', title: 'Collect Request', message: 'Sita Devi has requested Rs 1,500 from you', read: false, createdAt: new Date(Date.now() - 3600000) },
  { id: 'n3', type: 'SECURITY', title: 'Login Alert', message: 'New login detected from iPhone 15 Pro', read: false, createdAt: new Date(Date.now() - 7200000) },
  { id: 'n4', type: 'PROMO', title: 'Cashback Offer!', message: 'Get 10% cashback on your next bill payment. Valid till June 30.', read: true, createdAt: new Date(Date.now() - 86400000) },
  { id: 'n5', type: 'TRANSACTION', title: 'Payment Received', message: 'Rs 5,000 received from Hari Bahadur', read: true, createdAt: new Date(Date.now() - 86400000 * 2) },
  { id: 'n6', type: 'MANDATE', title: 'Auto-Pay Executed', message: 'Rs 999 debited for Nepal Telecom mandate', read: true, createdAt: new Date(Date.now() - 86400000 * 3) },
  { id: 'n7', type: 'SECURITY', title: 'MPIN Changed', message: 'Your UPI MPIN was changed successfully', read: true, createdAt: new Date(Date.now() - 86400000 * 5) },
];

const typeIcons = {
  TRANSACTION: { icon: IoSwapHorizontal, color: 'text-blue-600 bg-blue-100' },
  COLLECT: { icon: IoCard, color: 'text-orange-600 bg-orange-100' },
  SECURITY: { icon: IoShield, color: 'text-red-600 bg-red-100' },
  PROMO: { icon: IoPricetag, color: 'text-green-600 bg-green-100' },
  MANDATE: { icon: IoAlertCircle, color: 'text-purple-600 bg-purple-100' },
};

export default function Notifications() {
  const [notifications, setNotifications] = useState(demoNotifications);
  const [filter, setFilter] = useState('ALL');

  const unreadCount = notifications.filter(n => !n.read).length;
  const filters = ['ALL', 'UNREAD', 'TRANSACTION', 'SECURITY'];

  const filtered = notifications.filter(n => {
    if (filter === 'ALL') return true;
    if (filter === 'UNREAD') return !n.read;
    return n.type === filter;
  });

  const markAllRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const markRead = (id) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  return (
    <div className="page-container pb-24">
      <Header title="Notifications" showBack rightAction={
        unreadCount > 0 ? (
          <button onClick={markAllRead} className="p-2 rounded-full hover:bg-gray-100" title="Mark all read">
            <IoCheckmarkDone className="w-5 h-5 text-nupi-primary" />
          </button>
        ) : null
      } />

      <div className="px-4 mt-4 space-y-4">
        {/* Unread Badge */}
        {unreadCount > 0 && (
          <div className="bg-nupi-primary/10 rounded-xl p-3 flex items-center justify-between">
            <span className="text-sm text-nupi-primary font-medium">{unreadCount} unread notification{unreadCount > 1 ? 's' : ''}</span>
            <button onClick={markAllRead} className="text-xs text-nupi-primary font-semibold">Mark all read</button>
          </div>
        )}

        {/* Filters */}
        <div className="flex gap-2 overflow-x-auto scrollbar-none">
          {filters.map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-all ${filter === f ? 'bg-nupi-primary text-white' : 'bg-gray-100 text-nupi-text-secondary'}`}>
              {f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {/* Notification List */}
        <div className="space-y-2">
          {filtered.map(notif => {
            const typeConfig = typeIcons[notif.type] || typeIcons.TRANSACTION;
            const Icon = typeConfig.icon;
            return (
              <div key={notif.id} onClick={() => markRead(notif.id)}
                className={`card cursor-pointer transition-all ${!notif.read ? 'bg-blue-50/50 border-blue-200' : ''}`}>
                <div className="flex gap-3">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${typeConfig.color}`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-start">
                      <p className={`text-sm ${!notif.read ? 'font-bold' : 'font-medium'}`}>{notif.title}</p>
                      {!notif.read && <div className="w-2 h-2 rounded-full bg-nupi-primary flex-shrink-0 mt-1.5" />}
                    </div>
                    <p className="text-xs text-nupi-text-secondary mt-0.5 line-clamp-2">{notif.message}</p>
                    <p className="text-xs text-nupi-text-muted mt-1">{timeAgo(notif.createdAt)}</p>
                  </div>
                </div>
              </div>
            );
          })}
          {filtered.length === 0 && (
            <div className="text-center py-12">
              <IoNotifications className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-nupi-text-muted">{filter === 'ALL' ? 'No notifications' : 'No notifications in this category'}</p>
            </div>
          )}
        </div>

        {/* Clear All */}
        {notifications.length > 0 && (
          <button onClick={clearAll} className="w-full flex items-center justify-center gap-2 text-sm text-nupi-danger font-medium py-3">
            <IoTrash className="w-4 h-4" /> Clear All Notifications
          </button>
        )}
      </div>
    </div>
  );
}
