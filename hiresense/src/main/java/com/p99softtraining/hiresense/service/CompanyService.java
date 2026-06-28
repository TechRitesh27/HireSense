package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.dto.request.CreateCompanyRequest;
import com.p99softtraining.hiresense.dto.request.CreateUserRequest;
import com.p99softtraining.hiresense.dto.response.CompanyResponse;
import com.p99softtraining.hiresense.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface CompanyService {

    CompanyResponse createCompany(CreateCompanyRequest request);

    UserResponse createCompanyAdmin(UUID companyId, CreateUserRequest request);

    List<CompanyResponse> getAllCompanies();

    List<UserResponse> getAdminsForCompany(UUID companyId);
}
