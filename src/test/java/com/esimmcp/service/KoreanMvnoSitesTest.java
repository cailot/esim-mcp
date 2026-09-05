package com.esimmcp.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoreanMvnoSitesTest {

    @Test
    void coversMajorOperatorsAndComparisonHubs() {
        assertTrue(KoreanMvnoSites.HUB_URLS.size() >= 40);
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.freet.co.kr/plan/ratePlan/detail?svcCd=DDH200M6G"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.eyes.co.kr/"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.sk7mobile.com/"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.tplusmobile.com/"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://weayo.com/mobile/plan/1000001899"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.moyoplan.com/plans/34245"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://marvelring.com"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.mvnohub.kr/brand/plan/9.do"));
        assertTrue(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.mvnohub.kr/product/products/7416.do"));
        assertFalse(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.facebook.com/freet"));
        assertFalse(KoreanMvnoSites.isDiscoverablePlanUrl("https://example.com/plan"));
        assertFalse(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.mvnohub.kr/support/inquiry.do?partnerId=9"));
    }

    @Test
    void resolvesRelativeHubLinksAndAllowsDiscoveredHosts() {
        assertEquals(
                "https://www.mvnohub.kr/brand/plan/7.do",
                KoreanMvnoSites.resolveUrl("https://www.mvnohub.kr/brand.do", "/brand/plan/7.do"));
        assertEquals(
                "https://www.mvnohub.kr/product/products/7416.do",
                KoreanMvnoSites.resolveUrl("https://www.mvnohub.kr/brand/plan/9.do", "/product/products/7416.do"));

        var urls = KoreanMvnoSites.collectCandidateUrls(
                "- /url: /brand/plan/21.do\n- /url: /support/inquiry.do?partnerId=21\n- /url: https://asiamobile.kr/",
                "https://www.mvnohub.kr/brand.do",
                List.of());
        assertTrue(urls.contains("https://www.mvnohub.kr/brand/plan/21.do"));
        assertTrue(urls.contains("https://asiamobile.kr"));
        assertTrue(urls.stream().noneMatch(u -> u.contains("/support/inquiry")));
        assertFalse(KoreanMvnoSites.isCoveredOperatorHost("https://marvelring.com"));
        assertTrue(KoreanMvnoSites.isCoveredOperatorHost("https://www.freet.co.kr/main"));
    }
}
