package com.btg.funds.infrastructure.adapters.dynamodb.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CustomerEntity {

    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private BigDecimal balance;
    private Set<String> activeSubscriptions;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    public Set<String> getActiveSubscriptions() {
        if (activeSubscriptions == null || activeSubscriptions.isEmpty()) {
            return null;
        }
        return activeSubscriptions;
    }
}
