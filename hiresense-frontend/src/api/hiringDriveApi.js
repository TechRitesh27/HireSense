import axiosInstance from './axiosInstance';

export const getHiringDrives = () =>
  axiosInstance.get('/hiring-drives');

export const createHiringDrive = (data) =>
  axiosInstance.post('/hiring-drives', data);

export const updateHiringDriveStatus = (hiringDriveId, status) =>
  axiosInstance.patch(`/hiring-drives/${hiringDriveId}/status`, { status });
