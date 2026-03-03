import apiClient from './client';

export const authApi = {
  sendOtp: (phone) => apiClient.post('/auth/otp/send', { mobileNumber: phone }),
  verifyOtp: (phone, otp) => apiClient.post('/auth/otp/verify', { mobileNumber: phone, otp }),
  register: (data) => apiClient.post('/auth/register', data),
  setupMpin: (userId, mpin) => apiClient.post('/auth/mpin/setup', { userId, mpin }),
  verifyMpin: (userId, mpin) => apiClient.post('/auth/mpin/verify', { userId, mpin }),
  changeMpin: (userId, oldMpin, newMpin) => apiClient.post('/auth/mpin/change', { userId, oldMpin, newMpin }),
  resetMpin: (data) => apiClient.post('/auth/mpin/reset', data),
  refreshToken: () => apiClient.post('/auth/token/refresh'),
  logout: () => apiClient.post('/auth/logout'),
  getProfile: () => apiClient.get('/auth/profile'),
  updateProfile: (data) => apiClient.put('/auth/profile', data),
  bindDevice: (data) => apiClient.post('/auth/device/bind', data),
  verifyBiometric: (data) => apiClient.post('/auth/biometric/verify', data),
};

export default authApi;
