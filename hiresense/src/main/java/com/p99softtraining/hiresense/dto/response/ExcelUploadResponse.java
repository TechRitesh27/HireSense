package com.p99softtraining.hiresense.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ExcelUploadResponse {

    private Integer totalRows;

    private Integer successCount;

    private Integer failedCount;

    private List<String> missingColumns;

    private List<RowErrorResponse> errors;
}
