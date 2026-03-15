package com.btg.funds.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties
public class AppProperties {

    private DynamoDbProperties dynamodbAws = new DynamoDbProperties();
    private Business business = new Business();
    private Aws aws = new Aws();
    private TwilioProperties twilio = new TwilioProperties();

    @Data
    public static class Business {
        private Double initialBalance;
        private String fromEmail;
        private String twilioPhone;
    }

    @Data
    public static class DynamoDbProperties {
        private String customersTable;
        private String fundsTable;
        private String transactionsTable;
    }

    @Data
    public static class Aws {
        private String region;
        private String endpoint;
        private String sesEndpoint;
        private String accessKey;
        private String secretKey;
        private String sessionToken;

        @Data
        public static class DynamoDbTableProperties {
            private String customersTable;
            private String fundsTable;
            private String transactionsTable;
        }
    }

    @Data
    public static class TwilioProperties {
        private String sid;
        private String token;
        private String messagingServiceSid;
    }

 }
