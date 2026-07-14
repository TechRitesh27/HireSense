package com.p99softtraining.hiresense.dto.request;

import com.p99softtraining.hiresense.enums.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCustomQuestionRequest {

    @NotBlank
    private String questionText;

    @NotNull
    private DifficultyLevel difficultyLevel;
}
