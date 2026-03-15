package com.btg.funds.infrastructure.config;

import com.btg.funds.application.ports.in.GetCustomerFundsUseCase;
import com.btg.funds.application.ports.in.SubscribeFundUseCase;
import com.btg.funds.application.ports.in.UnsubscribeFundUseCase;
import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.application.ports.out.NotificationPort;
import com.btg.funds.application.usecases.GetCustomerFundsInteractor;
import com.btg.funds.application.usecases.SubscribeFundInteractor;
import com.btg.funds.application.usecases.UnsubscribeFundInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SubscribeFundUseCase subscribeFundUseCase(
            CustomerRepository customerRepository,
            FundRepository fundRepository,
            NotificationPort notificationPort) {

        return new SubscribeFundInteractor(
                customerRepository,
                fundRepository,
                notificationPort
        );
    }

    @Bean
    public UnsubscribeFundUseCase unsubscribeFundUseCase(
            CustomerRepository customerRepository,
            FundRepository fundRepository) {
        return new UnsubscribeFundInteractor(customerRepository, fundRepository);
    }

    @Bean
    public GetCustomerFundsUseCase getCustomerFundsUseCase(CustomerRepository customerRepository) {
        return new GetCustomerFundsInteractor(customerRepository);
    }
}
