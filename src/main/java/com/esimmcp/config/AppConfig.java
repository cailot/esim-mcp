package com.esimmcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Configuration loader.
 *
 * <p><b>Local:</b> values come from {@code application.properties}
 * (copy from {@code application.properties.example}).
 *
 * <p><b>GitHub Actions / CI:</b> the same keys can be overridden by environment
 * variables / GitHub Secrets. Non-blank env values win over the properties file.
 *
 * <p>Env naming:
 * <ul>
 *   <li>{@code spring.datasource.password} → {@code SPRING_DATASOURCE_PASSWORD}</li>
 *   <li>optional prefix {@code ESIM_MCP_} (e.g. {@code ESIM_MCP_SPRING_DATASOURCE_PASSWORD})</li>
 *   <li>MCP child env keys also accept the bare name
 *       (e.g. {@code mcp.brave.env.BRAVE_API_KEY} ← {@code BRAVE_API_KEY})</li>
 * </ul>
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Properties properties;
    private final Function<String, String> envLookup;

    private AppConfig(Properties properties, Function<String, String> envLookup) {
        this.properties = properties;
        this.envLookup = envLookup == null ? System::getenv : envLookup;
    }

    public static AppConfig load() {
        return load(System::getenv);
    }

    static AppConfig load(Function<String, String> envLookup) {
        Properties props = new Properties();
        loadClasspathProperties(props, "application.properties", true);
        if (props.isEmpty()) {
            log.info("application.properties missing; falling back to application.properties.example");
            loadClasspathProperties(props, "application.properties.example", false);
        }
        return new AppConfig(props, envLookup);
    }

    /** Test helper: build from an in-memory properties map + env lookup. */
    public static AppConfig fromProperties(Properties properties, Function<String, String> envLookup) {
        return new AppConfig(properties, envLookup);
    }

    private static void loadClasspathProperties(Properties props, String resource, boolean optional) {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (!optional) {
                    throw new IllegalStateException(resource + " not found on classpath");
                }
                return;
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resource, e);
        }
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

    public List<String> searchQueries() {
        List<String> queries = new ArrayList<>();
        String primary = searchQuery();
        if (primary != null && !primary.isBlank()) {
            queries.add(primary);
        }
        for (String extra : get("search.extra.queries", "").split("\\|")) {
            String q = extra.trim();
            if (!q.isEmpty() && !queries.contains(q)) {
                queries.add(q);
            }
        }
        return List.copyOf(queries);
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

    public String mailHost() {
        return get("spring.mail.host", "smtp.gmail.com");
    }

    public int mailPort() {
        return Integer.parseInt(get("spring.mail.port", "587"));
    }

    public String mailUsername() {
        return get("spring.mail.username", "");
    }

    /** Gmail app passwords are 16 chars; Google UI may show spaces. */
    public String mailPassword() {
        return get("spring.mail.password", "").replace(" ", "");
    }

    public boolean mailSmtpAuth() {
        return Boolean.parseBoolean(get("spring.mail.properties.mail.smtp.auth", "true"));
    }

    public boolean mailStartTlsEnabled() {
        return Boolean.parseBoolean(get("spring.mail.properties.mail.smtp.starttls.enable", "true"));
    }

    public boolean mailStartTlsRequired() {
        return Boolean.parseBoolean(get("spring.mail.properties.mail.smtp.starttls.required", "true"));
    }

    private McpServerSpec serverSpec(String name) {
        String command = get("mcp." + name + ".command", "");
        List<String> args = new ArrayList<>(csv(get("mcp." + name + ".args", "")));
        if ("supabase".equals(name)) {
            String projectRef = firstNonBlank(
                    env("SUPABASE_PROJECT_REF"),
                    get("mcp.supabase.project.ref", "")
            );
            if (!projectRef.isBlank()) {
                for (int i = 0; i < args.size(); i++) {
                    String arg = args.get(i);
                    if (arg.contains("YOUR_PROJECT_REF")) {
                        args.set(i, arg.replace("YOUR_PROJECT_REF", projectRef));
                    } else if (arg.startsWith("--project-ref=")) {
                        args.set(i, "--project-ref=" + projectRef);
                    } else if ("--project-ref".equals(arg) && i + 1 < args.size()) {
                        args.set(i + 1, projectRef);
                    }
                }
            }
        }
        Map<String, String> env = envMap(name);
        return new McpServerSpec(name, command, args, env);
    }

    private Map<String, String> envMap(String name) {
        String prefix = "mcp." + name + ".env.";
        Map<String, String> env = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String envKey = key.substring(prefix.length());
            String value = get(key, "");
            // Prefer bare process env for MCP child keys (BRAVE_API_KEY, etc.)
            String bare = env(envKey);
            if (bare != null && !bare.isBlank()) {
                value = bare;
            }
            if (value != null && !value.isBlank()) {
                env.put(envKey, value);
            }
        }
        // Also pick up common secrets even if the properties key was blank/missing.
        if ("brave".equals(name)) {
            putIfPresent(env, "BRAVE_API_KEY");
        } else if ("supabase".equals(name)) {
            putIfPresent(env, "SUPABASE_ACCESS_TOKEN");
        }
        return Collections.unmodifiableMap(env);
    }

    private void putIfPresent(Map<String, String> env, String key) {
        String value = env(key);
        if (value != null && !value.isBlank()) {
            env.put(key, value);
        }
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

    /**
     * Resolve a config value: non-blank environment variable overrides properties file.
     */
    public String get(String key, String defaultValue) {
        for (String envKey : envKeysFor(key)) {
            String fromEnv = env(envKey);
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv;
            }
        }
        return properties.getProperty(key, defaultValue);
    }

    private String env(String name) {
        return envLookup.apply(name);
    }

    static List<String> envKeysFor(String propertyKey) {
        String normalized = propertyKey.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        List<String> keys = new ArrayList<>(2);
        keys.add(normalized);
        keys.add("ESIM_MCP_" + normalized);
        // mcp.brave.env.BRAVE_API_KEY → also accept BRAVE_API_KEY
        int envIdx = propertyKey.indexOf(".env.");
        if (envIdx >= 0) {
            String bare = propertyKey.substring(envIdx + ".env.".length());
            if (!bare.isBlank()) {
                keys.add(0, bare);
            }
        }
        return keys;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return "";
    }
}
