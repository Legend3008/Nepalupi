import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/layout/Header';
import { IoFlashlightOutline, IoImageOutline, IoQrCode } from 'react-icons/io5';

export default function ScanPay() {
  const navigate = useNavigate();
  const [scanning, setScanning] = useState(true);
  const [manualMode, setManualMode] = useState(false);
  const [flashOn, setFlashOn] = useState(false);
  const videoRef = useRef(null);

  useEffect(() => {
    // Camera access attempt - gracefully degrade
    let stream = null;
    if (scanning && !manualMode) {
      navigator.mediaDevices?.getUserMedia({ video: { facingMode: 'environment' } })
        .then(s => {
          stream = s;
          if (videoRef.current) videoRef.current.srcObject = s;
        })
        .catch(() => setManualMode(true));
    }
    return () => { stream?.getTracks().forEach(t => t.stop()); };
  }, [scanning, manualMode]);

  const handleScanResult = (data) => {
    // Parse UPI QR: upi://pay?pa=xxx@bank&pn=Name&am=100
    try {
      const url = new URL(data);
      const pa = url.searchParams.get('pa');
      const am = url.searchParams.get('am');
      if (pa) {
        navigate(`/send/${encodeURIComponent(pa)}`, { state: { amount: am } });
      }
    } catch {
      if (data.includes('@')) {
        navigate(`/send/${encodeURIComponent(data)}`);
      }
    }
  };

  // Simulate scan for demo
  const handleDemoScan = () => {
    handleScanResult('upi://pay?pa=merchant@nchl&pn=Demo%20Shop&am=500');
  };

  return (
    <div className="fixed inset-0 bg-black flex flex-col">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 z-10 safe-top">
        <div className="flex items-center justify-between px-4 py-4">
          <button onClick={() => navigate(-1)} className="text-white text-sm font-medium bg-black/30 px-3 py-1.5 rounded-full">
            ← Back
          </button>
          <h2 className="text-white font-semibold">Scan QR Code</h2>
          <div className="flex gap-2">
            <button
              onClick={() => setFlashOn(!flashOn)}
              className="p-2 rounded-full bg-black/30"
            >
              <IoFlashlightOutline className={`w-5 h-5 ${flashOn ? 'text-yellow-400' : 'text-white'}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Camera / Scanner Area */}
      <div className="flex-1 relative flex items-center justify-center">
        {!manualMode ? (
          <>
            <video ref={videoRef} autoPlay playsInline className="absolute inset-0 w-full h-full object-cover" />
            {/* Scanner overlay */}
            <div className="absolute inset-0 bg-black/50" />
            <div className="relative w-64 h-64">
              {/* Transparent window */}
              <div className="absolute inset-0 border-2 border-white/30 rounded-2xl" />
              {/* Corner markers */}
              {['top-0 left-0 border-t-4 border-l-4 rounded-tl-2xl',
                'top-0 right-0 border-t-4 border-r-4 rounded-tr-2xl',
                'bottom-0 left-0 border-b-4 border-l-4 rounded-bl-2xl',
                'bottom-0 right-0 border-b-4 border-r-4 rounded-br-2xl',
              ].map((cls, i) => (
                <div key={i} className={`absolute w-8 h-8 border-white ${cls}`} />
              ))}
              {/* Scan line animation */}
              <div className="absolute left-4 right-4 h-0.5 bg-nupi-accent animate-[scan_2s_ease-in-out_infinite]" />
            </div>
          </>
        ) : (
          <div className="text-center px-6">
            <IoQrCode className="w-20 h-20 text-white/30 mx-auto mb-4" />
            <p className="text-white/60 mb-6">Camera not available</p>
          </div>
        )}
      </div>

      {/* Bottom controls */}
      <div className="absolute bottom-0 left-0 right-0 safe-bottom bg-gradient-to-t from-black/80 to-transparent pt-12 pb-8 px-6">
        <p className="text-white/60 text-center text-sm mb-4">
          Point camera at any UPI QR code to pay
        </p>
        <div className="flex gap-3">
          <button
            onClick={() => setManualMode(!manualMode)}
            className="flex-1 bg-white/10 text-white font-medium py-3 rounded-xl flex items-center justify-center gap-2"
          >
            <IoImageOutline className="w-5 h-5" />
            Gallery
          </button>
          <button
            onClick={handleDemoScan}
            className="flex-1 bg-white text-nupi-primary font-semibold py-3 rounded-xl"
          >
            Demo Scan
          </button>
        </div>
      </div>

      <style>{`
        @keyframes scan {
          0%, 100% { top: 10%; }
          50% { top: 85%; }
        }
      `}</style>
    </div>
  );
}
