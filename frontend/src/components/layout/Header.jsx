import { useNavigate } from 'react-router-dom';
import { IoArrowBack, IoNotificationsOutline, IoHelpCircleOutline } from 'react-icons/io5';
import { useApp } from '../../context/AppContext';

export default function Header({ title, showBack = false, showNotification = true, showHelp = false, rightAction, transparent = false }) {
  const navigate = useNavigate();
  const { unreadCount } = useApp();

  return (
    <header className={`sticky top-0 z-40 safe-top ${transparent ? 'bg-transparent' : 'bg-white shadow-sm'}`}>
      <div className="max-w-lg mx-auto flex items-center justify-between px-4 h-14">
        <div className="flex items-center gap-3">
          {showBack && (
            <button onClick={() => navigate(-1)} className="p-1.5 -ml-1.5 rounded-full hover:bg-gray-100 transition-colors">
              <IoArrowBack className="w-6 h-6 text-nupi-text" />
            </button>
          )}
          {title && (
            <h1 className={`text-lg font-semibold ${transparent ? 'text-white' : 'text-nupi-text'}`}>
              {title}
            </h1>
          )}
        </div>

        <div className="flex items-center gap-2">
          {rightAction}
          {showHelp && (
            <button onClick={() => navigate('/help')} className="p-2 rounded-full hover:bg-gray-100 transition-colors">
              <IoHelpCircleOutline className="w-6 h-6 text-nupi-text-secondary" />
            </button>
          )}
          {showNotification && (
            <button onClick={() => navigate('/notifications')} className="p-2 rounded-full hover:bg-gray-100 transition-colors relative">
              <IoNotificationsOutline className="w-6 h-6 text-nupi-text-secondary" />
              {unreadCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 bg-nupi-accent text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
