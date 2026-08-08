package com.esimmcp.service;

import com.esimmcp.domain.EsimPlan;
import com.esimmcp.domain.PlanCriteria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanCriteriaFilterTest {

    private final PlanCriteriaFilter filter = new PlanCriteriaFilter(PlanCriteria.DEFAULT);

    @Test
    void selectsCheapestStableEsimWithIntlSms() {
        EsimPlan winner = EsimPlan.builder()
                .provider("A")
                .planName("Good")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .monthlyPrice(new BigDecimal("8.00"))
                .promotionalPrice(false)
                .build();

        EsimPlan promo = EsimPlan.builder()
                .provider("B")
                .planName("Promo")
                .esimSupported(true)
                .internationalSmsReceive(true)
                .monthlyPrice(new BigDecimal("1.00"))
                .promotionalPrice(true)
                .regularMonthlyPrice(new BigDecimal("30.00"))
                .build();

        EsimPlan noSms = EsimPlan.builder()
                .provider("C")
                .planName("Data")
                .esimSupported(true)
                .internationalSmsReceive(false)
                .monthlyPrice(new BigDecimal("2.00"))
                .promotionalPrice(false)
                .build();

        Optional<EsimPlan> best = filter.bestOf(List.of(promo, noSms, winner));
        assertTrue(best.isPresent());
        assertEquals("Good", best.get().planName());
    }

    @Test
    void rejectsPhysicalSimOnly() {
        EsimPlan physical = EsimPlan.builder()
                .provider("D")
                .planName("Classic")
                .esimSupported(false)
                .internationalSmsReceive(true)
                .monthlyPrice(new BigDecimal("1.00"))
                .promotionalPrice(false)
                .build();

        assertTrue(filter.filter(List.of(physical)).isEmpty());
    }
}
