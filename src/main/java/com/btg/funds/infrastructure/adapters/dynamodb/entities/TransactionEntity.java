package com.btg.funds.infrastructure.adapters.dynamodb.entities;

import com.btg.funds.domain.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TransactionEntity {

    private String id;
    private String customerId;
    private String fundId;
    private String fundName;
    private Double amount;
    private String type;
    private Long timestamp;

    @DynamoDbPartitionKey
    public String getCustomerId() {
        return customerId;
    }

    @DynamoDbSortKey
    public Long getTimestamp() {
        return timestamp;
    }

    public static TransactionEntity fromDomain(Transaction tx) {
        return TransactionEntity.builder()
                .id(tx.getId())
                .customerId(tx.getCustomerId())
                .fundId(tx.getFundId())
                .fundName(tx.getFundName())
                .amount(tx.getAmount().doubleValue())
                .type(tx.getType())
                .timestamp(tx.getTimestamp())
                .build();
    }

    public Transaction toDomain() {
        return Transaction.builder()
                .id(this.id)
                .customerId(this.customerId)
                .fundId(this.fundId)
                .fundName(this.fundName)
                .amount(java.math.BigDecimal.valueOf(this.amount))
                .type(this.type)
                .timestamp(this.timestamp)
                .build();
    }
}
