import axiosInstance from './axiosInstance';

// Companies
export const getAllCompanies = () =>
  axiosInstance.get('/companies');

export const createCompany = (data) =>
  axiosInstance.post('/companies', data);

// Company Admins
export const getAdminsForCompany = (companyId) =>
  axiosInstance.get(`/companies/${companyId}/admins`);

export const createCompanyAdmin = (companyId, data) =>
  axiosInstance.post(`/companies/${companyId}/admins`, data);
