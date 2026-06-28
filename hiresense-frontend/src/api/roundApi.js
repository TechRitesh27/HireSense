import axiosInstance from './axiosInstance';

export const getRounds = (hiringDriveId) =>
  axiosInstance.get(`/hiring-drives/${hiringDriveId}/rounds`);

export const createRound = (hiringDriveId, data) =>
  axiosInstance.post(`/hiring-drives/${hiringDriveId}/rounds`, data);
