package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateCompanyRequest;
import com.p99softtraining.hiresense.dto.request.CreateUserRequest;
import com.p99softtraining.hiresense.dto.response.CompanyResponse;
import com.p99softtraining.hiresense.dto.response.UserResponse;

import java.util.UUID;

public interface CompanyService {

    CompanyResponse createCompany(CreateCompanyRequest request);

    UserResponse createCompanyAdmin(UUID companyId, CreateUserRequest request);
}
