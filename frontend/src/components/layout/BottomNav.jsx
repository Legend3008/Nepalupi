import { useNavigate, useLocation } from 'react-router-dom';
import { IoHomeOutline, IoHome, IoTimeOutline, IoTime, IoQrCodeOutline, IoQrCode, IoPersonOutline, IoPerson } from 'react-icons/io5';
import { HiOutlineArrowsRightLeft, HiArrowsRightLeft } from 'react-icons/hi2';

const tabs = [
  { path: '/home', label: 'Home', icon: IoHomeOutline, activeIcon: IoHome },
  { path: '/history', label: 'History', icon: IoTimeOutline, activeIcon: IoTime },
  { path: '/scan', label: 'Scan', icon: IoQrCodeOutline, activeIcon: IoQrCode, special: true },
  { path: '/send', label: 'Pay', icon: HiOutlineArrowsRightLeft, activeIcon: HiArrowsRightLeft },
  { path: '/profile', label: 'Profile', icon: IoPersonOutline, activeIcon: IoPerson },
];

export default function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white shadow-bottom-nav safe-bottom z-50">
      <div className="max-w-lg mx-auto flex items-center justify-around py-1">
        {tabs.map(tab => {
          const isActive = location.pathname === tab.path ||
            (tab.path === '/home' && location.pathname === '/home');
          const Icon = isActive ? tab.activeIcon : tab.icon;

          if (tab.special) {
            return (
              <button
                key={tab.path}
                onClick={() => navigate(tab.path)}
                className="relative -top-4 flex flex-col items-center"
              >
                <div className={`w-14 h-14 rounded-full flex items-center justify-center shadow-btn
                  ${isActive ? 'gradient-primary' : 'bg-nupi-primary'}`}>
                  <Icon className="w-7 h-7 text-white" />
                </div>
                <span className={`text-[10px] mt-0.5 font-medium
                  ${isActive ? 'text-nupi-primary' : 'text-nupi-text-muted'}`}>
                  {tab.label}
                </span>
              </button>
            );
          }

          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              className="flex flex-col items-center py-2 px-3 ripple rounded-lg min-w-[60px]"
            >
              <Icon className={`w-6 h-6 transition-colors ${isActive ? 'text-nupi-primary' : 'text-nupi-text-muted'}`} />
              <span className={`text-[10px] mt-0.5 font-medium transition-colors
                ${isActive ? 'text-nupi-primary' : 'text-nupi-text-muted'}`}>
                {tab.label}
              </span>
              {isActive && <div className="w-1 h-1 rounded-full bg-nupi-primary mt-0.5" />}
            </button>
          );
        })}
      </div>
    </nav>
  );
}
