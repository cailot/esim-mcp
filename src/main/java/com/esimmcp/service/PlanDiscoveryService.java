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
 * Discovers eSIM plans via Brave Search and Playwright MCP.
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

        mcp.client("playwright").ifPresent(client -> {
            try {
                String navigate = config.toolName("mcp.tool.playwright.navigate", "browser_navigate");
                mcp.callTool("playwright", navigate, Map.of(
                        "url", "https://www.google.com/search?q=" + query.replace(' ', '+')
                ));
                String snapshot = config.toolName("mcp.tool.playwright.snapshot", "browser_snapshot");
                var result = mcp.callTool("playwright", snapshot, Map.of());
                plans.addAll(parsePlansFromJsonOrText(mcp.extractText(result)));
            } catch (Exception e) {
                log.error("Playwright MCP failed: {}", e.getMessage());
            }
        });

        if (plans.isEmpty()) {
            log.warn("No plans discovered from MCP");
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
