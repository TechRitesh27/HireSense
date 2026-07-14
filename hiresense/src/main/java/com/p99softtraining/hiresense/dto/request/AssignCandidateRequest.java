package com.p99softtraining.hiresense.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AssignCandidateRequest {

    @NotEmpty(message = "Atleast one interviewer Id must be provided")
    private List<UUID> interviewerIds;

    @NotNull(message = "Interview round ID must be provided")
    private UUID interviewRoundId;
}
