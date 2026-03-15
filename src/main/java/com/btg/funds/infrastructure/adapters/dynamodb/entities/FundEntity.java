package com.btg.funds.infrastructure.adapters.dynamodb.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FundEntity {

    private String id;
    private String name;
    private BigDecimal minAmount;
    private String category;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
