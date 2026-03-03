import apiClient from './client';

export const billPayApi = {
  getCategories: () => apiClient.get('/bills/categories'),
  getBillers: (category) => apiClient.get(`/bills/billers?category=${category}`),
  fetchBill: (billerId, consumerId) => apiClient.post('/bills/fetch', { billerId, consumerId }),
  payBill: (data) => apiClient.post('/bills/pay', data),
  getHistory: () => apiClient.get('/bills/history'),
  getSavedBillers: () => apiClient.get('/bills/saved'),
  saveBiller: (data) => apiClient.post('/bills/save', data),
  removeSavedBiller: (id) => apiClient.delete(`/bills/saved/${id}`),
};

export const mandateApi = {
  create: (data) => apiClient.post('/mandates/create', data),
  getAll: () => apiClient.get('/mandates'),
  getById: (id) => apiClient.get(`/mandates/${id}`),
  pause: (id) => apiClient.put(`/mandates/${id}/pause`),
  resume: (id) => apiClient.put(`/mandates/${id}/resume`),
  revoke: (id) => apiClient.put(`/mandates/${id}/revoke`),
  modify: (id, data) => apiClient.put(`/mandates/${id}`, data),
  getExecutionHistory: (id) => apiClient.get(`/mandates/${id}/executions`),
  getPending: () => apiClient.get('/mandates/pending'),
  approve: (id, mpin) => apiClient.post(`/mandates/${id}/approve`, { mpin }),
  reject: (id, reason) => apiClient.post(`/mandates/${id}/reject`, { reason }),
};

export const upiLiteApi = {
  enableWallet: (bankAccountId) => apiClient.post('/upi-lite/enable', { bankAccountId }),
  getWallet: () => apiClient.get('/upi-lite/wallet'),
  topUp: (amount, mpin) => apiClient.post('/upi-lite/top-up', { amount, mpin }),
  pay: (data) => apiClient.post('/upi-lite/pay', data),
  getHistory: () => apiClient.get('/upi-lite/history'),
  disableWallet: () => apiClient.post('/upi-lite/disable'),
  getBalance: () => apiClient.get('/upi-lite/balance'),
};

export const disputeApi = {
  raise: (data) => apiClient.post('/disputes/raise', data),
  getAll: () => apiClient.get('/disputes'),
  getById: (id) => apiClient.get(`/disputes/${id}`),
  addComment: (id, comment) => apiClient.post(`/disputes/${id}/comment`, { comment }),
  escalate: (id) => apiClient.post(`/disputes/${id}/escalate`),
  getCategories: () => apiClient.get('/disputes/categories'),
};

export const qrApi = {
  generateStatic: () => apiClient.get('/qr/static'),
  generateDynamic: (amount, note) => apiClient.post('/qr/dynamic', { amount, note }),
  parseQr: (data) => apiClient.post('/qr/parse', { data }),
};
