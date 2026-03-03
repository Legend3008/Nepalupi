import { useLocation, useNavigate } from 'react-router-dom';
import { IoCheckmarkCircle, IoShareSocial, IoDownload, IoHome } from 'react-icons/io5';
import { CURRENCY } from '../utils/constants';
import { formatDateTime, formatRRN } from '../utils/formatters';

export default function TransactionSuccess() {
  const navigate = useNavigate();
  const { state } = useLocation();

  if (!state) {
    navigate('/home', { replace: true });
    return null;
  }

  const amountRs = (state.amount / 100).toLocaleString();

  return (
    <div className="min-h-screen bg-white flex flex-col items-center justify-center px-6 py-8">
      {/* Success Icon */}
      <div className="w-24 h-24 rounded-full bg-green-100 flex items-center justify-center mb-6 animate-bounce-in">
        <IoCheckmarkCircle className="w-16 h-16 text-green-500" />
      </div>

      <h1 className="text-2xl font-bold text-nupi-text mb-1">Payment Successful!</h1>
      <p className="text-nupi-text-secondary text-sm mb-6">Your transaction was completed successfully</p>

      {/* Amount */}
      <div className="text-center mb-6">
        <p className="text-4xl font-bold text-nupi-text">
          {CURRENCY.symbol} {amountRs}
        </p>
        {state.receiverName && (
          <p className="text-nupi-text-secondary mt-2">
            Paid to <span className="font-medium">{state.receiverName}</span>
          </p>
        )}
        {state.receiverVpa && (
          <p className="text-sm text-nupi-text-muted">{state.receiverVpa}</p>
        )}
      </div>

      {/* Details Card */}
      <div className="card w-full max-w-sm">
        <div className="space-y-3">
          {[
            { label: 'Transaction ID', value: state.txnId },
            { label: 'RRN', value: state.rrn ? formatRRN(state.rrn) : null },
            { label: 'Date & Time', value: formatDateTime(state.timestamp) },
            { label: 'Type', value: state.type?.replace('_', ' ') },
            { label: 'From', value: state.from || null },
            { label: 'To', value: state.to || null },
          ].filter(item => item.value).map(item => (
            <div key={item.label} className="flex justify-between text-sm">
              <span className="text-nupi-text-secondary">{item.label}</span>
              <span className="font-medium text-nupi-text text-right">{item.value}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-4 mt-8 w-full max-w-sm">
        <button className="flex-1 btn-secondary flex items-center justify-center gap-2 py-3">
          <IoShareSocial className="w-5 h-5" />
          Share
        </button>
        <button className="flex-1 btn-secondary flex items-center justify-center gap-2 py-3">
          <IoDownload className="w-5 h-5" />
          Download
        </button>
      </div>

      <button
        onClick={() => navigate('/home', { replace: true })}
        className="btn-primary w-full max-w-sm mt-4 flex items-center justify-center gap-2"
      >
        <IoHome className="w-5 h-5" />
        Back to Home
      </button>
    </div>
  );
}
