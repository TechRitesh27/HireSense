import axiosInstance from './axiosInstance';

export const parseResume = (candidateId) =>
  axiosInstance.post(`/candidates/${candidateId}/parse-resume`);

export const getCandidateProfile = (candidateId) =>
  axiosInstance.get(`/candidates/${candidateId}/profile`);
