package com.interviewai.dto.response;

import com.interviewai.domain.Company;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String website,
        String industry,
        String location,
        String sizeRange,
        Integer foundedYear,
        String brandingColor,
        boolean verified,
        Instant createdAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getDescription(),
                company.getLogoUrl(),
                company.getWebsite(),
                company.getIndustry(),
                company.getLocation(),
                company.getSizeRange(),
                company.getFoundedYear(),
                company.getBrandingColor(),
                company.isVerified(),
                company.getCreatedAt());
    }
}
