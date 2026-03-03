import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import Header from '../components/layout/Header';
import AmountInput from '../components/common/AmountInput';
import { useAuth } from '../context/AuthContext';
import { CURRENCY, APP_NAME } from '../utils/constants';
import { IoShareSocial, IoDownload, IoSwapHorizontal, IoCopy, IoCheckmark } from 'react-icons/io5';

export default function MyQRCode() {
  const { user } = useAuth();
  const [mode, setMode] = useState('static'); // static | dynamic
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [copied, setCopied] = useState(false);

  const upiId = user?.vpa || 'user@nchl';
  const userName = user?.name || 'NUPI User';

  const qrValue = mode === 'static'
    ? `upi://pay?pa=${upiId}&pn=${encodeURIComponent(userName)}&cu=NPR`
    : `upi://pay?pa=${upiId}&pn=${encodeURIComponent(userName)}&am=${amount || '0'}&cu=NPR${note ? `&tn=${encodeURIComponent(note)}` : ''}`;

  const handleCopy = () => {
    navigator.clipboard?.writeText(upiId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: `Pay ${userName} via ${APP_NAME}`,
          text: `Pay me using UPI ID: ${upiId}`,
          url: qrValue,
        });
      } catch (e) { /* cancelled */ }
    }
  };

  const handleDownload = () => {
    const svg = document.getElementById('nupi-qr-svg');
    if (!svg) return;
    const svgData = new XMLSerializer().serializeToString(svg);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const img = new Image();
    img.onload = () => {
      canvas.width = img.width;
      canvas.height = img.height;
      ctx.fillStyle = 'white';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0);
      const link = document.createElement('a');
      link.download = `nupi-qr-${upiId}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    };
    img.src = 'data:image/svg+xml;base64,' + btoa(svgData);
  };

  return (
    <div className="page-container">
      <Header title="My QR Code" showBack />

      <div className="px-4 mt-4 space-y-4">
        {/* Toggle */}
        <div className="bg-gray-100 rounded-xl p-1 flex">
          {[['static', 'Static QR'], ['dynamic', 'With Amount']].map(([key, label]) => (
            <button key={key} onClick={() => setMode(key)}
              className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${mode === key ? 'bg-white text-nupi-primary shadow-sm' : 'text-nupi-text-secondary'}`}>
              {label}
            </button>
          ))}
        </div>

        {/* QR Card */}
        <div className="card text-center py-8">
          <div className="inline-block p-4 bg-white rounded-2xl border-2 border-nupi-primary/20 shadow-lg">
            <QRCodeSVG
              id="nupi-qr-svg"
              value={qrValue}
              size={220}
              level="H"
              includeMargin={true}
              fgColor="#003893"
              imageSettings={{
                src: '/nupi-logo.svg',
                x: undefined,
                y: undefined,
                height: 40,
                width: 40,
                excavate: true,
              }}
            />
          </div>

          {/* User Info */}
          <div className="mt-4">
            <p className="font-bold text-lg">{userName}</p>
            <div className="flex items-center justify-center gap-2 mt-1">
              <p className="text-sm text-nupi-text-secondary">{upiId}</p>
              <button onClick={handleCopy} className="p-1">
                {copied ? <IoCheckmark className="w-4 h-4 text-green-500" /> : <IoCopy className="w-4 h-4 text-gray-400" />}
              </button>
            </div>
          </div>

          {mode === 'dynamic' && amount && (
            <div className="mt-3 inline-block bg-nupi-primary/10 px-4 py-1.5 rounded-full">
              <p className="text-nupi-primary font-bold">{CURRENCY.symbol} {Number(amount).toLocaleString()}</p>
            </div>
          )}
        </div>

        {/* Amount Input for Dynamic */}
        {mode === 'dynamic' && (
          <div className="space-y-3">
            <AmountInput value={amount} onChange={setAmount} label="Amount (optional)" />
            <div>
              <label className="text-sm font-medium text-nupi-text-secondary mb-1 block">Note (optional)</label>
              <input type="text" value={note} onChange={e => setNote(e.target.value)}
                placeholder="e.g., Rent, Dinner split" className="input-field" maxLength={50} />
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button onClick={handleShare} className="flex-1 btn-secondary flex items-center justify-center gap-2">
            <IoShareSocial className="w-4 h-4" /> Share
          </button>
          <button onClick={handleDownload} className="flex-1 btn-primary flex items-center justify-center gap-2">
            <IoDownload className="w-4 h-4" /> Save
          </button>
        </div>

        {/* Scan to Pay Info */}
        <div className="card bg-blue-50 border-blue-200">
          <p className="text-sm text-blue-800">
            <span className="font-semibold">Tip:</span> Share your QR code to receive instant payments. Works with any UPI app in Nepal.
          </p>
        </div>
      </div>
    </div>
  );
}
