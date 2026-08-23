package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.DailyReport;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Sends the daily report through Gmail SMTP using an app password.
 */
public final class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final AppConfig config;

    public EmailNotificationService(AppConfig config) {
        this.config = config;
    }

    public void send(DailyReport report) {
        String to = config.reportEmailTo();
        String subject = config.reportSubjectPrefix() + " " + report.reportDate();
        String textBody = report.toEmailBody();
        String htmlBody = report.toHtmlEmailBody();

        if (to.isBlank()) {
            log.warn("report.email.to is empty; skipping email send");
            return;
        }

        String username = config.mailUsername();
        String password = config.mailPassword();
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "spring.mail.username / spring.mail.password are required for SMTP send");
        }

        try {
            sendSmtp(username, password, to, subject, textBody, htmlBody);
            log.info("Sent daily report email to {} via {}:{}", to, config.mailHost(), config.mailPort());
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send report email via Gmail SMTP: " + e.getMessage(), e);
        }
    }

    private void sendSmtp(
            String username,
            String password,
            String to,
            String subject,
            String textBody,
            String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.mailHost());
        props.put("mail.smtp.port", String.valueOf(config.mailPort()));
        props.put("mail.smtp.auth", String.valueOf(config.mailSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.mailStartTlsEnabled()));
        props.put("mail.smtp.starttls.required", String.valueOf(config.mailStartTlsRequired()));
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject, "UTF-8");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(textBody, "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
    }
}
