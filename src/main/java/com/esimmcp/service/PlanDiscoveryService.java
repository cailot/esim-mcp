package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.EsimPlan;
import com.esimmcp.mcp.McpClientManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Discovers eSIM plans via Brave Search and Puppeteer MCP.
 */
public final class PlanDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(PlanDiscoveryService.class);

    private final AppConfig config;
    private final McpClientManager mcp;
    private final ObjectMapper objectMapper;

    public PlanDiscoveryService(AppConfig config, McpClientManager mcp, ObjectMapper objectMapper) {
        this.config = config;
        this.mcp = mcp;
        this.objectMapper = objectMapper;
    }

    public List<EsimPlan> discover() {
        if (mcp.dryRun()) {
            log.info("Dry-run discovery: returning sample candidate plans");
            return samplePlans();
        }

        List<EsimPlan> plans = new ArrayList<>();
        String query = config.searchQuery();

        mcp.client("brave").ifPresent(client -> {
            try {
                String tool = config.toolName("mcp.tool.brave.search", "brave_web_search");
                var result = mcp.callTool("brave", tool, Map.of(
                        "query", query,
                        "count", 10
                ));
                String text = mcp.extractText(result);
                plans.addAll(parsePlansFromJsonOrText(text));
                log.info("Brave Search returned {} candidate plan(s)", plans.size());
            } catch (Exception e) {
                log.error("Brave Search MCP failed: {}", e.getMessage());
            }
        });

        mcp.client("puppeteer").ifPresent(client -> {
            try {
                String navigate = config.toolName("mcp.tool.puppeteer.navigate", "puppeteer_navigate");
                mcp.callTool("puppeteer", navigate, Map.of(
                        "url", "https://www.google.com/search?q=" + query.replace(' ', '+')
                ));
                String evaluate = config.toolName("mcp.tool.puppeteer.evaluate", "puppeteer_evaluate");
                var result = mcp.callTool("puppeteer", evaluate, Map.of(
                        "script",
                        "() => document.body.innerText.slice(0, 8000)"
                ));
                plans.addAll(parsePlansFromJsonOrText(mcp.extractText(result)));
            } catch (Exception e) {
                log.error("Puppeteer MCP failed: {}", e.getMessage());
            }
        });

        if (plans.isEmpty()) {
            log.warn("No plans discovered from MCP; falling back to sample plans");
            return samplePlans();
        }
        return plans;
    }

    private List<EsimPlan> parsePlansFromJsonOrText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            String json = extractJsonArray(text);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<EsimPlanDto>>() {})
                        .stream()
                        .map(EsimPlanDto::toDomain)
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Could not parse plans as JSON: {}", e.getMessage());
        }
        // Keep raw discovery text as a single placeholder candidate for later human/AI review.
        return List.of(EsimPlan.builder()
                .provider("web-discovery")
                .planName("unparsed-result")
                .countryOrRegion("unknown")
                .esimSupported(false)
                .internationalSmsReceive(false)
                .monthlyPrice(new BigDecimal("999999"))
                .currency("USD")
                .promotionalPrice(true)
                .sourceUrl(null)
                .notes(text.length() > 2000 ? text.substring(0, 2000) : text)
                .build());
    }

    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private static List<EsimPlan> samplePlans() {
        return List.of(
                EsimPlan.builder()
                        .provider("SampleMobile")
                        .planName("Global Light")
                        .countryOrRegion("Korea / Global")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .monthlyPrice(new BigDecimal("9.99"))
                        .currency("USD")
                        .promotionalPrice(false)
                        .sourceUrl("https://example.com/sample-global-light")
                        .notes("Dry-run sample: stable monthly price")
                        .build(),
                EsimPlan.builder()
                        .provider("PromoTel")
                        .planName("Travel Intro")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .monthlyPrice(new BigDecimal("1.00"))
                        .currency("USD")
                        .promotionalPrice(true)
                        .regularMonthlyPrice(new BigDecimal("29.99"))
                        .sourceUrl("https://example.com/promo-travel")
                        .notes("Dry-run sample: promo then higher regular price")
                        .build(),
                EsimPlan.builder()
                        .provider("NoSms Carrier")
                        .planName("Data Only")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(false)
                        .monthlyPrice(new BigDecimal("5.00"))
                        .currency("USD")
                        .promotionalPrice(false)
                        .sourceUrl("https://example.com/data-only")
                        .notes("Dry-run sample: no international SMS receive")
                        .build(),
                EsimPlan.builder()
                        .provider("PhysicalOnly")
                        .planName("Classic SIM")
                        .countryOrRegion("Korea")
                        .esimSupported(false)
                        .internationalSmsReceive(true)
                        .monthlyPrice(new BigDecimal("3.00"))
                        .currency("USD")
                        .promotionalPrice(false)
                        .sourceUrl("https://example.com/classic-sim")
                        .notes("Dry-run sample: physical SIM only")
                        .build()
        );
    }

    /**
     * Loose DTO for MCP JSON payloads.
     */
    public static final class EsimPlanDto {
        public String provider;
        public String planName;
        public String countryOrRegion;
        public boolean esimSupported;
        public boolean internationalSmsReceive;
        public BigDecimal monthlyPrice;
        public String currency = "USD";
        public boolean promotionalPrice;
        public BigDecimal regularMonthlyPrice;
        public String sourceUrl;
        public String notes;

        EsimPlan toDomain() {
            return EsimPlan.builder()
                    .provider(provider == null ? "unknown" : provider)
                    .planName(planName == null ? "unknown" : planName)
                    .countryOrRegion(countryOrRegion)
                    .esimSupported(esimSupported)
                    .internationalSmsReceive(internationalSmsReceive)
                    .monthlyPrice(monthlyPrice == null ? new BigDecimal("999999") : monthlyPrice)
                    .currency(currency == null ? "USD" : currency)
                    .promotionalPrice(promotionalPrice)
                    .regularMonthlyPrice(regularMonthlyPrice)
                    .sourceUrl(sourceUrl)
                    .notes(notes == null ? "" : notes)
                    .build();
        }
    }
}
