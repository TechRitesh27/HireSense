import axiosInstance from './axiosInstance';

export const startSession = (candidateId, roundId) =>
  axiosInstance.post(`/candidates/${candidateId}/rounds/${roundId}/sessions`);

export const getSession = (sessionId) =>
  axiosInstance.get(`/sessions/${sessionId}`);

export const markKeyPoints = (sessionId, questionId, coveredKeyPointIds) =>
  axiosInstance.patch(`/sessions/${sessionId}/questions/${questionId}/key-points`, {
    coveredKeyPointIds,
  });

export const evaluateQuestion = (sessionId, questionId, data) =>
  axiosInstance.patch(`/sessions/${sessionId}/questions/${questionId}/evaluate`, data);

export const submitSession = (sessionId) =>
  axiosInstance.post(`/sessions/${sessionId}/submit`);
