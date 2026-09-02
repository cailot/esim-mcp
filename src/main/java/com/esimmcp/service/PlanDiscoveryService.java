package com.esimmcp.service;

import com.esimmcp.config.AppConfig;
import com.esimmcp.domain.EsimPlan;
import com.esimmcp.domain.PlanAvailabilitySignals;
import com.esimmcp.mcp.McpClientManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers eSIM plans via Brave Search and Playwright MCP.
 *
 * <p>Brave/Playwright return free-text, not structured EsimPlan JSON. We therefore:
 * <ol>
 *   <li>start from priced keep-fee seeds plus all Korean MVNO/comparison hubs</li>
 *   <li>enrich with Brave hit URLs when useful</li>
 *   <li>Playwright-verify each URL for sold-out / closed signals</li>
 * </ol>
 */
public final class PlanDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(PlanDiscoveryService.class);

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final int MAX_CANDIDATES = 80;


    private final AppConfig config;
    private final McpClientManager mcp;
    private final ObjectMapper objectMapper;

    public PlanDiscoveryService(AppConfig config, McpClientManager mcp, ObjectMapper objectMapper) {
        this.config = config;
        this.mcp = mcp;
        this.objectMapper = objectMapper;
    }

    public List<EsimPlan> discover() {
        Map<String, EsimPlan> byUrl = new LinkedHashMap<>();
        for (EsimPlan seed : seedCandidates()) {
            byUrl.put(normalizeUrl(seed.sourceUrl()), seed);
        }
        for (String hub : KoreanMvnoSites.HUB_URLS) {
            addDiscoveredUrl(byUrl, hub, "Catalog seed");
        }

        mcp.client("brave").ifPresent(client -> {
            for (String query : config.searchQueries()) {
                enrichFromBrave(byUrl, query);
            }
        });

        discoverFromHubs(byUrl);
        byUrl.entrySet().removeIf(entry -> {
            String notes = entry.getValue().notes();
            return notes != null && notes.startsWith("Catalog seed")
                    && entry.getValue().monthlyPrice().compareTo(new BigDecimal("999999")) == 0;
        });
        capCandidates(byUrl);

        List<EsimPlan> verified = new ArrayList<>();
        for (EsimPlan candidate : byUrl.values()) {
            verified.add(verifyWithPlaywright(candidate));
        }

        log.info("Discovery complete: {} candidate(s) after Playwright verification", verified.size());
        return verified;
    }

    private void enrichFromBrave(Map<String, EsimPlan> byUrl, String query) {
        try {
            String tool = config.toolName("mcp.tool.brave.search", "brave_web_search");
            var result = mcp.callTool("brave", tool, Map.of(
                    "query", query,
                    "count", 20
            ));
            String text = mcp.extractText(result);
            int before = byUrl.size();
            for (EsimPlan parsed : parsePlansFromJsonOrText(text)) {
                if (parsed.sourceUrl() != null && !parsed.sourceUrl().isBlank()) {
                    byUrl.putIfAbsent(normalizeUrl(parsed.sourceUrl()), parsed);
                }
            }
            for (String url : extractUsefulUrls(text)) {
                addDiscoveredUrl(byUrl, url, "Discovered URL from Brave; needs verification");
            }
            log.info("Brave Search '{}' enriched candidates: +{} (total={})",
                    query, byUrl.size() - before, byUrl.size());
        } catch (Exception e) {
            log.error("Brave Search MCP failed for '{}': {}", query, e.getMessage());
        }
    }

    private void discoverFromHubs(Map<String, EsimPlan> byUrl) {
        if (mcp.client("playwright").isEmpty()) {
            return;
        }
        String navigate = config.toolName("mcp.tool.playwright.navigate", "browser_navigate");
        String snapshot = config.toolName("mcp.tool.playwright.snapshot", "browser_snapshot");
        for (String hub : KoreanMvnoSites.HUB_URLS) {
            try {
                mcp.callTool("playwright", navigate, Map.of("url", hub));
                String pageText = mcp.extractText(mcp.callTool("playwright", snapshot, Map.of()));
                int before = byUrl.size();
                for (String url : extractUsefulUrls(pageText)) {
                    addDiscoveredUrl(byUrl, url, "Discovered from hub " + hub);
                }
                log.info("Hub {} enriched candidates: +{} (total={})", hub, byUrl.size() - before, byUrl.size());
            } catch (Exception e) {
                log.warn("Hub crawl failed for {}: {}", hub, e.getMessage());
            }
        }
    }

    private static void addDiscoveredUrl(Map<String, EsimPlan> byUrl, String url, String notes) {
        byUrl.putIfAbsent(normalizeUrl(url), EsimPlan.builder()
                .provider("web-discovery")
                .planName(url)
                .countryOrRegion("Korea")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .available(true)
                .monthlyPrice(new BigDecimal("999999"))
                .currency("KRW")
                .promotionalPrice(false)
                .sourceUrl(url)
                .notes(notes)
                .build());
    }

    private static void capCandidates(Map<String, EsimPlan> byUrl) {
        if (byUrl.size() <= MAX_CANDIDATES) {
            return;
        }
        Map<String, EsimPlan> kept = new LinkedHashMap<>();
        for (EsimPlan seed : seedCandidates()) {
            String key = normalizeUrl(seed.sourceUrl());
            EsimPlan value = byUrl.get(key);
            if (value != null) {
                kept.put(key, value);
            }
        }
        for (Map.Entry<String, EsimPlan> entry : byUrl.entrySet()) {
            if (kept.size() >= MAX_CANDIDATES) {
                break;
            }
            kept.putIfAbsent(entry.getKey(), entry.getValue());
        }
        byUrl.clear();
        byUrl.putAll(kept);
        log.info("Capped discovery candidates at {}", byUrl.size());
    }

    private EsimPlan verifyWithPlaywright(EsimPlan candidate) {
        String url = candidate.sourceUrl();
        if (url == null || url.isBlank() || mcp.client("playwright").isEmpty()) {
            return candidate;
        }
        try {
            String navigate = config.toolName("mcp.tool.playwright.navigate", "browser_navigate");
            mcp.callTool("playwright", navigate, Map.of("url", url));
            String snapshot = config.toolName("mcp.tool.playwright.snapshot", "browser_snapshot");
            String pageText = mcp.extractText(mcp.callTool("playwright", snapshot, Map.of()));
            boolean closed = PlanAvailabilitySignals.looksUnavailable(pageText);
            boolean esimMention = pageText.toLowerCase(Locale.ROOT).contains("esim") || pageText.contains("eSIM");
            boolean roamingMention = pageText.contains("해외로밍") || pageText.contains("해외 로밍")
                    || pageText.contains("로밍");
            boolean lifetime = candidate.lifetimeDiscount() || looksLikeLifetimeKeepFee(pageText);
            boolean stepUp = looksLikePriceStepUp(pageText) && !lifetime;

            String notes = candidate.notes() == null ? "" : candidate.notes();
            if (closed) {
                notes = (notes + " | Playwright: 마감/가입불가 신호 감지").trim();
            } else {
                notes = (notes + " | Playwright: page opened").trim();
            }
            if (lifetime) {
                notes = (notes + " | lifetime keep fee").trim();
            }
            if (stepUp) {
                notes = (notes + " | price steps up after term").trim();
            }

            return EsimPlan.builder()
                    .provider(candidate.provider())
                    .planName(candidate.planName())
                    .countryOrRegion(candidate.countryOrRegion())
                    .esimSupported(candidate.esimSupported() || esimMention)
                    .internationalSmsReceive(candidate.internationalSmsReceive() || roamingMention)
                    .available(candidate.available() && !closed)
                    .monthlyPrice(candidate.monthlyPrice())
                    .currency(candidate.currency())
                    .promotionalPrice(candidate.promotionalPrice() || stepUp || lifetime)
                    .lifetimeDiscount(lifetime)
                    .regularMonthlyPrice(candidate.regularMonthlyPrice())
                    .sourceUrl(candidate.sourceUrl())
                    .notes(notes)
                    .build();
        } catch (Exception e) {
            log.warn("Playwright verify failed for {}: {}", url, e.getMessage());
            return candidate;
        }
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
                        .filter(p -> p.sourceUrl() != null && !p.sourceUrl().isBlank())
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Could not parse plans as JSON: {}", e.getMessage());
        }
        // Free-text Brave/Playwright output is NOT a structured plan — do not invent
        // a fake candidate with esim=false/promo=true (that always fails criteria).
        return List.of();
    }

    private static List<String> extractUsefulUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group().replaceAll("[),.;]+$", "");
            if (KoreanMvnoSites.isDiscoverablePlanUrl(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private static boolean looksLikeLifetimeKeepFee(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("평생 할인")
                || text.contains("평생할인")
                || text.contains("평생요금")
                || text.contains("평생 요금")
                || text.contains("평생 특가")
                || text.contains("평생특가");
    }

    private static boolean looksLikePriceStepUp(String text) {
        if (text == null || text.isBlank() || looksLikeLifetimeKeepFee(text)) {
            return false;
        }
        return text.contains("개월 이후")
                || text.contains("개월차")
                || text.contains("이후 정상가")
                || text.contains("프로모션 종료 후");
    }

    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.trim().replaceAll("/+$", "");
    }

    /** Curated keep-number eSIM candidates used when search returns unstructured text. */
    private static List<EsimPlan> seedCandidates() {
        return List.of(
                EsimPlan.builder()
                        .provider("LG헬로모바일")
                        .planName("슬림 유심 500MB 50분")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("1700"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://weayo.com/mobile/plan/1000001726")
                        .notes("Seed: lifelong-style low keep fee; verify availability")
                        .build(),
                EsimPlan.builder()
                        .provider("티플러스")
                        .planName("우체국 티플음성200")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("2000"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://www.moyoplan.com/plans/23684")
                        .notes("Seed: eSIM+USIM on Moyo")
                        .build(),
                EsimPlan.builder()
                        .provider("KT엠모바일")
                        .planName("초알뜰 3GB/100분")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("4900"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://weayo.com/mobile/plan/1000025139")
                        .notes("Seed: eSIM fee applies; roaming listed")
                        .build(),
                EsimPlan.builder()
                        .provider("KT스카이라이프")
                        .planName("초슬림 500M/60분")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("1900"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://weayo.com/mobile/plan/1000014573")
                        .notes("Seed: previously reported as 마감 — must verify")
                        .build(),
                EsimPlan.builder()
                        .provider("ksimmarket")
                        .planName("해외본인인증 eSIM")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("1900"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://ksimmarket.com/")
                        .notes("Seed: overseas signup may be blocked")
                        .build(),
                EsimPlan.builder()
                        .provider("프리티모바일")
                        .planName("든든한 200분6G")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("550"))
                        .currency("KRW")
                        .promotionalPrice(true)
                        .lifetimeDiscount(true)
                        .regularMonthlyPrice(new BigDecimal("25300"))
                        .sourceUrl("https://www.freet.co.kr/plan/ratePlan/detail?svcCd=DDH200M6G")
                        .notes("Seed: 평생 특가 유지비; 개통기한/사용량 조건은 무시")
                        .build(),
                EsimPlan.builder()
                        .provider("프리티모바일")
                        .planName("든든한 200분6G (아요)")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("550"))
                        .currency("KRW")
                        .promotionalPrice(true)
                        .lifetimeDiscount(true)
                        .regularMonthlyPrice(new BigDecimal("25300"))
                        .sourceUrl("https://weayo.com/mobile/plan/1000001899")
                        .notes("Seed: weayo listing for FreeT lifetime 특가")
                        .build(),
                EsimPlan.builder()
                        .provider("아이즈모바일")
                        .planName("아이즈우정 300분/5G")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("999999"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://weayo.com/mobile/plan/1000001947")
                        .notes("Seed: eSIM listed on weayo")
                        .build(),
                EsimPlan.builder()
                        .provider("리브엠")
                        .planName("LTE 실속 5GB(200분)")
                        .countryOrRegion("Korea")
                        .esimSupported(true)
                        .internationalSmsReceive(true)
                        .available(true)
                        .monthlyPrice(new BigDecimal("999999"))
                        .currency("KRW")
                        .promotionalPrice(false)
                        .sourceUrl("https://weayo.com/mobile/plan/1000005389")
                        .notes("Seed: Liiv M eSIM listing")
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
        public Boolean available;
        public BigDecimal monthlyPrice;
        public String currency = "KRW";
        public boolean promotionalPrice;
        public boolean lifetimeDiscount;
        public BigDecimal regularMonthlyPrice;
        public String sourceUrl;
        public String notes;

        EsimPlan toDomain() {
            boolean isAvailable = available == null || available;
            if (PlanAvailabilitySignals.looksUnavailable(notes)) {
                isAvailable = false;
            }
            return EsimPlan.builder()
                    .provider(provider == null ? "unknown" : provider)
                    .planName(planName == null ? "unknown" : planName)
                    .countryOrRegion(countryOrRegion)
                    .esimSupported(esimSupported)
                    .internationalSmsReceive(internationalSmsReceive)
                    .available(isAvailable)
                    .monthlyPrice(monthlyPrice == null ? new BigDecimal("999999") : monthlyPrice)
                    .currency(currency == null ? "KRW" : currency)
                    .promotionalPrice(promotionalPrice)
                    .lifetimeDiscount(lifetimeDiscount)
                    .regularMonthlyPrice(regularMonthlyPrice)
                    .sourceUrl(sourceUrl)
                    .notes(notes == null ? "" : notes)
                    .build();
        }
    }
}
