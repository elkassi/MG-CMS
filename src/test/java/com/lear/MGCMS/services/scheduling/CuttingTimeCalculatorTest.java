package com.lear.MGCMS.services.scheduling;

import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator.Resolved;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator.Source;
import com.lear.MGCMS.services.scheduling.CuttingTimeCalculator.TimingRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CuttingTimeCalculator}.
 *
 * <p>All tests exercise {@link CuttingTimeCalculator#resolve} which is pure
 * (no repository interaction). The batch method and DB loader are covered
 * by integration tests elsewhere.</p>
 */
class CuttingTimeCalculatorTest {

    private final CuttingTimeCalculator calc = new CuttingTimeCalculator();

    // helper — placement key used throughout
    private static final String P = "PLCMT-1";

    private Map<String, TimingRow> tim(Double validated, Double real) {
        Map<String, TimingRow> m = new HashMap<>();
        m.put(P, new TimingRow(validated, real));
        return m;
    }

    // -----------------------------------------------------------------------
    // Priority waterfall
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("priority: Validated > Real > tempsDeCoupe > 0")
    class Priority {

        @Test
        @DisplayName("validated present → validated wins even if real and tempsDeCoupe also present")
        void validatedWins() {
            Resolved r = calc.resolve(P, 99.0, 5, "Lectra", tim(10.0, 20.0));
            assertEquals(10.0, r.minutes, 1e-9);
            assertEquals(Source.VALIDATED, r.source);
        }

        @Test
        @DisplayName("validated null, real > 0 → real wins")
        void realWhenValidatedNull() {
            Resolved r = calc.resolve(P, 99.0, 5, "Lectra", tim(null, 20.0));
            assertEquals(20.0, r.minutes, 1e-9);
            assertEquals(Source.REAL, r.source);
        }

        @Test
        @DisplayName("validated 0, real 0 → fall back to tempsDeCoupe")
        void bothZeroFallsBack() {
            Resolved r = calc.resolve(P, 7.5, 1, "Lectra", tim(0.0, 0.0));
            assertEquals(7.5, r.minutes, 1e-9);
            assertEquals(Source.TEMPS_DE_COUPE, r.source);
        }

        @Test
        @DisplayName("placement not in map → fall back to tempsDeCoupe")
        void unknownPlacementFallsBack() {
            Resolved r = calc.resolve(P, 8.0, null, "Lectra", Collections.emptyMap());
            assertEquals(8.0, r.minutes, 1e-9);
            assertEquals(Source.TEMPS_DE_COUPE, r.source);
        }

        @Test
        @DisplayName("no timing, no tempsDeCoupe → 0.0 / NONE")
        void nothingAvailable() {
            Resolved r = calc.resolve(P, null, null, "Lectra", Collections.emptyMap());
            assertEquals(0.0, r.minutes, 1e-9);
            assertEquals(Source.NONE, r.source);
        }

        @Test
        @DisplayName("negative tempsDeCoupe is treated as no value")
        void negativeTempsIsIgnored() {
            Resolved r = calc.resolve(P, -3.0, 2, "LASER-DXF", Collections.emptyMap());
            assertEquals(0.0, r.minutes, 1e-9);
            assertEquals(Source.NONE, r.source);
        }

        @Test
        @DisplayName("null placement → skip timing lookup, fall back directly")
        void nullPlacementSkipsLookup() {
            Resolved r = calc.resolve(null, 4.0, null, "Lectra", tim(10.0, 20.0));
            assertEquals(4.0, r.minutes, 1e-9);
            assertEquals(Source.TEMPS_DE_COUPE, r.source);
        }
    }

    // -----------------------------------------------------------------------
    // LASER-DXF layer multiplier
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("LASER-DXF × nbrCouche")
    class LaserDxfMultiplier {

        @Test
        @DisplayName("LASER-DXF, nbrCouche=4, tempsDeCoupe source → × 4")
        void multipliesWhenFallback() {
            Resolved r = calc.resolve(P, 5.0, 4, "LASER-DXF", Collections.emptyMap());
            assertEquals(20.0, r.minutes, 1e-9);
            assertEquals(Source.TEMPS_DE_COUPE, r.source);
        }

        @Test
        @DisplayName("LASER-DXF, nbrCouche=1 → no multiplier")
        void noMultiplierForSingleLayer() {
            Resolved r = calc.resolve(P, 5.0, 1, "LASER-DXF", Collections.emptyMap());
            assertEquals(5.0, r.minutes, 1e-9);
        }

        @Test
        @DisplayName("LASER-DXF, nbrCouche=null → no multiplier, no NPE")
        void nullNbrCouche() {
            Resolved r = calc.resolve(P, 5.0, null, "LASER-DXF", Collections.emptyMap());
            assertEquals(5.0, r.minutes, 1e-9);
        }

        @Test
        @DisplayName("LASER-DXF, nbrCouche=4, validated source → NO multiplier (TimingModel bakes layers in)")
        void noMultiplierForValidatedSource() {
            Resolved r = calc.resolve(P, 5.0, 4, "LASER-DXF", tim(12.0, null));
            assertEquals(12.0, r.minutes, 1e-9);
            assertEquals(Source.VALIDATED, r.source);
        }

        @Test
        @DisplayName("LASER-DXF, nbrCouche=4, real source → NO multiplier")
        void noMultiplierForRealSource() {
            Resolved r = calc.resolve(P, 5.0, 4, "LASER-DXF", tim(null, 15.0));
            assertEquals(15.0, r.minutes, 1e-9);
            assertEquals(Source.REAL, r.source);
        }

        @Test
        @DisplayName("Lectra, nbrCouche=4 → multiplier does NOT apply to non-LASER-DXF types")
        void noMultiplierForOtherTypes() {
            Resolved r = calc.resolve(P, 5.0, 4, "Lectra", Collections.emptyMap());
            assertEquals(5.0, r.minutes, 1e-9);
        }
    }

    // -----------------------------------------------------------------------
    // Gerber universal doubling
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Gerber × 2 (applies to every source)")
    class GerberDoubler {

        @Test
        @DisplayName("Gerber, validated source → × 2")
        void doublesValidated() {
            Resolved r = calc.resolve(P, 99.0, 5, "Gerber", tim(10.0, 20.0));
            assertEquals(20.0, r.minutes, 1e-9);
            assertEquals(Source.VALIDATED, r.source);
        }

        @Test
        @DisplayName("Gerber, real source → × 2")
        void doublesReal() {
            Resolved r = calc.resolve(P, 99.0, 5, "Gerber", tim(null, 15.0));
            assertEquals(30.0, r.minutes, 1e-9);
            assertEquals(Source.REAL, r.source);
        }

        @Test
        @DisplayName("Gerber, tempsDeCoupe source → × 2")
        void doublesTempsDeCoupe() {
            Resolved r = calc.resolve(P, 7.0, null, "Gerber", Collections.emptyMap());
            assertEquals(14.0, r.minutes, 1e-9);
            assertEquals(Source.TEMPS_DE_COUPE, r.source);
        }

        @Test
        @DisplayName("Gerber, nothing available → stays at 0 (no × 0)")
        void doesNotDoubleZero() {
            Resolved r = calc.resolve(P, null, null, "Gerber", Collections.emptyMap());
            assertEquals(0.0, r.minutes, 1e-9);
            assertEquals(Source.NONE, r.source);
        }
    }

    // -----------------------------------------------------------------------
    // Non-regression guard for the two known consumers
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("regression: matches pre-extraction PlanDeChargeService behaviour")
    class PlanDeChargeParity {

        @Test
        @DisplayName("Lectra typical path: validated 12.3 → 12.3")
        void lectraValidated() {
            Resolved r = calc.resolve(P, 9.9, 1, "Lectra", tim(12.3, null));
            assertEquals(12.3, r.minutes, 1e-9);
        }

        @Test
        @DisplayName("Lectra IP6 validated null real 8.0 → 8.0")
        void lectraIp6Real() {
            Resolved r = calc.resolve(P, 5.0, 2, "Lectra IP6", tim(null, 8.0));
            assertEquals(8.0, r.minutes, 1e-9);
        }

        @Test
        @DisplayName("LASER-LSR tempsDeCoupe 3.0 nbrCouche 5 → 3.0 (LSR is not DXF, no layer mult)")
        void laserLsrNoMultiplier() {
            Resolved r = calc.resolve(P, 3.0, 5, "LASER-LSR", Collections.emptyMap());
            assertEquals(3.0, r.minutes, 1e-9);
        }
    }

    // -----------------------------------------------------------------------
    // Convenience overload
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("resolveMinutes() returns the same value as resolve().minutes")
    void resolveMinutesMatchesResolve() {
        double m = calc.resolveMinutes(P, 5.0, 2, "LASER-DXF", Collections.emptyMap());
        assertEquals(10.0, m, 1e-9);
    }

    @Test
    @DisplayName("null timing map does not NPE")
    void nullMapSafe() {
        Resolved r = calc.resolve(P, 4.0, null, "Lectra", null);
        assertEquals(4.0, r.minutes, 1e-9);
        assertEquals(Source.TEMPS_DE_COUPE, r.source);
    }

    // -----------------------------------------------------------------------
    // Defensive: every combination that caused the Ordonnancement drift
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("drift check: Gerber with TimingModel value — OrdonnancementService used to NOT × 2; we MUST × 2 now")
    void gerberDriftCanary() {
        // Pre-extraction: OrdonnancementService.getEstimatedCuttingTime would return 10.0.
        // Post-extraction via this bean: must return 20.0 (matches PlanDeChargeService).
        Resolved r = calc.resolve(P, 99.0, 1, "Gerber", tim(10.0, null));
        assertTrue(r.minutes > 19.99 && r.minutes < 20.01,
                "Gerber × 2 factor must apply even when TimingModel value is used");
    }
}
