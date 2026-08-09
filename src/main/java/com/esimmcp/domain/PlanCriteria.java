package com.esimmcp.domain;

/**
 * Hard requirements for selecting an eSIM plan.
 */
public final class PlanCriteria {

    public static final PlanCriteria DEFAULT = new PlanCriteria(true, true, true, true);

    private final boolean requireEsim;
    private final boolean requireInternationalSmsReceive;
    private final boolean requireStableNonPromotionalPrice;
    private final boolean requireAvailable;

    public PlanCriteria(
            boolean requireEsim,
            boolean requireInternationalSmsReceive,
            boolean requireStableNonPromotionalPrice,
            boolean requireAvailable) {
        this.requireEsim = requireEsim;
        this.requireInternationalSmsReceive = requireInternationalSmsReceive;
        this.requireStableNonPromotionalPrice = requireStableNonPromotionalPrice;
        this.requireAvailable = requireAvailable;
    }

    public boolean requireEsim() {
        return requireEsim;
    }

    public boolean requireInternationalSmsReceive() {
        return requireInternationalSmsReceive;
    }

    public boolean requireStableNonPromotionalPrice() {
        return requireStableNonPromotionalPrice;
    }

    public boolean requireAvailable() {
        return requireAvailable;
    }

    public boolean matches(EsimPlan plan) {
        if (requireAvailable && !plan.available()) {
            return false;
        }
        if (requireEsim && !plan.esimSupported()) {
            return false;
        }
        if (requireInternationalSmsReceive && !plan.internationalSmsReceive()) {
            return false;
        }
        if (requireStableNonPromotionalPrice && plan.promotionalPrice()) {
            return false;
        }
        return plan.monthlyPrice() != null && plan.monthlyPrice().signum() >= 0;
    }
}
