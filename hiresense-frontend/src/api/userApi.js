import axiosInstance from './axiosInstance';

export const createInterviewer = (data) =>
  axiosInstance.post('/users/interviewers', data);

export const getInterviewers = () =>
  axiosInstance.get('/users/interviewers');
