package com.p99softtraining.hiresense.dto.request;

import com.p99softtraining.hiresense.enums.RoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInterviewRoundRequest {

    @NotBlank(message = "Round name must not be blank")
    private String name;

    @NotNull(message = "Round type must not be null")
    private RoundType roundType;
}
