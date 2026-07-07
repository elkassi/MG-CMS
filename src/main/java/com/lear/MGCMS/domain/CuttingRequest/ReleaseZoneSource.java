package com.lear.MGCMS.domain.CuttingRequest;

/**
 * Who last wrote {@code CuttingRequest.releaseZone} (camelCase DB column
 * {@code releaseZoneSource}, V17_02). Decides whether the zone auto-correction
 * job may overwrite it:
 *
 * <ul>
 *   <li>{@link #LOGISTICS} — the /logisticsRelease picklist fixed the zone at
 *       release; locked, auto-correction never touches it.</li>
 *   <li>{@link #CHEF} — a chef set it manually (floor "pas ma zone" or the
 *       rectification screen); locked against auto-correction.</li>
 *   <li>{@link #AUTO} — inferred by {@code SequenceZoneAutoCorrectService}
 *       from the STRICT zone of the table that worked the last serie; may be
 *       re-inferred on every pass.</li>
 *   <li>{@code null} — legacy/unknown writer; treated like {@link #AUTO}
 *       (correctable), because zones written before the picklist went live
 *       are guesses.</li>
 * </ul>
 */
public final class ReleaseZoneSource {

    private ReleaseZoneSource() { }

    public static final String LOGISTICS = "LOGISTICS";
    public static final String CHEF = "CHEF";
    public static final String AUTO = "AUTO";

    /** May the auto-correction job overwrite a zone written by {@code source}? */
    public static boolean isAutoCorrectable(String source) {
        return source == null || AUTO.equals(source);
    }
}
