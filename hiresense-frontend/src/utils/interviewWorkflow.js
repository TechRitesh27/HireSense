export const WORKFLOW_STEPS = [
  {
    key: 'IMPORTED',
    label: 'Imported',
    description: 'Candidate has been imported into the hiring drive.',
  },
  {
    key: 'ASSIGNED',
    label: 'Assigned',
    description: 'Interviewers have been assigned to the candidate.',
  },
  {
    key: 'INTERVIEW_IN_PROGRESS',
    label: 'In Progress',
    description: 'The interviewer has started the interview session.',
  },
  {
    key: 'INTERVIEW_COMPLETED',
    label: 'Completed',
    description: 'The interview session has been submitted and scored.',
  },
  {
    key: 'SELECTED',
    label: 'Selected',
    description: 'The candidate has been selected after evaluation.',
  },
  {
    key: 'REJECTED',
    label: 'Rejected',
    description: 'The candidate has been rejected after evaluation.',
  },
];

export function getCandidateWorkflowStage(status) {
  const normalized = (status || 'IMPORTED').toUpperCase();

  switch (normalized) {
    case 'ASSIGNED':
      return WORKFLOW_STEPS[1];
    case 'INTERVIEW_IN_PROGRESS':
      return WORKFLOW_STEPS[2];
    case 'INTERVIEW_COMPLETED':
      return WORKFLOW_STEPS[3];
    case 'SELECTED':
      return WORKFLOW_STEPS[4];
    case 'REJECTED':
      return WORKFLOW_STEPS[5];
    case 'IMPORTED':
    default:
      return WORKFLOW_STEPS[0];
  }
}

export function getCandidateWorkflowLabel(status) {
  return getCandidateWorkflowStage(status).label;
}

export function getCandidateWorkflowHint(status) {
  return getCandidateWorkflowStage(status).description;
}

export function getCandidateWorkflowColor(status) {
  const normalized = (status || 'IMPORTED').toUpperCase();

  switch (normalized) {
    case 'ASSIGNED':
      return 'primary';
    case 'INTERVIEW_IN_PROGRESS':
      return 'warning';
    case 'INTERVIEW_COMPLETED':
      return 'info';
    case 'SELECTED':
      return 'success';
    case 'REJECTED':
      return 'error';
    case 'IMPORTED':
    default:
      return 'default';
  }
}
