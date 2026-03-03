import { TRANSACTION_STATUS } from '../../utils/constants';
import { IoCheckmarkCircle, IoTime, IoCloseCircle, IoRefresh, IoHourglass } from 'react-icons/io5';

const statusConfig = {
  [TRANSACTION_STATUS.SUCCESS]: { icon: IoCheckmarkCircle, label: 'Success', classes: 'status-success' },
  [TRANSACTION_STATUS.PENDING]: { icon: IoTime, label: 'Pending', classes: 'status-pending' },
  [TRANSACTION_STATUS.PROCESSING]: { icon: IoHourglass, label: 'Processing', classes: 'status-pending' },
  [TRANSACTION_STATUS.FAILED]: { icon: IoCloseCircle, label: 'Failed', classes: 'status-failed' },
  [TRANSACTION_STATUS.REVERSED]: { icon: IoRefresh, label: 'Reversed', classes: 'status-failed' },
  [TRANSACTION_STATUS.EXPIRED]: { icon: IoCloseCircle, label: 'Expired', classes: 'status-failed' },
};

export default function StatusBadge({ status, size = 'sm' }) {
  const config = statusConfig[status] || statusConfig[TRANSACTION_STATUS.PENDING];
  const Icon = config.icon;
  const sizeClasses = size === 'sm' ? 'text-xs px-2.5 py-1' : 'text-sm px-3 py-1.5';

  return (
    <span className={`inline-flex items-center gap-1 rounded-full font-medium ${config.classes} ${sizeClasses}`}>
      <Icon className={size === 'sm' ? 'w-3 h-3' : 'w-4 h-4'} />
      {config.label}
    </span>
  );
}
