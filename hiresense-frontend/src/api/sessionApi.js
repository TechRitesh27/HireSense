import axiosInstance from './axiosInstance';

export const startSession = (sessionId, data) =>
  axiosInstance.post(`/sessions/${sessionId}/start`, data);

export const getSession = (sessionId) =>
  axiosInstance.get(`/sessions/${sessionId}`);

export const addCustomQuestion = (sessionId, data) =>
  axiosInstance.post(`/sessions/${sessionId}/questions`, data);

export const evaluateQuestion = (sessionId, questionId, data) =>
  axiosInstance.put(`/sessions/${sessionId}/questions/${questionId}/evaluate`, data);

export const submitSession = (sessionId) =>
  axiosInstance.post(`/sessions/${sessionId}/submit`);

export const getAssignedCandidates = () =>
  axiosInstance.get('/interviewers/me/assigned-candidates');
