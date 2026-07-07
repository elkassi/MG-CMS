package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;

/**
 * Compact payload the operator kiosk renders as the "next job" banner.
 *
 * <p>Intentionally flat and serializable — no JPA entities. The kiosk is
 * a thin tablet client that polls {@code /api/kiosk/nextSerie} every
 * few seconds; every field here is something the operator reads at a
 * glance.</p>
 *
 * <p>{@link #queueVersion} is the source-of-truth counter the kiosk also
 * fetches from {@code /api/kiosk/version}. When it goes up, the kiosk
 * forces a re-fetch of {@code nextSerie}.</p>
 */
public final class NextSerieDto {

    private String serieId;
    private String sequenceId;
    private String machineNom;
    private String partNumberMaterial;
    private Double longueur;
    private Integer nbrCouche;
    private Double estimatedCuttingTime;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime estimatedEndTime;
    private Long queueVersion;

    public NextSerieDto() {}

    public String getSerieId()                        { return serieId; }
    public void setSerieId(String serieId)            { this.serieId = serieId; }

    public String getSequenceId()                     { return sequenceId; }
    public void setSequenceId(String sequenceId)      { this.sequenceId = sequenceId; }

    public String getMachineNom()                     { return machineNom; }
    public void setMachineNom(String machineNom)      { this.machineNom = machineNom; }

    public String getPartNumberMaterial()                { return partNumberMaterial; }
    public void setPartNumberMaterial(String pn)         { this.partNumberMaterial = pn; }

    public Double getLongueur()                       { return longueur; }
    public void setLongueur(Double longueur)          { this.longueur = longueur; }

    public Integer getNbrCouche()                     { return nbrCouche; }
    public void setNbrCouche(Integer nbrCouche)       { this.nbrCouche = nbrCouche; }

    public Double getEstimatedCuttingTime()           { return estimatedCuttingTime; }
    public void setEstimatedCuttingTime(Double v)     { this.estimatedCuttingTime = v; }

    public LocalDateTime getEstimatedStartTime()      { return estimatedStartTime; }
    public void setEstimatedStartTime(LocalDateTime v){ this.estimatedStartTime = v; }

    public LocalDateTime getEstimatedEndTime()        { return estimatedEndTime; }
    public void setEstimatedEndTime(LocalDateTime v)  { this.estimatedEndTime = v; }

    public Long getQueueVersion()                     { return queueVersion; }
    public void setQueueVersion(Long v)               { this.queueVersion = v; }
}
