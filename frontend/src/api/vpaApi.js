import apiClient from './client';

export const vpaApi = {
  resolve: (vpaAddress) => apiClient.post('/vpa/resolve', { vpaAddress }),
  create: (data) => apiClient.post('/vpa/create', data),
  getMyVpas: () => apiClient.get('/vpa/my'),
  delete: (vpaId) => apiClient.delete(`/vpa/${vpaId}`),
  transfer: (vpaId, targetBankAccountId) => apiClient.post(`/vpa/${vpaId}/transfer`, { targetBankAccountId }),
  checkAvailability: (vpaAddress) => apiClient.get(`/vpa/check/${vpaAddress}`),
  setPrimary: (vpaId) => apiClient.put(`/vpa/${vpaId}/primary`),
};

export default vpaApi;
