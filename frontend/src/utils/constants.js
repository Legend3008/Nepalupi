// Nepal UPI Constants

export const APP_NAME = 'NUPI';
export const APP_FULL_NAME = 'Nepal Unified Payment Interface';
export const APP_VERSION = '1.0.0';
export const API_BASE = '/api';

export const CURRENCY = {
  code: 'NPR',
  symbol: 'रू',
  name: 'Nepalese Rupee',
};

export const LIMITS = {
  PER_TRANSACTION: 100000_00, // Rs 1,00,000 in paisa
  DAILY: 200000_00,           // Rs 2,00,000 in paisa
  MONTHLY: 500000_00,         // Rs 5,00,000 in paisa
  UPI_LITE_MAX: 500_00,       // Rs 500 per transaction
  UPI_LITE_WALLET: 2000_00,   // Rs 2,000 wallet balance
  DAILY_COUNT: 20,
};

export const BANKS = [
  { code: 'NCHL', name: 'Nepal Clearing House', short: 'NCHL', color: '#003893' },
  { code: 'NABIL', name: 'Nabil Bank', short: 'Nabil', color: '#C80000' },
  { code: 'NIC', name: 'NIC Asia Bank', short: 'NIC Asia', color: '#E31837' },
  { code: 'GLOBAL', name: 'Global IME Bank', short: 'Global IME', color: '#00529B' },
  { code: 'NIMB', name: 'Nepal Investment Mega Bank', short: 'NIMB', color: '#1A237E' },
  { code: 'SCB', name: 'Standard Chartered Bank Nepal', short: 'SCB', color: '#0072AA' },
  { code: 'SBL', name: 'Siddhartha Bank', short: 'SBL', color: '#006341' },
  { code: 'EVEREST', name: 'Everest Bank', short: 'Everest', color: '#0D47A1' },
  { code: 'HBL', name: 'Himalayan Bank', short: 'HBL', color: '#263238' },
  { code: 'MEGA', name: 'Mega Bank Nepal', short: 'Mega', color: '#4A148C' },
  { code: 'MBL', name: 'Machhapuchchhre Bank', short: 'MBL', color: '#1B5E20' },
  { code: 'KUMARI', name: 'Kumari Bank', short: 'Kumari', color: '#FF6F00' },
  { code: 'LAXMI', name: 'Laxmi Sunrise Bank', short: 'Laxmi', color: '#B71C1C' },
  { code: 'SANIMA', name: 'Sanima Bank', short: 'Sanima', color: '#004D40' },
  { code: 'PRABHU', name: 'Prabhu Bank', short: 'Prabhu', color: '#311B92' },
  { code: 'CITIZENS', name: 'Citizens Bank International', short: 'Citizens', color: '#1A237E' },
  { code: 'PRIME', name: 'Prime Commercial Bank', short: 'Prime', color: '#880E4F' },
  { code: 'CZBIL', name: 'Civil Bank', short: 'Civil', color: '#E65100' },
  { code: 'NMB', name: 'NMB Bank', short: 'NMB', color: '#0071BC' },
  { code: 'AGRICULTURE', name: 'Agriculture Development Bank', short: 'ADBL', color: '#1B5E20' },
  { code: 'RBB', name: 'Rastriya Banijya Bank', short: 'RBB', color: '#0D47A1' },
  { code: 'NBL', name: 'Nepal Bank Limited', short: 'NBL', color: '#263238' },
];

export const TRANSACTION_TYPES = {
  SEND: 'SEND',
  RECEIVE: 'RECEIVE',
  SELF_TRANSFER: 'SELF_TRANSFER',
  BILL_PAY: 'BILL_PAY',
  MANDATE: 'MANDATE',
  UPI_LITE: 'UPI_LITE',
  COLLECT: 'COLLECT',
  QR_PAY: 'QR_PAY',
};

export const TRANSACTION_STATUS = {
  SUCCESS: 'SUCCESS',
  PENDING: 'PENDING',
  FAILED: 'FAILED',
  PROCESSING: 'PROCESSING',
  REVERSED: 'REVERSED',
  EXPIRED: 'EXPIRED',
};

export const KYC_LEVELS = {
  BASIC: { label: 'Basic', limit: 50000, color: '#FFB300' },
  MEDIUM: { label: 'Medium', limit: 100000, color: '#00C853' },
  FULL: { label: 'Full', limit: 200000, color: '#003893' },
};

export const BILL_CATEGORIES = [
  { id: 'electricity', name: 'Electricity', icon: '⚡', color: '#FFB300' },
  { id: 'water', name: 'Water', icon: '💧', color: '#2196F3' },
  { id: 'internet', name: 'Internet', icon: '🌐', color: '#4CAF50' },
  { id: 'mobile', name: 'Mobile Recharge', icon: '📱', color: '#9C27B0' },
  { id: 'television', name: 'Cable TV', icon: '📺', color: '#FF5722' },
  { id: 'insurance', name: 'Insurance', icon: '🛡️', color: '#607D8B' },
  { id: 'education', name: 'Education', icon: '🎓', color: '#3F51B5' },
  { id: 'tax', name: 'Tax Payment', icon: '🏛️', color: '#795548' },
  { id: 'ipo', name: 'IPO/FPO', icon: '📈', color: '#00BCD4' },
  { id: 'housing', name: 'Housing', icon: '🏠', color: '#8BC34A' },
  { id: 'gas', name: 'Gas/LPG', icon: '🔥', color: '#F44336' },
  { id: 'broadband', name: 'Broadband', icon: '📡', color: '#673AB7' },
];

export const QUICK_AMOUNTS = [100, 200, 500, 1000, 2000, 5000];
