import { CURRENCY } from './constants';

export function formatAmount(paisa) {
  const rupees = Math.abs(paisa) / 100;
  return new Intl.NumberFormat('en-NP', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(rupees);
}

export function formatAmountShort(paisa) {
  const rupees = Math.abs(paisa) / 100;
  if (rupees >= 100000) return `${(rupees / 100000).toFixed(1)}L`;
  if (rupees >= 1000) return `${(rupees / 1000).toFixed(1)}K`;
  return rupees.toFixed(0);
}

export function formatCurrency(paisa) {
  return `${CURRENCY.symbol} ${formatAmount(paisa)}`;
}

export function formatDate(date) {
  return new Intl.DateTimeFormat('en-NP', {
    day: '2-digit', month: 'short', year: 'numeric',
  }).format(new Date(date));
}

export function formatDateTime(date) {
  return new Intl.DateTimeFormat('en-NP', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(date));
}

export function formatTime(date) {
  return new Intl.DateTimeFormat('en-NP', {
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(date));
}

export function timeAgo(date) {
  const seconds = Math.floor((new Date() - new Date(date)) / 1000);
  if (seconds < 60) return 'Just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
  return formatDate(date);
}

export function maskAccountNumber(acc) {
  if (!acc || acc.length < 4) return acc;
  return `XXXX ${acc.slice(-4)}`;
}

export function maskPhone(phone) {
  if (!phone || phone.length < 4) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(-3)}`;
}

export function formatUpiId(upiId) {
  return upiId?.toLowerCase().trim() || '';
}

export function getInitials(name) {
  if (!name) return 'U';
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
}

export function formatRRN(rrn) {
  if (!rrn) return '';
  return rrn.replace(/(.{4})/g, '$1 ').trim();
}
