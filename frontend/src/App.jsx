import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import AppLayout from './components/layout/AppLayout';

// Auth pages
import SplashScreen from './pages/SplashScreen';
import Login from './pages/Login';
import OtpVerify from './pages/OtpVerify';
import MpinSetup from './pages/MpinSetup';
import MpinLogin from './pages/MpinLogin';

// Main pages
import Home from './pages/Home';
import SendMoney from './pages/SendMoney';
import SendToContact from './pages/SendToContact';
import RequestMoney from './pages/RequestMoney';
import ScanPay from './pages/ScanPay';
import SelfTransfer from './pages/SelfTransfer';
import TransactionHistory from './pages/TransactionHistory';
import TransactionDetail from './pages/TransactionDetail';
import BankAccounts from './pages/BankAccounts';
import LinkBank from './pages/LinkBank';
import BalanceCheck from './pages/BalanceCheck';
import BillPayments from './pages/BillPayments';
import PayBill from './pages/PayBill';
import UpiLite from './pages/UpiLite';
import Mandates from './pages/Mandates';
import CreateMandate from './pages/CreateMandate';
import MyQRCode from './pages/MyQRCode';
import Profile from './pages/Profile';
import Settings from './pages/Settings';
import Complaints from './pages/Complaints';
import RaiseComplaint from './pages/RaiseComplaint';
import Notifications from './pages/Notifications';
import Help from './pages/Help';
import ChangeMpin from './pages/ChangeMpin';
import TransactionSuccess from './pages/TransactionSuccess';

function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return null;
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function AuthRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return null;
  return !isAuthenticated ? children : <Navigate to="/home" replace />;
}

export default function App() {
  return (
    <Routes>
      {/* Auth Routes */}
      <Route path="/" element={<SplashScreen />} />
      <Route path="/login" element={<AuthRoute><Login /></AuthRoute>} />
      <Route path="/otp-verify" element={<AuthRoute><OtpVerify /></AuthRoute>} />
      <Route path="/mpin-setup" element={<MpinSetup />} />
      <Route path="/mpin-login" element={<MpinLogin />} />

      {/* Protected Routes */}
      <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route path="/home" element={<Home />} />
        <Route path="/send" element={<SendMoney />} />
        <Route path="/send/:upiId" element={<SendToContact />} />
        <Route path="/request" element={<RequestMoney />} />
        <Route path="/scan" element={<ScanPay />} />
        <Route path="/self-transfer" element={<SelfTransfer />} />
        <Route path="/history" element={<TransactionHistory />} />
        <Route path="/transaction/:id" element={<TransactionDetail />} />
        <Route path="/banks" element={<BankAccounts />} />
        <Route path="/bank-accounts" element={<BankAccounts />} />
        <Route path="/link-bank" element={<LinkBank />} />
        <Route path="/balance" element={<BalanceCheck />} />
        <Route path="/bills" element={<BillPayments />} />
        <Route path="/pay-bill/:billerId" element={<PayBill />} />
        <Route path="/upi-lite" element={<UpiLite />} />
        <Route path="/mandates" element={<Mandates />} />
        <Route path="/mandates/create" element={<CreateMandate />} />
        <Route path="/my-qr" element={<MyQRCode />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/complaints" element={<Complaints />} />
        <Route path="/complaints/raise" element={<RaiseComplaint />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/help" element={<Help />} />
        <Route path="/change-mpin" element={<ChangeMpin />} />
        <Route path="/transaction-success" element={<TransactionSuccess />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
