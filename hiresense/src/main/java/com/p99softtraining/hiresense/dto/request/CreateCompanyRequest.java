package com.p99softtraining.hiresense.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @Email
    @NotBlank
    private String email;
}
