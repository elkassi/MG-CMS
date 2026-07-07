package com.lear.MGCMS.services.dispatcher;

/**
 * Minimal view of a serie the dispatcher needs to reason about zone placement.
 *
 * <p>Separated from {@code CuttingRequestSerieData} and {@code OrdonnancementService.SerieDTO}
 * so the dispatcher services depend on a stable, tiny shape that any caller
 * (REST controller, batch job, test) can build without touching JPA.</p>
 *
 * <p>{@link #machine} holds the machine-type NAME (e.g. {@code "Lectra"},
 * {@code "LASER-DXF"}) — <em>not</em> a foreign-key id. This matches the
 * column type on {@code CuttingRequestSerie}.</p>
 */
public final class SerieDispatchInfo {

    private final String serieId;
    private final String sequence;
    /** Machine-type NAME (Lectra / Lectra IP6 / Gerber / LASER-DXF / LASER-LSR). */
    private final String machine;
    private final Double tempsDeCoupe;
    private final Integer nbrCouche;
    private final String placement;
    private final String preferredZoneNom;

    public SerieDispatchInfo(String serieId, String sequence, String machine,
                             Double tempsDeCoupe, Integer nbrCouche, String placement) {
        this(serieId, sequence, machine, tempsDeCoupe, nbrCouche, placement, null);
    }

    public SerieDispatchInfo(String serieId, String sequence, String machine,
                             Double tempsDeCoupe, Integer nbrCouche, String placement,
                             String preferredZoneNom) {
        this.serieId = serieId;
        this.sequence = sequence;
        this.machine = machine;
        this.tempsDeCoupe = tempsDeCoupe;
        this.nbrCouche = nbrCouche;
        this.placement = placement;
        this.preferredZoneNom = preferredZoneNom;
    }

    public String getSerieId()      { return serieId; }
    public String getSequence()     { return sequence; }
    public String getMachine()      { return machine; }
    public Double getTempsDeCoupe() { return tempsDeCoupe; }
    public Integer getNbrCouche()   { return nbrCouche; }
    public String getPlacement()    { return placement; }
    public String getPreferredZoneNom() { return preferredZoneNom; }
}
