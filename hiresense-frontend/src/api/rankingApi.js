import axiosInstance from './axiosInstance';

export const getRankings = (hiringDriveId) =>
  axiosInstance.get(`/hiring-drives/${hiringDriveId}/rankings`);
