package com.p99softtraining.hiresense.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AssignCandidateRequest {

    @NotEmpty(message = "Atleast one interviewer Id must be provided")
    private List<UUID> interviewerIds;
}
