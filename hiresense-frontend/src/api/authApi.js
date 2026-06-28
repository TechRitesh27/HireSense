import axiosInstance from './axiosInstance';

export const login = (email, password) =>
  axiosInstance.post('/auth/login', { email, password });

export const register = (data) =>
  axiosInstance.post('/auth/register', data);
