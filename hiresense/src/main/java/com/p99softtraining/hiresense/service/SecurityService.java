package com.p99softtraining.hiresense.service;

import com.p99softtraining.hiresense.entity.Company;
import com.p99softtraining.hiresense.entity.User;

public interface SecurityService {

    User getCurrentUser();

    Company getCurrentUserCompany();
}
