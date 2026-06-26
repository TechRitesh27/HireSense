package com.p99softtraining.hiresense.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCandidateRequest {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String collegeName;

    @NotBlank
    private String degree;

    @NotBlank
    private String branch;

    @NotNull
    @Min(1900)
    private Integer graduationYear;

    @NotBlank
    private String resumeUrl;
}
