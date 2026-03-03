import apiClient from './client';

export const bankApi = {
  getLinkedAccounts: () => apiClient.get('/banks/linked'),
  discoverAccounts: (phone, bankCode) => apiClient.post('/banks/discover', { mobileNumber: phone, bankCode }),
  linkAccount: (data) => apiClient.post('/banks/link', data),
  unlinkAccount: (accountId) => apiClient.delete(`/banks/${accountId}/unlink`),
  setPrimary: (accountId) => apiClient.put(`/banks/${accountId}/primary`),
  getBalance: (accountId, mpin) => apiClient.post(`/banks/${accountId}/balance`, { mpin }),
  getSupportedBanks: () => apiClient.get('/banks/supported'),
  verifyAccount: (bankCode, accountNumber) => apiClient.post('/banks/verify', { bankCode, accountNumber }),
};

export default bankApi;
