package com.p99softtraining.hiresense.parser;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CandidateFileParser {

    boolean supports(String contentType, String fileName);

    ParsedCandidateData parse(MultipartFile file) throws IOException;

    ParsedCandidateData parse(java.io.InputStream inputStream) throws IOException;
}
