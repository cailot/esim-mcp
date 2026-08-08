package com.esimmcp.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Loads all configuration from {@code application.properties} only.
 * No {@code .env} or process environment overrides.
 */
public final class AppConfig {

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
        return new AppConfig(props);
    }

    public LocalDate reportEndDate() {
        return LocalDate.parse(get("report.end.date", "2026-09-15"));
    }

    public int reportHour() {
        return Integer.parseInt(get("report.cron.hour", "9"));
    }

    public int reportMinute() {
        return Integer.parseInt(get("report.cron.minute", "0"));
    }

    public ZoneId timezone() {
        return ZoneId.of(get("report.timezone", "Asia/Seoul"));
    }

    public String searchQuery() {
        return get("search.query", "best cheap eSIM Korea international SMS receive monthly plan");
    }

    public String reportEmailTo() {
        return get("report.email.to", "");
    }

    public String reportSubjectPrefix() {
        return get("report.email.subject.prefix", "[esim-mcp] Daily eSIM report");
    }

    public String datasourceUrl() {
        return get("spring.datasource.url", "");
    }

    public String datasourceUsername() {
        return get("spring.datasource.username", "");
    }

    public String datasourcePassword() {
        return get("spring.datasource.password", "");
    }

    public String datasourceDriverClassName() {
        return get("spring.datasource.driver-class-name", "org.postgresql.Driver");
    }

    public String toolName(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    public McpServerSpec braveServer() {
        return serverSpec("brave");
    }

    public McpServerSpec playwrightServer() {
        return serverSpec("playwright");
    }

    public McpServerSpec sequentialServer() {
        return serverSpec("sequential");
    }

    public McpServerSpec supabaseServer() {
        return serverSpec("supabase");
    }

    public McpServerSpec gmailServer() {
        return serverSpec("gmail");
    }

    private McpServerSpec serverSpec(String name) {
        String command = get("mcp." + name + ".command", "");
        List<String> args = csv(get("mcp." + name + ".args", ""));
        Map<String, String> env = envMap(name);
        return new McpServerSpec(name, command, args, env);
    }

    private Map<String, String> envMap(String name) {
        String prefix = "mcp." + name + ".env.";
        Map<String, String> env = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String envKey = key.substring(prefix.length());
                String value = properties.getProperty(key, "");
                if (!value.isBlank()) {
                    env.put(envKey, value);
                }
            }
        }
        return Collections.unmodifiableMap(env);
    }

    private List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
