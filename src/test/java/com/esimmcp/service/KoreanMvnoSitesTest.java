package com.esimmcp.service;

import org.junit.jupiter.api.Test;

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
        assertFalse(KoreanMvnoSites.isDiscoverablePlanUrl("https://www.facebook.com/freet"));
        assertFalse(KoreanMvnoSites.isDiscoverablePlanUrl("https://example.com/plan"));
    }
}
