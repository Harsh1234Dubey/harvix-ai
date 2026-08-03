package com.interviewai.dto.request;

import com.interviewai.common.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionRequest(
        @NotNull SubscriptionPlan plan,
        Integer aiQuotaMonth
) {
}
