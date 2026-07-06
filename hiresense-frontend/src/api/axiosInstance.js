import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every request automatically
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('hiresense_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Global response error handling
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only clear the token and redirect on 401 (token expired / missing)
    // 403 means forbidden but authenticated — don't logout, let the caller handle it
    if (error.response?.status === 401) {
      const isLoginEndpoint = error.config?.url?.includes('/auth/login');
      if (!isLoginEndpoint) {
        localStorage.removeItem('hiresense_token');
        localStorage.removeItem('hiresense_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
