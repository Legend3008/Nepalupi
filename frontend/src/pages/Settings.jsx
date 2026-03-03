import { useState } from 'react';
import Header from '../components/layout/Header';
import {
  IoLanguage, IoNotifications, IoFingerPrint, IoMoon, IoTrash,
  IoInformationCircle, IoShield, IoChevronForward, IoToggle
} from 'react-icons/io5';

export default function Settings() {
  const [settings, setSettings] = useState({
    language: 'English',
    notifications: true,
    biometric: false,
    darkMode: false,
    transactionAlerts: true,
    promotionalAlerts: false,
    soundEffects: true,
  });

  const toggleSetting = (key) => setSettings(p => ({ ...p, [key]: !p[key] }));

  const Toggle = ({ enabled, onToggle }) => (
    <button onClick={onToggle}
      className={`w-12 h-7 rounded-full relative transition-colors ${enabled ? 'bg-nupi-primary' : 'bg-gray-300'}`}>
      <div className={`w-5 h-5 bg-white rounded-full absolute top-1 transition-transform shadow ${enabled ? 'translate-x-6' : 'translate-x-1'}`} />
    </button>
  );

  const sections = [
    {
      title: 'General',
      items: [
        { icon: IoLanguage, label: 'Language', type: 'select', value: settings.language, color: 'text-blue-600 bg-blue-100' },
        { icon: IoMoon, label: 'Dark Mode', type: 'toggle', key: 'darkMode', value: settings.darkMode, color: 'text-purple-600 bg-purple-100' },
        { icon: IoToggle, label: 'Sound Effects', type: 'toggle', key: 'soundEffects', value: settings.soundEffects, color: 'text-orange-600 bg-orange-100' },
      ],
    },
    {
      title: 'Notifications',
      items: [
        { icon: IoNotifications, label: 'Push Notifications', type: 'toggle', key: 'notifications', value: settings.notifications, color: 'text-pink-600 bg-pink-100' },
        { icon: IoNotifications, label: 'Transaction Alerts', type: 'toggle', key: 'transactionAlerts', value: settings.transactionAlerts, color: 'text-green-600 bg-green-100' },
        { icon: IoNotifications, label: 'Promotional', type: 'toggle', key: 'promotionalAlerts', value: settings.promotionalAlerts, color: 'text-yellow-600 bg-yellow-100' },
      ],
    },
    {
      title: 'Security',
      items: [
        { icon: IoFingerPrint, label: 'Biometric Login', type: 'toggle', key: 'biometric', value: settings.biometric, color: 'text-green-600 bg-green-100' },
        { icon: IoShield, label: 'App Lock', type: 'toggle', key: 'appLock', value: false, color: 'text-indigo-600 bg-indigo-100' },
      ],
    },
    {
      title: 'Data',
      items: [
        { icon: IoTrash, label: 'Clear Cache', type: 'action', color: 'text-red-600 bg-red-100' },
        { icon: IoInformationCircle, label: 'About NUPI', type: 'nav', color: 'text-gray-600 bg-gray-100' },
      ],
    },
  ];

  return (
    <div className="page-container pb-24">
      <Header title="Settings" showBack />

      <div className="px-4 mt-4 space-y-5">
        {sections.map(section => (
          <div key={section.title}>
            <p className="text-xs font-semibold text-nupi-text-muted uppercase tracking-wider mb-2 px-1">{section.title}</p>
            <div className="card divide-y divide-gray-100 p-0 overflow-hidden">
              {section.items.map(item => (
                <div key={item.label} className="flex items-center gap-3 px-4 py-3.5">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${item.color}`}>
                    <item.icon className="w-4.5 h-4.5" />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">{item.label}</p>
                  </div>
                  {item.type === 'toggle' && <Toggle enabled={item.value} onToggle={() => toggleSetting(item.key)} />}
                  {item.type === 'select' && (
                    <span className="text-sm text-nupi-text-secondary flex items-center gap-1">
                      {item.value} <IoChevronForward className="w-3.5 h-3.5" />
                    </span>
                  )}
                  {(item.type === 'action' || item.type === 'nav') && (
                    <IoChevronForward className="w-4 h-4 text-gray-400" />
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}

        <p className="text-center text-xs text-nupi-text-muted pt-2">
          NUPI v1.0.0 • Build 2024.06.001
        </p>
      </div>
    </div>
  );
}
