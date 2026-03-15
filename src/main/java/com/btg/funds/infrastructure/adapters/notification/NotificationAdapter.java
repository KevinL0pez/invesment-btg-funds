package com.btg.funds.infrastructure.adapters.notification;

import com.btg.funds.application.ports.out.NotificationPort;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.infrastructure.config.AppProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationPort {

    private final SesAsyncClient sesAsyncClient;
    private final AppProperties appProps;

    @PostConstruct
    public void initTwilio() {
        String sid = trimToNull(appProps.getTwilio().getSid());
        String token = trimToNull(appProps.getTwilio().getToken());

        if (sid == null || token == null) {
            log.warn("Twilio no inicializado: faltan credenciales (TWILIO_SID/TWILIO_TOKEN).");
            return;
        }

        Twilio.init(sid, token);
        log.info("Twilio inicializado correctamente para notificaciones SMS.");
    }

    @Override
    public Mono<Void> send(Customer customer, String message, String notificationType) {
        String channel = trimToNull(notificationType);
        if (channel == null) {
            return Mono.error(new IllegalArgumentException("notificationType es obligatorio"));
        }

        return switch (channel.toUpperCase()) {
            case "SMS" -> validateAndSendSms(customer, message);
            case "EMAIL" -> validateAndSendEmail(customer, message);
            default -> Mono.error(new IllegalArgumentException("Canal no soportado: " + channel));
        };
    }

    private Mono<Void> validateAndSendSms(Customer customer, String message) {
        if (customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) {
            log.warn("Suscripción exitosa, pero no se envió SMS: Cliente sin teléfono.");
            return Mono.empty();
        }
        return sendSms(customer.getPhoneNumber(), message);
    }

    private Mono<Void> validateAndSendEmail(Customer customer, String message) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return Mono.error(new IllegalArgumentException("Cliente sin correo electrónico registrado"));
        }
        return sendEmail(customer.getEmail(), message);
    }

    private Mono<Void> sendEmail(String email, String bodyText) {
        String fromEmail = trimToNull(appProps.getBusiness().getFromEmail());
        if (fromEmail == null) {
            log.warn("No se envió EMAIL: configura SES_MAIL (remitente verificado en SES).");
            return Mono.empty();
        }

        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(email).build())
                .message(software.amazon.awssdk.services.ses.model.Message.builder()
                        .subject(Content.builder().data("BTG Pactual - Notificación de Movimiento").build())
                        .body(Body.builder().text(Content.builder().data(bodyText).build()).build())
                        .build())
                .source(fromEmail)
                .build();

        return Mono.fromFuture(sesAsyncClient.sendEmail(request))
                .doOnSuccess(s -> log.info("📧 Email enviado exitosamente a: {}", email))
                .onErrorResume(e -> {
                    log.error("⚠️ Falló el envío de EMAIL por SES: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> sendSms(String phone, String bodyText) {
        return Mono.fromRunnable(() -> {
                    String messagingServiceSid = trimToNull(appProps.getTwilio().getMessagingServiceSid());
                    String fromPhone = trimToNull(appProps.getBusiness().getTwilioPhone());

                    Message message;
                    if (messagingServiceSid != null) {
                        log.info("🚀 Enviando SMS a {} usando Messaging Service SID: {}", phone, messagingServiceSid);
                        message = Message.creator(new PhoneNumber(phone), messagingServiceSid, bodyText).create();
                    } else if (fromPhone != null) {
                        log.info("🚀 Enviando SMS a {} usando número origen: {}", phone, fromPhone);
                        message = Message.creator(new PhoneNumber(phone), new PhoneNumber(fromPhone), bodyText).create();
                    } else {
                        log.warn("No se envió SMS: configura TWILIO_MESSAGING_SERVICE_SID o TWILIO_PHONE.");
                        return;
                    }

                    log.info("✅ SMS enviado con éxito. Twilio SID: {}", message.getSid());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("⚠️ Falló el envío de SMS de Twilio: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
