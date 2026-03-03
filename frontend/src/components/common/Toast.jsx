import { IoCheckmarkCircle, IoWarning, IoAlertCircle, IoInformationCircle } from 'react-icons/io5';

const icons = {
  success: IoCheckmarkCircle,
  warning: IoWarning,
  error: IoAlertCircle,
  info: IoInformationCircle,
};
const colors = {
  success: 'bg-green-50 border-green-200 text-green-800',
  warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
  error: 'bg-red-50 border-red-200 text-red-800',
  info: 'bg-blue-50 border-blue-200 text-blue-800',
};

export default function Toast({ message, type = 'info' }) {
  const Icon = icons[type];
  return (
    <div className={`flex items-center gap-3 px-4 py-3 rounded-xl border shadow-lg animate-slide-down ${colors[type]}`}>
      <Icon className="w-5 h-5 flex-shrink-0" />
      <p className="text-sm font-medium">{message}</p>
    </div>
  );
}
