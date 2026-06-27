package com.p99softtraining.hiresense.parser;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ParsedCandidateData {

    private final List<ParsedRow> rows;

    private final List<String> missingColumns;

    private final int totalRows;
}
