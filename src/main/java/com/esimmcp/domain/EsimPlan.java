package com.esimmcp.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Candidate eSIM plan discovered from the web.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EsimPlan {

    private final String provider;
    private final String planName;
    private final String countryOrRegion;
    private final boolean esimSupported;
    private final boolean internationalSmsReceive;
    private final boolean available;
    private final BigDecimal monthlyPrice;
    private final String currency;
    private final boolean promotionalPrice;
    private final BigDecimal regularMonthlyPrice;
    private final String sourceUrl;
    private final String notes;

    private EsimPlan(Builder builder) {
        this.provider = builder.provider;
        this.planName = builder.planName;
        this.countryOrRegion = builder.countryOrRegion;
        this.esimSupported = builder.esimSupported;
        this.internationalSmsReceive = builder.internationalSmsReceive;
        this.available = builder.available;
        this.monthlyPrice = builder.monthlyPrice;
        this.currency = builder.currency;
        this.promotionalPrice = builder.promotionalPrice;
        this.regularMonthlyPrice = builder.regularMonthlyPrice;
        this.sourceUrl = builder.sourceUrl;
        this.notes = builder.notes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String provider() {
        return provider;
    }

    public String planName() {
        return planName;
    }

    public String countryOrRegion() {
        return countryOrRegion;
    }

    public boolean esimSupported() {
        return esimSupported;
    }

    public boolean internationalSmsReceive() {
        return internationalSmsReceive;
    }

    /** Whether the plan is currently joinable (not sold out / closed / signup blocked). */
    public boolean available() {
        return available;
    }

    public BigDecimal monthlyPrice() {
        return monthlyPrice;
    }

    public String currency() {
        return currency;
    }

    public boolean promotionalPrice() {
        return promotionalPrice;
    }

    public BigDecimal regularMonthlyPrice() {
        return regularMonthlyPrice;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public String notes() {
        return notes;
    }

    /** Effective ongoing monthly price used for ranking (rejects promo-only deals). */
    public BigDecimal ongoingMonthlyPrice() {
        if (promotionalPrice && regularMonthlyPrice != null) {
            return regularMonthlyPrice;
        }
        return monthlyPrice;
    }

    @Override
    public String toString() {
        return provider + " / " + planName + " @ " + ongoingMonthlyPrice() + " " + currency
                + (available ? "" : " [unavailable]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EsimPlan that)) {
            return false;
        }
        return Objects.equals(provider, that.provider)
                && Objects.equals(planName, that.planName)
                && Objects.equals(sourceUrl, that.sourceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, planName, sourceUrl);
    }

    public static final class Builder {
        private String provider;
        private String planName;
        private String countryOrRegion;
        private boolean esimSupported;
        private boolean internationalSmsReceive;
        private boolean available = true;
        private BigDecimal monthlyPrice;
        private String currency = "KRW";
        private boolean promotionalPrice;
        private BigDecimal regularMonthlyPrice;
        private String sourceUrl;
        private String notes = "";

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder planName(String planName) {
            this.planName = planName;
            return this;
        }

        public Builder countryOrRegion(String countryOrRegion) {
            this.countryOrRegion = countryOrRegion;
            return this;
        }

        public Builder esimSupported(boolean esimSupported) {
            this.esimSupported = esimSupported;
            return this;
        }

        public Builder internationalSmsReceive(boolean internationalSmsReceive) {
            this.internationalSmsReceive = internationalSmsReceive;
            return this;
        }

        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public Builder monthlyPrice(BigDecimal monthlyPrice) {
            this.monthlyPrice = monthlyPrice;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder promotionalPrice(boolean promotionalPrice) {
            this.promotionalPrice = promotionalPrice;
            return this;
        }

        public Builder regularMonthlyPrice(BigDecimal regularMonthlyPrice) {
            this.regularMonthlyPrice = regularMonthlyPrice;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public EsimPlan build() {
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(planName, "planName");
            Objects.requireNonNull(monthlyPrice, "monthlyPrice");
            if (PlanAvailabilitySignals.looksUnavailable(notes)) {
                this.available = false;
            }
            return new EsimPlan(this);
        }
    }
}
