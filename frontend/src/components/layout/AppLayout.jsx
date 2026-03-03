import { Outlet } from 'react-router-dom';
import BottomNav from './BottomNav';
import Toast from '../common/Toast';
import { useApp } from '../../context/AppContext';

export default function AppLayout() {
  const { toasts } = useApp();

  return (
    <div className="max-w-lg mx-auto min-h-screen bg-nupi-bg relative">
      <main className="page-enter">
        <Outlet />
      </main>
      <BottomNav />

      {/* Toast notifications */}
      <div className="fixed top-4 left-4 right-4 z-[100] flex flex-col gap-2 max-w-lg mx-auto">
        {toasts.map(toast => (
          <Toast key={toast.id} message={toast.message} type={toast.type} />
        ))}
      </div>
    </div>
  );
}
