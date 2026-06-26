package com.p99softtraining.hiresense.mapper;

import java.util.List;
import java.util.Map;

public class CandidatesExcelMapper {

    public static final Map<String, List<String>> FIELD_MAPPINGS = Map.of(

            "fullName",
            List.of(
                    "full name",
                    "candidate name",
                    "name"
            ),

            "email",
            List.of(
                    "email",
                    "email address",
                    "mail"
            ),

            "phone",
            List.of(
                    "phone",
                    "mobile",
                    "phone number"
            ),

            "collegeName",
            List.of(
                    "college",
                    "college name",
                    "university"
            ),

            "degree",
            List.of(
                    "degree",
                    "qualification"
            ),

            "branch",
            List.of(
                    "branch",
                    "department",
                    "specialization"
            ),

            "graduationYear",
            List.of(
                    "graduation year",
                    "passout year",
                    "year"
            ),

            "resumeUrl",
            List.of(
                    "resume",
                    "resume url",
                    "cv link"
            )
    );

    private CandidatesExcelMapper() {
    }
}
