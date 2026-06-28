import axiosInstance from './axiosInstance';

export const assignInterviewers = (hiringDriveId, candidateId, interviewerIds) =>
  axiosInstance.post(
    `/hiring-drives/${hiringDriveId}/candidates/${candidateId}/assignments`,
    { interviewerIds }
  );

export const getAssignmentsForDrive = (hiringDriveId) =>
  axiosInstance.get(`/hiring-drives/${hiringDriveId}/assignments`);

export const removeAssignment = (hiringDriveId, candidateId, assignmentId) =>
  axiosInstance.delete(
    `/hiring-drives/${hiringDriveId}/candidates/${candidateId}/assignments/${assignmentId}`
  );
