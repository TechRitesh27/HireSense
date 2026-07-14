package com.p99softtraining.hiresense.dto.request;

import com.p99softtraining.hiresense.enums.Verdict;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluateQuestionRequest {

    private String notes;

    @NotNull
    private Verdict verdict;
}
