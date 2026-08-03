package com.interviewai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String name,

        @Size(max = 5000) String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 255) String website,
        @Size(max = 100) String industry,
        @Size(max = 255) String location,
        @Size(max = 50) String sizeRange,
        Integer foundedYear,
        @Size(max = 20) String brandingColor
) {
}
