package com.btg.funds.application.ports.out;

import com.btg.funds.domain.model.Customer;
import reactor.core.publisher.Mono;

public interface NotificationPort {
    Mono<Void> send(Customer customer, String message, String notificationType);
}
