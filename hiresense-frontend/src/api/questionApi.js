import axiosInstance from './axiosInstance';

export const generateQuestions = (candidateId, roundId) =>
  axiosInstance.post(`/candidates/${candidateId}/rounds/${roundId}/questions/generate`);

export const getQuestions = (candidateId, roundId) =>
  axiosInstance.get(`/candidates/${candidateId}/rounds/${roundId}/questions`);
