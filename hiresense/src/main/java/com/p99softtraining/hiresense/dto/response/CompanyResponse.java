package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CompanyResponse {

    private UUID id;

    private String name;

    private String address;

    private String email;
}
