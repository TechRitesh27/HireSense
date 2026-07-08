package com.p99softtraining.hiresense.dto;

import java.util.List;

public record ResumeExtractionResult(
        List<String> skills,
        List<ProjectData> projects
) {
    public record ProjectData(
            String name,
            List<String> techStack,
            String description
    ) {}
}
