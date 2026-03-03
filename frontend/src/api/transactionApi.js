import apiClient from './client';

export const transactionApi = {
  send: (data) => apiClient.post('/transactions/send', data),
  collect: (data) => apiClient.post('/transactions/collect', data),
  selfTransfer: (data) => apiClient.post('/transactions/self-transfer', data),
  getHistory: (page = 0, size = 20, filters = {}) => {
    const params = new URLSearchParams({ page, size, ...filters });
    return apiClient.get(`/transactions/history?${params}`);
  },
  getDetail: (id) => apiClient.get(`/transactions/${id}`),
  checkStatus: (txnId) => apiClient.get(`/transactions/${txnId}/status`),
  getPending: () => apiClient.get('/transactions/pending'),
  approveCollect: (txnId, mpin) => apiClient.post(`/transactions/${txnId}/approve`, { mpin }),
  declineCollect: (txnId, reason) => apiClient.post(`/transactions/${txnId}/decline`, { reason }),
  getStats: (period = 'month') => apiClient.get(`/transactions/stats?period=${period}`),
  reverse: (txnId, reason) => apiClient.post(`/transactions/${txnId}/reverse`, { reason }),
};

export default transactionApi;
