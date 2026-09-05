package com.esimmcp.service;

import com.esimmcp.domain.MvnoHubBrand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvnoHubBrandCatalogTest {

    private static final String BRAND_DIRECTORY_SNAPSHOT = """
            - heading "알뜰폰 브랜드"
            - tabpanel "전체":
              - list:
                - listitem:
                  - link:
                    - /url: /brand/plan/7.do
                    - img "A모바일(에넥스텔레콤)"
                    - paragraph: A모바일(에넥스텔레콤)
                  - link "홈페이지":
                    - /url: https://amobile.co.kr/
                  - link "상담 신청":
                    - /url: /support/inquiry.do?partnerId=7
                - listitem:
                  - link:
                    - /url: /brand/plan/20.do
                    - img "마블링"
                    - paragraph: 마블프로듀스
                  - link "홈페이지":
                    - /url: https://marvelring.com
                - listitem:
                  - link:
                    - /url: /brand/plan/9.do
                    - img "프리티 (LGU+망)"
                  - link "홈페이지":
                    - /url: https://www.freet.co.kr/main
                - listitem:
                  - link:
                    - /url: /brand/plan/17.do
                    - img "프리티 (SKT, KT망)"
                  - link "홈페이지":
                    - /url: https://www.freet.co.kr/main
            """;

    @Test
    void parsesEveryBrandCardFromHubSnapshot() {
        List<MvnoHubBrand> brands = MvnoHubBrandCatalog.parse(BRAND_DIRECTORY_SNAPSHOT);
        assertEquals(4, brands.size());
        assertEquals("7", brands.get(0).partnerId());
        assertEquals("A모바일(에넥스텔레콤)", brands.get(0).name());
        assertEquals("https://www.mvnohub.kr/brand/plan/7.do", brands.get(0).hubPlanUrl());
        assertEquals("https://amobile.co.kr/", brands.get(0).homepageUrl());
        assertEquals("마블링", brands.get(1).name());
        assertEquals("https://marvelring.com", brands.get(1).homepageUrl());
        assertEquals("9", brands.get(2).partnerId());
        assertEquals("17", brands.get(3).partnerId());
    }

    @Test
    void parsesHtmlBrandCards() {
        String html = """
                <li><a href="/brand/plan/5021.do"><img alt="스테이지파이브"></a>
                <a href="https://www.pindirectshop.com/">홈페이지</a></li>
                <li><a href="/brand/plan/1040.do"><img alt="인스모바일"></a>
                <a href="https://www.insmobile.co.kr/">홈페이지</a></li>
                """;
        List<MvnoHubBrand> brands = MvnoHubBrandCatalog.parse(html);
        assertEquals(2, brands.size());
        assertEquals("스테이지파이브", brands.get(0).name());
        assertEquals("https://www.pindirectshop.com/", brands.get(0).homepageUrl());
        assertEquals("인스모바일", brands.get(1).name());
        assertEquals("https://www.insmobile.co.kr/", brands.get(1).homepageUrl());
    }

    @Test
    void extractsProductDetailUrlsFromBrandListing() {
        String listing = """
                - /url: /product/products/7527.do
                - /url: /product/products/7416.do
                - /url: /product/products/7416.do
                - /url: /product/products/7209.do
                """;
        List<String> urls = MvnoHubBrandCatalog.productUrls(listing, 6);
        assertEquals(3, urls.size());
        assertEquals("https://www.mvnohub.kr/product/products/7527.do", urls.get(0));
        assertEquals("https://www.mvnohub.kr/product/products/7209.do", urls.get(2));
    }

    @Test
    void productUrlLimitIsRespected() {
        String listing = "/product/products/1.do /product/products/2.do /product/products/3.do";
        assertEquals(2, MvnoHubBrandCatalog.productUrls(listing, 2).size());
        assertTrue(MvnoHubBrandCatalog.productUrls("", 6).isEmpty());
    }

    @Test
    void braveQueryIncludesBrandNameAndEsimIntent() {
        MvnoHubBrand brand = new MvnoHubBrand(
                "찬스모바일", "12",
                MvnoHubBrandCatalog.hubPlanUrl("12"),
                "https://chancemobile.co.kr/");
        String query = MvnoHubBrandCatalog.braveQuery(brand);
        assertTrue(query.contains("찬스모바일"));
        assertTrue(query.contains("eSIM"));
    }

    @Test
    void parsesFullLiveBrandDirectoryPartnerIds() {
        StringBuilder snapshot = new StringBuilder();
        String[][] cards = {
                {"7", "A모바일(에넥스텔레콤)", "https://amobile.co.kr/"},
                {"3", "리브모바일", "https://www.liivm.com/"},
                {"18", "KCT (티플러스)", "https://tplusmobile.com/"},
                {"19", "LG헬로모바일", "https://direct.lghellovision.net/main.do"},
                {"4", "U+유모바일", "https://www.uplusumobile.com/"},
                {"2", "고고모바일", "https://gogomobile.co.kr/"},
                {"20", "마블링", "https://marvelring.com"},
                {"1000", "슈가모바일", "https://www.sugarmobile.co.kr/"},
                {"5", "스마텔", "https://smartel.kr/"},
                {"5021", "스테이지파이브", "https://www.pindirectshop.com/"},
                {"21", "아시아모바일", "https://asiamobile.kr/"},
                {"6", "아이즈모바일", "https://eyes.co.kr/"},
                {"8", "SK세븐모바일", "https://www.sk7mobile.com"},
                {"10", "Well", "https://www.idowell.co.kr/home/"},
                {"11", "모빙", "https://www.mobing.co.kr/main"},
                {"15", "이지모바일", "https://www.egmobile.co.kr/"},
                {"1040", "인스모바일", "https://www.insmobile.co.kr/"},
                {"5061", "조이텔", "https://joytel.co.kr/"},
                {"12", "찬스모바일", "https://chancemobile.co.kr/"},
                {"13", "KT스카이라이프", "https://www.skylife.co.kr/Main"},
                {"14", "KT엠모바일", "https://www.ktmmobile.com/main.do"},
                {"16", "이야기모바일", "https://www.eyagi.co.kr/"},
                {"5041", "토스모바일", "https://tossmobile.co.kr/"},
                {"9", "프리티 (LGU+망)", "https://www.freet.co.kr/main"},
                {"17", "프리티 (SKT, KT망)", "https://www.freet.co.kr/main"}
        };
        for (String[] card : cards) {
            snapshot.append("- /url: /brand/plan/").append(card[0]).append(".do\n")
                    .append("  - img \"").append(card[1]).append("\"\n")
                    .append("  - link \"홈페이지\"\n")
                    .append("    - /url: ").append(card[2]).append('\n');
        }
        List<MvnoHubBrand> brands = MvnoHubBrandCatalog.parse(snapshot.toString());
        assertEquals(25, brands.size());
        assertEquals("A모바일(에넥스텔레콤)", brands.get(0).name());
        assertEquals("프리티 (SKT, KT망)", brands.get(24).name());
        assertEquals("https://www.pindirectshop.com/", brands.get(9).homepageUrl());
        assertEquals("https://www.insmobile.co.kr/", brands.get(16).homepageUrl());
        assertTrue(brands.stream().allMatch(MvnoHubBrand::hasHomepage));
    }

    @Test
    void ignoresInquiryAndSocialHomepages() {
        String snapshot = """
                - /url: /brand/plan/12.do
                - img "찬스모바일"
                - link "상담 신청":
                  - /url: /support/inquiry.do?partnerId=12
                - link "홈페이지":
                  - /url: https://facebook.com/chancemobile
                """;
        List<MvnoHubBrand> brands = MvnoHubBrandCatalog.parse(snapshot);
        assertEquals(1, brands.size());
        assertEquals("찬스모바일", brands.get(0).name());
        assertFalse(brands.get(0).hasHomepage());
    }
}
