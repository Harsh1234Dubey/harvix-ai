package com.interviewai.dto.response;

import com.interviewai.domain.Resume;
import com.interviewai.domain.ResumeVersion;

import java.time.Instant;
import java.util.List;

public record ResumeResponse(
        Long id,
        String title,
        int currentVersion,
        boolean primary,
        Instant createdAt,
        List<Version> versions
) {
    public static ResumeResponse from(Resume resume, List<ResumeVersion> versions) {
        return new ResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getCurrentVersion(),
                resume.isPrimary(),
                resume.getCreatedAt(),
                versions.stream().map(Version::from).toList());
    }

    public record Version(
            Long id,
            int versionNo,
            String filePath,
            Long fileSize,
            String fileType,
            Instant uploadedAt
    ) {
        public static Version from(ResumeVersion v) {
            return new Version(v.getId(), v.getVersionNo(), v.getFilePath(),
                    v.getFileSize(), v.getFileType(), v.getUploadedAt());
        }
    }
}
