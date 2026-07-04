package com.srivinayakabakery.comingsoon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sends an email to the bakery inbox whenever someone signs up for
 * launch notifications. Mail is optional: if no SMTP host is configured
 * (spring.mail.host), Spring creates no JavaMailSender and this service
 * only logs — subscriptions keep working either way.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z");

    private final JavaMailSender mailSender;
    private final String notificationTo;
    private final String from;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notification-to:hello@srivinayakabakeryhome.com}") String notificationTo,
            @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.notificationTo = notificationTo;
        this.from = from;
        if (this.mailSender == null) {
            log.warn("SMTP is not configured (spring.mail.host is unset) — "
                    + "subscriber notifications to {} will be skipped", notificationTo);
        }
    }

    @Async
    public void notifyNewSubscriber(String subscriberEmail) {
        if (mailSender == null) {
            log.info("Skipping notification mail for {} (SMTP not configured)", subscriberEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notificationTo);
            if (!from.isBlank()) {
                message.setFrom(from);
            }
            message.setSubject("New launch signup — Sri Vinayaka Bakery Home");
            message.setText("""
                    A new visitor signed up for launch notifications on srivinayakabakeryhome.com

                    Email: %s
                    Time:  %s

                    — Coming Soon website
                    """.formatted(
                    subscriberEmail,
                    ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(TIMESTAMP)));
            mailSender.send(message);
            log.info("Notification mail sent to {} for new subscriber {}", notificationTo, subscriberEmail);
        } catch (MailException e) {
            log.error("Failed to send notification mail for subscriber {}", subscriberEmail, e);
        }
    }
}
