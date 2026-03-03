import { useNavigate } from 'react-router-dom';
import { formatCurrency, timeAgo, getInitials } from '../../utils/formatters';
import StatusBadge from '../common/StatusBadge';
import { IoArrowUp, IoArrowDown, IoSwapHorizontal } from 'react-icons/io5';

const typeConfig = {
  SEND: { icon: IoArrowUp, color: 'text-red-500', bg: 'bg-red-50', sign: '-' },
  QR_PAY: { icon: IoArrowUp, color: 'text-red-500', bg: 'bg-red-50', sign: '-' },
  BILL_PAY: { icon: IoArrowUp, color: 'text-orange-500', bg: 'bg-orange-50', sign: '-' },
  RECEIVE: { icon: IoArrowDown, color: 'text-green-500', bg: 'bg-green-50', sign: '+' },
  COLLECT: { icon: IoArrowDown, color: 'text-green-500', bg: 'bg-green-50', sign: '+' },
  SELF_TRANSFER: { icon: IoSwapHorizontal, color: 'text-blue-500', bg: 'bg-blue-50', sign: '' },
  MANDATE: { icon: IoArrowUp, color: 'text-purple-500', bg: 'bg-purple-50', sign: '-' },
  UPI_LITE: { icon: IoArrowUp, color: 'text-cyan-500', bg: 'bg-cyan-50', sign: '-' },
};

export default function TransactionCard({ transaction }) {
  const navigate = useNavigate();
  const config = typeConfig[transaction.type] || typeConfig.SEND;
  const Icon = config.icon;

  const displayName = transaction.type === 'RECEIVE' || transaction.type === 'COLLECT'
    ? transaction.senderName || transaction.senderVpa
    : transaction.receiverName || transaction.receiverVpa;

  return (
    <button
      onClick={() => navigate(`/transaction/${transaction.id}`)}
      className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 active:bg-gray-100 transition-colors rounded-xl"
    >
      {/* Avatar */}
      <div className={`w-11 h-11 rounded-full flex items-center justify-center ${config.bg} flex-shrink-0`}>
        <Icon className={`w-5 h-5 ${config.color}`} />
      </div>

      {/* Details */}
      <div className="flex-1 min-w-0 text-left">
        <div className="flex items-center justify-between">
          <p className="font-medium text-nupi-text truncate mr-2">
            {displayName || 'Unknown'}
          </p>
          <p className={`font-semibold text-sm whitespace-nowrap ${
            config.sign === '+' ? 'text-green-600' : config.sign === '-' ? 'text-nupi-text' : 'text-blue-600'
          }`}>
            {config.sign}{formatCurrency(transaction.amountPaisa)}
          </p>
        </div>
        <div className="flex items-center justify-between mt-0.5">
          <p className="text-xs text-nupi-text-muted truncate mr-2">
            {transaction.note || transaction.type.replace('_', ' ')}
          </p>
          <div className="flex items-center gap-2">
            <span className="text-xs text-nupi-text-muted">{timeAgo(transaction.createdAt)}</span>
            <StatusBadge status={transaction.status} size="sm" />
          </div>
        </div>
      </div>
    </button>
  );
}

export function TransactionList({ transactions, title, emptyMessage = 'No transactions yet' }) {
  if (!transactions || transactions.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-3">
          <IoSwapHorizontal className="w-8 h-8 text-nupi-text-muted" />
        </div>
        <p className="text-nupi-text-secondary">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div>
      {title && <h3 className="section-header">{title}</h3>}
      <div className="card p-0 divide-y divide-nupi-border/50">
        {transactions.map(txn => (
          <TransactionCard key={txn.id} transaction={txn} />
        ))}
      </div>
    </div>
  );
}
