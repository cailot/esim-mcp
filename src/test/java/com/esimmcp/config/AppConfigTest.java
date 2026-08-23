package com.esimmcp.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void localPropertiesUsedWhenEnvAbsent() {
        Properties props = new Properties();
        props.setProperty("report.email.to", "local@example.com");
        props.setProperty("spring.datasource.password", "from-file");
        props.setProperty("mcp.brave.env.BRAVE_API_KEY", "file-brave-key");

        AppConfig config = AppConfig.fromProperties(props, key -> null);

        assertEquals("local@example.com", config.reportEmailTo());
        assertEquals("from-file", config.datasourcePassword());
        assertEquals("file-brave-key", config.braveServer().env().get("BRAVE_API_KEY"));
    }

    @Test
    void githubSecretEnvOverridesProperties() {
        Properties props = new Properties();
        props.setProperty("report.email.to", "local@example.com");
        props.setProperty("spring.datasource.password", "from-file");
        props.setProperty("mcp.brave.env.BRAVE_API_KEY", "file-brave-key");
        props.setProperty("mcp.supabase.args", "-y,@supabase/mcp-server-supabase@latest,--project-ref=YOUR_PROJECT_REF");

        Map<String, String> env = Map.of(
                "REPORT_EMAIL_TO", "ci@example.com",
                "SPRING_DATASOURCE_PASSWORD", "from-secret",
                "BRAVE_API_KEY", "secret-brave-key",
                "SUPABASE_PROJECT_REF", "abc123project"
        );

        AppConfig config = AppConfig.fromProperties(props, env::get);

        assertEquals("ci@example.com", config.reportEmailTo());
        assertEquals("from-secret", config.datasourcePassword());
        assertEquals("secret-brave-key", config.braveServer().env().get("BRAVE_API_KEY"));
        assertTrue(config.supabaseServer().args().stream().anyMatch(a -> a.contains("abc123project")));
    }

    @Test
    void envKeyMappingIncludesBareMcpSecretName() {
        assertEquals("BRAVE_API_KEY", AppConfig.envKeysFor("mcp.brave.env.BRAVE_API_KEY").get(0));
        assertTrue(AppConfig.envKeysFor("spring.datasource.password")
                .contains("SPRING_DATASOURCE_PASSWORD"));
        assertTrue(AppConfig.envKeysFor("spring.datasource.password")
                .contains("ESIM_MCP_SPRING_DATASOURCE_PASSWORD"));
        assertTrue(AppConfig.envKeysFor("spring.mail.username")
                .contains("SPRING_MAIL_USERNAME"));
        assertTrue(AppConfig.envKeysFor("spring.mail.password")
                .contains("SPRING_MAIL_PASSWORD"));
    }

    @Test
    void mailSettingsFromPropertiesAndStripsAppPasswordSpaces() {
        Properties props = new Properties();
        props.setProperty("spring.mail.host", "smtp.gmail.com");
        props.setProperty("spring.mail.port", "587");
        props.setProperty("spring.mail.username", "sender@example.com");
        props.setProperty("spring.mail.password", "abcd efgh ijkl mnop");

        AppConfig config = AppConfig.fromProperties(props, key -> null);

        assertEquals("smtp.gmail.com", config.mailHost());
        assertEquals(587, config.mailPort());
        assertEquals("sender@example.com", config.mailUsername());
        assertEquals("abcdefghijklmnop", config.mailPassword());
        assertTrue(config.mailSmtpAuth());
        assertTrue(config.mailStartTlsEnabled());
    }

    @Test
    void mailSecretsFromEnvOverrideProperties() {
        Properties props = new Properties();
        props.setProperty("spring.mail.username", "file-user");
        props.setProperty("spring.mail.password", "file-pass");

        Map<String, String> env = Map.of(
                "SPRING_MAIL_USERNAME", "ci-user@example.com",
                "SPRING_MAIL_PASSWORD", "ci-app-password"
        );

        AppConfig config = AppConfig.fromProperties(props, env::get);

        assertEquals("ci-user@example.com", config.mailUsername());
        assertEquals("ci-app-password", config.mailPassword());
    }
}
