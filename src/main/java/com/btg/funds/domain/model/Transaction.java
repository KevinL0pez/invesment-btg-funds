package com.btg.funds.domain.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String customerId;
    private String fundId;
    private String fundName;
    private BigDecimal amount;
    private String type;
    private Long timestamp;
}
