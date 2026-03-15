package com.btg.funds.domain.model;

import java.math.BigDecimal;

public record SubscriptionRequest(
        String customerId,
        String fundId,
        String notificationType,
        BigDecimal amount
) {}
