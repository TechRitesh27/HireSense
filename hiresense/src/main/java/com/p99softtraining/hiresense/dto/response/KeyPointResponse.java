package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class KeyPointResponse {

    private UUID id;
    private String pointText;
    private boolean covered;
}
