const BASE_URL = '/api';

class ApiClient {
  constructor() {
    this.baseUrl = BASE_URL;
  }

  getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('nupi_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;
    headers['X-API-Version'] = '1';
    return headers;
  }

  async request(method, path, body = null, customHeaders = {}) {
    const url = `${this.baseUrl}${path}`;
    const options = {
      method,
      headers: { ...this.getHeaders(), ...customHeaders },
    };
    if (body) options.body = JSON.stringify(body);

    try {
      const response = await fetch(url, options);
      const data = response.headers.get('content-type')?.includes('application/json')
        ? await response.json()
        : await response.text();

      if (!response.ok) {
        const error = new Error(data.message || data.error || 'Request failed');
        error.status = response.status;
        error.data = data;
        throw error;
      }
      return data;
    } catch (err) {
      if (err.status === 401) {
        localStorage.removeItem('nupi_token');
        localStorage.removeItem('nupi_user');
        window.location.href = '/login';
      }
      throw err;
    }
  }

  get(path) { return this.request('GET', path); }
  post(path, body) { return this.request('POST', path, body); }
  put(path, body) { return this.request('PUT', path, body); }
  patch(path, body) { return this.request('PATCH', path, body); }
  delete(path) { return this.request('DELETE', path); }
}

export const apiClient = new ApiClient();
export default apiClient;
