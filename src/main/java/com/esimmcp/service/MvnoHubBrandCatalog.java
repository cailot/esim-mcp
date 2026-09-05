package com.esimmcp.service;

import com.esimmcp.domain.MvnoHubBrand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the 알뜰폰허브 brand directory and per-brand plan listings.
 *
 * <p>Playwright snapshots are YAML-like ({@code /url:}, {@code img "name"}). The same
 * regexes also accept HTML {@code href}/{@code alt} so MCP text-extract variants still work.
 */
public final class MvnoHubBrandCatalog {

    public static final String ORIGIN = "https://www.mvnohub.kr";
    public static final String BRAND_LIST_URL = ORIGIN + "/brand.do";

    /** Hub checkbox value for eSIM ({@code usimTypeIds=93}). */
    public static final String ESIM_FILTER_INPUT_ID = "usimType_93";

    static final int MAX_PRODUCTS_PER_BRAND = 6;

    private static final Pattern BRAND_PLAN_PATH = Pattern.compile("/brand/plan/(\\d+)\\.do");
    private static final Pattern PRODUCT_PATH = Pattern.compile("/product/products/(\\d+)\\.do");
    private static final Pattern IMG_NAME = Pattern.compile("img\\s+\"([^\"]+)\"");
    private static final Pattern IMG_ALT = Pattern.compile("alt=\"([^\"]+)\"");
    private static final Pattern HOMEPAGE_ABSOLUTE = Pattern.compile(
            "홈페이지[\\s\\S]{0,500}?(https?://[^\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern YAML_URL = Pattern.compile("/url:\\s*(\\S+)");
    private static final Pattern HREF_URL = Pattern.compile("href=[\"']([^\"']+)[\"']");
    private static final Pattern ABSOLUTE_URL = Pattern.compile("https?://[^\\s\"'<>]+");

    private MvnoHubBrandCatalog() {
    }

    public static String hubPlanUrl(String partnerId) {
        return ORIGIN + "/brand/plan/" + partnerId + ".do";
    }

    public static String braveQuery(MvnoHubBrand brand) {
        return brand.name() + " 알뜰폰 eSIM 요금제 해외 SMS 수신 평생요금 가입가능";
    }

    /**
     * Extracts every brand card: hub plan path, display name, and homepage.
     */
    public static List<MvnoHubBrand> parse(String pageText) {
        if (pageText == null || pageText.isBlank()) {
            return List.of();
        }
        Map<String, MvnoHubBrand> byId = new LinkedHashMap<>();
        Matcher matcher = BRAND_PLAN_PATH.matcher(pageText);
        List<int[]> spans = new ArrayList<>();
        while (matcher.find()) {
            spans.add(new int[] {matcher.start(), matcher.end(), Integer.parseInt(matcher.group(1))});
        }
        for (int i = 0; i < spans.size(); i++) {
            int[] span = spans.get(i);
            String partnerId = Integer.toString(span[2]);
            int windowEnd = i + 1 < spans.size() ? spans.get(i + 1)[0] : Math.min(pageText.length(), span[1] + 2500);
            String window = pageText.substring(span[0], windowEnd);
            String name = extractName(window);
            String homepage = extractHomepage(window);
            if (name.isBlank()) {
                name = "MVNO-" + partnerId;
            }
            byId.putIfAbsent(partnerId, new MvnoHubBrand(name, partnerId, hubPlanUrl(partnerId), homepage));
        }
        return List.copyOf(byId.values());
    }

    /**
     * Product detail paths on a brand plan listing, resolved to absolute hub URLs.
     */
    public static List<String> productUrls(String pageText, int limit) {
        if (pageText == null || pageText.isBlank() || limit <= 0) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        Matcher matcher = PRODUCT_PATH.matcher(pageText);
        while (matcher.find() && unique.size() < limit) {
            String id = matcher.group(1);
            unique.putIfAbsent(id, ORIGIN + "/product/products/" + id + ".do");
        }
        return List.copyOf(unique.values());
    }

    static List<String> rawUrlsFromPage(String pageText) {
        if (pageText == null || pageText.isBlank()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        addMatches(unique, YAML_URL.matcher(pageText), 1);
        addMatches(unique, HREF_URL.matcher(pageText), 1);
        addMatches(unique, ABSOLUTE_URL.matcher(pageText), 0);
        return List.copyOf(unique.values());
    }

    private static void addMatches(Map<String, String> unique, Matcher matcher, int group) {
        while (matcher.find()) {
            String value = matcher.group(group).replaceAll("[),.;]+$", "");
            if (!value.isBlank()) {
                unique.putIfAbsent(value, value);
            }
        }
    }

    private static String extractName(String window) {
        Matcher img = IMG_NAME.matcher(window);
        while (img.find()) {
            String name = sanitizeName(img.group(1));
            if (!name.isBlank()) {
                return name;
            }
        }
        Matcher alt = IMG_ALT.matcher(window);
        while (alt.find()) {
            String name = sanitizeName(alt.group(1));
            if (!name.isBlank()) {
                return name;
            }
        }
        return "";
    }

    private static String extractHomepage(String window) {
        Matcher labeled = HOMEPAGE_ABSOLUTE.matcher(window);
        if (labeled.find()) {
            String url = cleanUrl(labeled.group(1));
            if (isHomepageCandidate(url)) {
                return url;
            }
        }
        boolean seenHomepage = window.contains("홈페이지");
        if (!seenHomepage) {
            return "";
        }
        Matcher yaml = YAML_URL.matcher(window);
        while (yaml.find()) {
            String url = cleanUrl(yaml.group(1));
            if (isHomepageCandidate(url)) {
                return url;
            }
        }
        Matcher href = HREF_URL.matcher(window);
        while (href.find()) {
            String url = cleanUrl(href.group(1));
            if (isHomepageCandidate(url)) {
                return url;
            }
        }
        return "";
    }

    private static boolean isHomepageCandidate(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        return !lower.contains("mvnohub.kr")
                && !lower.contains("javascript:")
                && !lower.contains("facebook.com")
                && !lower.contains("instagram.com");
    }

    private static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.replaceAll("\\s+", " ").trim();
        if (name.isBlank() || name.equals("홈페이지") || name.equals("상담 신청") || name.startsWith("통신망")) {
            return "";
        }
        return name;
    }

    private static String cleanUrl(String url) {
        return url == null ? "" : url.replaceAll("[),.;]+$", "").trim();
    }
}
