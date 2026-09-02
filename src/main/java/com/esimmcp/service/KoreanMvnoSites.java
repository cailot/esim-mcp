package com.esimmcp.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Seed crawl targets covering Korean MVNO operators and comparison hubs.
 *
 * <p>Operator coverage is based on the KMVNO / public MVNO brand list. Sites without a
 * stable official URL are reached via 알뜰폰허브, 아요, 모요.
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
            "iz.co.kr"
    );

    private KoreanMvnoSites() {
    }

    public static boolean isDiscoverablePlanUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("javascript:") || lower.contains("/login") || lower.contains("/cart")
                || lower.contains("facebook.com") || lower.contains("instagram.com")
                || lower.contains("youtube.com") || lower.contains("blog.naver.com")) {
            return false;
        }
        if (!isKnownHost(lower)) {
            return false;
        }
        if (isComparisonHost(lower)) {
            return lower.contains("weayo.com/mobile/plan/")
                    || lower.contains("weayo.com/plan/")
                    || lower.contains("weayo.com/mobile/mvnos/")
                    || lower.contains("moyoplan.com/plans/")
                    || lower.contains("moyoplan.com/mvnos/")
                    || lower.contains("mvnohub.kr")
                    || lower.contains("phoneb.co.kr/detail")
                    || lower.contains("ksimmarket.com")
                    || lower.contains("epost.go.kr");
        }
        return PLAN_PATH.matcher(lower).find() || isHubUrl(url);
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
