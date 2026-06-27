package com.p99softtraining.hiresense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RowErrorResponse {

    private Integer rowNumber;

    private String message;
}
