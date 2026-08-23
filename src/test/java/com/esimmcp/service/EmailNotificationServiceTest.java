package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.DailyReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailNotificationServiceTest {

    @Test
    void skipsWhenRecipientBlank() {
        Properties props = new Properties();
        props.setProperty("report.email.to", "");
        AppConfig config = AppConfig.fromProperties(props, key -> null);

        assertDoesNotThrow(() -> new EmailNotificationService(config).send(sampleReport()));
    }

    @Test
    void failsWhenMailCredentialsMissing() {
        Properties props = new Properties();
        props.setProperty("report.email.to", "to@example.com");
        AppConfig config = AppConfig.fromProperties(props, key -> null);

        assertThrows(IllegalStateException.class,
                () -> new EmailNotificationService(config).send(sampleReport()));
    }

    private static DailyReport sampleReport() {
        return new DailyReport(
                LocalDate.parse("2026-08-23"),
                Instant.parse("2026-08-23T01:00:00Z"),
                List.of(),
                List.of(),
                null,
                "test notes"
        );
    }
}
