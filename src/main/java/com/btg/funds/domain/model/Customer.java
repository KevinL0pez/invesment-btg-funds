package com.btg.funds.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private BigDecimal balance;

    @Builder.Default
    private Set<String> activeSubscriptions = new HashSet<>();

    public boolean hasSubscription(String fundId) {
        return activeSubscriptions != null && activeSubscriptions.contains(fundId);
    }

    public void addSubscription(String fundId) {
        if (this.activeSubscriptions == null) {
            this.activeSubscriptions = new HashSet<>();
        }
        this.activeSubscriptions.add(fundId);
    }

    public void removeSubscription(String fundId) {
        if (this.activeSubscriptions != null) {
            this.activeSubscriptions.remove(fundId);
        }
    }

    public void addBalance(BigDecimal amount) {
        if (this.balance == null) this.balance = BigDecimal.ZERO;
        this.balance = this.balance.add(amount);
    }

    public void subtractBalance(BigDecimal amount) {
        if (this.balance == null) this.balance = BigDecimal.ZERO;
        this.balance = this.balance.subtract(amount);
    }

    public boolean hasEnoughBalance(BigDecimal amount) {
        return this.balance != null && this.balance.compareTo(amount) >= 0;
    }
}
