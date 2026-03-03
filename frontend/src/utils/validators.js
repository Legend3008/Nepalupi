export function validatePhone(phone) {
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length !== 10) return 'Phone number must be 10 digits';
  if (!cleaned.startsWith('98') && !cleaned.startsWith('97') && !cleaned.startsWith('96'))
    return 'Enter a valid Nepal mobile number';
  return null;
}

export function validateUpiId(upiId) {
  if (!upiId || upiId.trim().length === 0) return 'UPI ID is required';
  const regex = /^[a-zA-Z0-9._-]+@[a-zA-Z]{2,}$/;
  if (!regex.test(upiId)) return 'Invalid UPI ID format (e.g., name@bank)';
  return null;
}

export function validateAmount(amount, min = 1, max = 100000) {
  const num = parseFloat(amount);
  if (isNaN(num) || num <= 0) return 'Enter a valid amount';
  if (num < min) return `Minimum amount is रू ${min}`;
  if (num > max) return `Maximum amount is रू ${max.toLocaleString()}`;
  return null;
}

export function validateMpin(mpin) {
  if (!mpin || mpin.length !== 6) return 'MPIN must be 6 digits';
  if (!/^\d{6}$/.test(mpin)) return 'MPIN must contain only numbers';
  if (/^(\d)\1{5}$/.test(mpin)) return 'MPIN cannot be all same digits';
  if ('012345678901'.includes(mpin) || '987654321098'.includes(mpin))
    return 'MPIN cannot be sequential';
  return null;
}

export function validateOtp(otp) {
  if (!otp || otp.length !== 6) return 'OTP must be 6 digits';
  if (!/^\d{6}$/.test(otp)) return 'OTP must contain only numbers';
  return null;
}

export function validateAccountNumber(acc) {
  if (!acc || acc.trim().length < 5) return 'Enter a valid account number';
  if (!/^[0-9A-Za-z-]+$/.test(acc)) return 'Account number contains invalid characters';
  return null;
}

export function validateName(name) {
  if (!name || name.trim().length < 2) return 'Name must be at least 2 characters';
  if (name.trim().length > 100) return 'Name is too long';
  return null;
}

export function validateIfscCode(code) {
  if (!code) return 'Bank code is required';
  return null;
}
