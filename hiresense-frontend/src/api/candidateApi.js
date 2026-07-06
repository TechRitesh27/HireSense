import axiosInstance from './axiosInstance';

export const getCandidates = (hiringDriveId) =>
  axiosInstance.get(`/hiring-drives/${hiringDriveId}/candidates`);

export const createCandidate = (hiringDriveId, data) =>
  axiosInstance.post(`/hiring-drives/${hiringDriveId}/candidates`, data);

export const uploadCandidatesExcel = (hiringDriveId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return axiosInstance.post(`/hiring-drives/${hiringDriveId}/candidates/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const importCandidatesFromUrl = (hiringDriveId, url) =>
  axiosInstance.post(`/hiring-drives/${hiringDriveId}/candidates/import-url`, null, {
    params: { url },
  });
