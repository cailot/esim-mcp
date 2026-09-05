package com.esimmcp.domain;

import java.util.Objects;

/**
 * One MVNO brand card from {@code https://www.mvnohub.kr/brand.do}.
 */
public record MvnoHubBrand(String name, String partnerId, String hubPlanUrl, String homepageUrl) {

    public MvnoHubBrand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(partnerId, "partnerId");
        Objects.requireNonNull(hubPlanUrl, "hubPlanUrl");
        homepageUrl = homepageUrl == null ? "" : homepageUrl;
    }

    public boolean hasHomepage() {
        return !homepageUrl.isBlank();
    }
}
