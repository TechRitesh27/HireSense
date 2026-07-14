import axiosInstance from './axiosInstance';

export const getRankings = (hiringDriveId, roundId) =>
  axiosInstance.get(`/hiring-drives/${hiringDriveId}/rounds/${roundId}/rankings`);
