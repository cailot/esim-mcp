package com.esimmcp.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Seed crawl targets covering Korean MVNO operators and comparison hubs.
 *
 * <p>Operator coverage is based on the KMVNO / public MVNO brand list plus live cards from
 * {@code https://www.mvnohub.kr/brand.do}. Sites without a stable official URL are reached via
 * 알뜰폰허브, 아요, 모요.
 */
public final class KoreanMvnoSites {

    private static final Pattern PLAN_PATH = Pattern.compile(
            "plan|rate|esim|usim|product|charge|fee|pric|mobile/mvnos|brand",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Official operator sites plus comparison hubs. Playwright visits each as a seed hub.
     */
    public static final List<String> HUB_URLS = List.of(
            // Comparison / marketplaces
            "https://weayo.com/plan/list/1?f_esim=on&f_sort=1008",
            "https://www.moyoplan.com/plans",
            "https://www.moyoplan.com/ranking",
            "https://www.mvnohub.kr/main",
            "https://www.mvnohub.kr/brand.do",
            "https://www.mvnohub.kr/product/products.do",
            "https://www.phoneb.co.kr",
            "https://ksimmarket.com/",
            "https://www.epost.go.kr/comm.alddl.RetrieveAlddlChargeList.comm",
            // Major operators
            "https://direct.lghellovision.net/esim/everythingEsim.do",
            "https://www.freet.co.kr/plan/ratePlan",
            "https://www.eyes.co.kr/",
            "https://www.sk7mobile.com/",
            "https://www.ktmmobile.com/",
            "https://www.uplusumobile.com/product/pric/usim/pricList",
            "https://www.tplusmobile.com/",
            "https://www.liivm.com/",
            "https://www.tossmobile.co.kr/",
            "https://www.eyagi.co.kr/shop/index.php",
            "https://www.mobing.co.kr/",
            "https://www.smartel.kr/",
            "https://mobile.skylife.co.kr/",
            "https://www.amobile.co.kr/",
            "https://www.flashmobile.co.kr/",
            "https://www.findirect.co.kr/",
            "https://www.joytel.co.kr/",
            "https://www.egmobile.co.kr/",
            "https://www.siwolmobile.com/",
            "https://www.insim.co.kr/",
            "https://www.ntelecom.co.kr/",
            "https://www.m2mobile.co.kr/",
            "https://www.needs.co.kr/",
            "https://www.s1.co.kr/",
            "https://www.pointpark.co.kr/",
            "https://www.wontelecom.kr/",
            "https://www.gogofactory.co.kr/",
            "https://www.yeoyu.co.kr/",
            "https://www.snowmanmobile.com/",
            "https://www.hanpassmobile.co.kr/",
            "https://www.kgmobile.co.kr/",
            "https://www.dosirakmobile.com/",
            "https://www.sugarmobile.co.kr/",
            "https://www.wellmobile.co.kr/",
            "https://www.iplusu.co.kr/",
            "https://www.marbl.co.kr/",
            "https://www.emartmobile.co.kr/",
            "https://www.valuecom.co.kr/",
            "https://www.pinplay.co.kr/",
            "https://www.erel.co.kr/",
            "https://www.cellmobile.co.kr/",
            "https://www.asia-mobile.co.kr/",
            "https://www.chance-mobile.co.kr/"
    );

    private static final List<String> HOST_FRAGMENTS = List.of(
            "weayo.com",
            "moyoplan.com",
            "mvnohub.kr",
            "phoneb.co.kr",
            "ksimmarket.com",
            "epost.go.kr",
            "lghellovision.net",
            "freet.co.kr",
            "eyes.co.kr",
            "sk7mobile.com",
            "ktmmobile.com",
            "uplusumobile.com",
            "uplusmvno.com",
            "tplusmobile.com",
            "tplus.co.kr",
            "liivm.com",
            "tossmobile.co.kr",
            "eyagi.co.kr",
            "mobing.co.kr",
            "smartel.kr",
            "smartel.co.kr",
            "skylife.co.kr",
            "amobile.co.kr",
            "flashmobile.co.kr",
            "findirect.co.kr",
            "joytel.co.kr",
            "joytel.kr",
            "egmobile.co.kr",
            "siwolmobile.com",
            "insim.co.kr",
            "ntelecom.co.kr",
            "m2mobile.co.kr",
            "needs.co.kr",
            "s1.co.kr",
            "pointpark.co.kr",
            "wontelecom.kr",
            "gogofactory.co.kr",
            "gogomobile.co.kr",
            "yeoyu.co.kr",
            "snowmanmobile.com",
            "hanpassmobile.co.kr",
            "hanpass.com",
            "kgmobile.co.kr",
            "kgmobilians.co.kr",
            "dosirakmobile.com",
            "sugarmobile.co.kr",
            "sugar-mobile.com",
            "wellmobile.co.kr",
            "iplusu.co.kr",
            "marbl.co.kr",
            "emartmobile.co.kr",
            "valuecom.co.kr",
            "pinplay.co.kr",
            "erel.co.kr",
            "cellmobile.co.kr",
            "asia-mobile.co.kr",
            "chance-mobile.co.kr",
            "ktctplus.co.kr",
            "iz.co.kr",
            "marvelring.com",
            "pindirectshop.com",
            "asiamobile.kr",
            "idowell.co.kr",
            "insmobile.co.kr",
            "chancemobile.co.kr"
    );

    private KoreanMvnoSites() {
    }

    public static boolean isDiscoverablePlanUrl(String url) {
        return isDiscoverablePlanUrl(url, List.of());
    }

    public static boolean isDiscoverablePlanUrl(String url, Collection<String> extraHostFragments) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("javascript:") || lower.contains("/login") || lower.contains("/cart")
                || lower.contains("facebook.com") || lower.contains("instagram.com")
                || lower.contains("youtube.com") || lower.contains("blog.naver.com")
                || isJunkPath(lower)) {
            return false;
        }
        boolean known = isKnownHost(lower) || containsHostFragment(lower, extraHostFragments);
        if (!known) {
            return false;
        }
        if (isComparisonHost(lower)) {
            if (lower.contains("mvnohub.kr")) {
                return isUsefulMvnoHubPath(lower);
            }
            return lower.contains("weayo.com/mobile/plan/")
                    || lower.contains("weayo.com/plan/")
                    || lower.contains("weayo.com/mobile/mvnos/")
                    || lower.contains("moyoplan.com/plans/")
                    || lower.contains("moyoplan.com/mvnos/")
                    || lower.contains("phoneb.co.kr/detail")
                    || lower.contains("ksimmarket.com")
                    || lower.contains("epost.go.kr");
        }
        return PLAN_PATH.matcher(lower).find() || isHubUrl(url) || isOperatorHome(lower);
    }

    /**
     * Turns snapshot/HTML relative links into absolute URLs using the page that contained them.
     */
    public static String resolveUrl(String baseUrl, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        String trimmed = rawUrl.trim().replaceAll("[),.;]+$", "");
        if (trimmed.startsWith("javascript:") || trimmed.startsWith("#") || trimmed.startsWith("mailto:")) {
            return "";
        }
        try {
            URI resolved;
            if (trimmed.startsWith("//")) {
                resolved = URI.create("https:" + trimmed);
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                resolved = URI.create(trimmed);
            } else if (baseUrl == null || baseUrl.isBlank()) {
                if (trimmed.startsWith("/")) {
                    resolved = URI.create(MvnoHubBrandCatalog.ORIGIN + trimmed);
                } else {
                    return "";
                }
            } else {
                resolved = URI.create(baseUrl).resolve(trimmed);
            }
            String ascii = resolved.toString();
            return ascii.contains("#") ? ascii.substring(0, ascii.indexOf('#')) : ascii;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static List<String> collectCandidateUrls(
            String pageText, String baseUrl, Collection<String> extraHostFragments) {
        Collection<String> extras = extraHostFragments == null ? List.of() : extraHostFragments;
        List<String> urls = new ArrayList<>();
        for (String raw : MvnoHubBrandCatalog.rawUrlsFromPage(pageText)) {
            String resolved = resolveUrl(baseUrl, raw);
            if (isDiscoverablePlanUrl(resolved, extras)) {
                urls.add(resolved.replaceAll("/+$", ""));
            }
        }
        return List.copyOf(urls);
    }

    public static String hostFragmentOf(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(url).getHost();
            if (host == null || host.isBlank()) {
                return "";
            }
            return host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    static boolean isJunkPath(String urlLower) {
        return urlLower.contains("/support/inquiry")
                || urlLower.contains("/policy/")
                || urlLower.contains("/support/about")
                || urlLower.contains("/support/reviews")
                || urlLower.contains("/support/order")
                || urlLower.contains("/product/phones")
                || urlLower.contains("/product/customer")
                || urlLower.contains("/event/events")
                || urlLower.contains("/custom-product/");
    }

    static boolean isUsefulMvnoHubPath(String urlLower) {
        return urlLower.contains("/brand/plan/")
                || urlLower.contains("/product/products/")
                || urlLower.contains("/brand.do")
                || urlLower.contains("/product/products.do")
                || urlLower.contains("/main");
    }

    static boolean isOperatorHome(String urlLower) {
        try {
            URI uri = URI.create(urlLower.startsWith("http") ? urlLower : "https://" + urlLower);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return true;
            }
            String normalized = path.replaceAll("/+$", "").toLowerCase(Locale.ROOT);
            return normalized.equals("/main")
                    || normalized.equals("/main.do")
                    || normalized.equals("/home")
                    || normalized.equals("/index")
                    || normalized.equals("/index.php")
                    || normalized.equals("/index.do");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean containsHostFragment(String urlLower, Collection<String> extraHostFragments) {
        if (extraHostFragments == null || extraHostFragments.isEmpty()) {
            return false;
        }
        for (String fragment : extraHostFragments) {
            if (fragment != null && !fragment.isBlank() && urlLower.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isKnownHost(String urlLower) {
        for (String fragment : HOST_FRAGMENTS) {
            if (urlLower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    static boolean isComparisonHost(String urlLower) {
        return urlLower.contains("weayo.com")
                || urlLower.contains("moyoplan.com")
                || urlLower.contains("mvnohub.kr")
                || urlLower.contains("phoneb.co.kr")
                || urlLower.contains("ksimmarket.com")
                || urlLower.contains("epost.go.kr");
    }

    static boolean isCoveredOperatorHost(String homepageUrl) {
        String host = hostFragmentOf(homepageUrl);
        if (host.isBlank()) {
            return false;
        }
        for (String hub : HUB_URLS) {
            String hubHost = hostFragmentOf(hub);
            if (host.equals(hubHost) || hub.toLowerCase(Locale.ROOT).contains(host)) {
                return true;
            }
        }
        return false;
    }

    static boolean isHubUrl(String url) {
        String normalized = url.trim().replaceAll("/+$", "");
        for (String hub : HUB_URLS) {
            if (normalized.equalsIgnoreCase(hub.replaceAll("/+$", ""))) {
                return true;
            }
        }
        return false;
    }
}
