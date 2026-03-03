import { useParams, useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import StatusBadge from '../components/common/StatusBadge';
import { CURRENCY } from '../utils/constants';
import { formatDateTime, formatRRN } from '../utils/formatters';
import { IoShareSocial, IoDownload, IoChatbubbleOutline, IoAlertCircleOutline, IoArrowUp, IoArrowDown } from 'react-icons/io5';

export default function TransactionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // Demo data
  const txn = {
    id, type: 'SEND', status: 'SUCCESS', amountPaisa: 250000,
    senderVpa: 'me@nchl', senderName: 'You', senderBank: 'NCHL',
    receiverVpa: 'ram@nchl', receiverName: 'Ram Sharma', receiverBank: 'NCHL',
    rrn: '123456789012', txnId: 'TXN' + id,
    note: 'Lunch money', createdAt: new Date(Date.now() - 3600000).toISOString(),
  };

  const isSend = ['SEND', 'QR_PAY', 'BILL_PAY'].includes(txn.type);

  return (
    <div className="page-container">
      <Header title="Transaction Details" showBack />

      <div className="px-4 mt-4 space-y-4">
        {/* Main Card */}
        <div className="card text-center py-6">
          <div className={`w-14 h-14 rounded-full mx-auto mb-3 flex items-center justify-center ${isSend ? 'bg-red-50' : 'bg-green-50'}`}>
            {isSend ? <IoArrowUp className="w-7 h-7 text-red-500" /> : <IoArrowDown className="w-7 h-7 text-green-500" />}
          </div>
          <p className={`text-3xl font-bold ${isSend ? 'text-nupi-text' : 'text-green-600'}`}>
            {isSend ? '-' : '+'}{CURRENCY.symbol} {(txn.amountPaisa / 100).toLocaleString()}
          </p>
          <p className="text-sm text-nupi-text-secondary mt-1">
            {isSend ? 'Paid to' : 'Received from'} {isSend ? txn.receiverName : txn.senderName}
          </p>
          <div className="mt-3">
            <StatusBadge status={txn.status} size="md" />
          </div>
        </div>

        {/* Details */}
        <div className="card divide-y divide-nupi-border/50">
          {[
            { label: 'Transaction ID', value: txn.txnId },
            { label: 'UPI RRN', value: formatRRN(txn.rrn) },
            { label: 'Date & Time', value: formatDateTime(txn.createdAt) },
            { label: 'Sender', value: `${txn.senderName} (${txn.senderVpa})` },
            { label: 'Receiver', value: `${txn.receiverName} (${txn.receiverVpa})` },
            { label: 'Sender Bank', value: txn.senderBank },
            { label: 'Receiver Bank', value: txn.receiverBank },
            { label: 'Remark', value: txn.note || '-' },
          ].map(item => (
            <div key={item.label} className="flex justify-between py-3 text-sm">
              <span className="text-nupi-text-secondary">{item.label}</span>
              <span className="font-medium text-nupi-text text-right max-w-[55%] break-all">{item.value}</span>
            </div>
          ))}
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          <button className="flex-1 btn-secondary flex items-center justify-center gap-2 py-3 text-sm">
            <IoShareSocial className="w-4 h-4" /> Share
          </button>
          <button className="flex-1 btn-secondary flex items-center justify-center gap-2 py-3 text-sm">
            <IoDownload className="w-4 h-4" /> Receipt
          </button>
        </div>

        {/* Raise Issue */}
        <button
          onClick={() => navigate('/raise-complaint', { state: { txnId: txn.txnId } })}
          className="w-full card-hover flex items-center gap-3 border border-nupi-border"
        >
          <IoAlertCircleOutline className="w-6 h-6 text-nupi-accent" />
          <div className="flex-1 text-left">
            <p className="font-medium text-nupi-text text-sm">Having an issue?</p>
            <p className="text-xs text-nupi-text-muted">Raise a complaint for this transaction</p>
          </div>
        </button>
      </div>
    </div>
  );
}
