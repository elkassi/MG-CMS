package com.lear.MGCMS.payload.scheduling;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for sequence loading
 */
public class SequenceLoadResponse {

    private String sequenceId;
    private String modele;
    private String dueDate;
    private String dueShift;
    private Integer boxCount;
    private Integer totalQty;
    private String status; // FINISHED, IN_PROGRESS, NOT_STARTED
    private LocalDateTime minStartDate;
    private LocalDateTime maxEndDate;
    private Double durationHours;
    private List<SerieInfo> series;

    public static class SerieInfo {
        private String serieId;
        private String placement;
        private String partNumberMaterial;
        private String machine;
        private String statusMatelassage;
        private String statusCoupe;
        private Double cuttingTimeMinutes;
        private String tableMatelassage;
        private String tableCoupe;
        private LocalDateTime dateDebutMatelassage;
        private LocalDateTime dateFinMatelassage;
        private LocalDateTime dateDebutCoupe;
        private LocalDateTime dateFinCoupe;
        private Boolean isLocked;

        // Getters and Setters
        public String getSerieId() {
            return serieId;
        }

        public void setSerieId(String serieId) {
            this.serieId = serieId;
        }

        public String getPlacement() {
            return placement;
        }

        public void setPlacement(String placement) {
            this.placement = placement;
        }

        public String getPartNumberMaterial() {
            return partNumberMaterial;
        }

        public void setPartNumberMaterial(String partNumberMaterial) {
            this.partNumberMaterial = partNumberMaterial;
        }

        public String getMachine() {
            return machine;
        }

        public void setMachine(String machine) {
            this.machine = machine;
        }

        public String getStatusMatelassage() {
            return statusMatelassage;
        }

        public void setStatusMatelassage(String statusMatelassage) {
            this.statusMatelassage = statusMatelassage;
        }

        public String getStatusCoupe() {
            return statusCoupe;
        }

        public void setStatusCoupe(String statusCoupe) {
            this.statusCoupe = statusCoupe;
        }

        public Double getCuttingTimeMinutes() {
            return cuttingTimeMinutes;
        }

        public void setCuttingTimeMinutes(Double cuttingTimeMinutes) {
            this.cuttingTimeMinutes = cuttingTimeMinutes;
        }

        public String getTableMatelassage() {
            return tableMatelassage;
        }

        public void setTableMatelassage(String tableMatelassage) {
            this.tableMatelassage = tableMatelassage;
        }

        public String getTableCoupe() {
            return tableCoupe;
        }

        public void setTableCoupe(String tableCoupe) {
            this.tableCoupe = tableCoupe;
        }

        public LocalDateTime getDateDebutMatelassage() {
            return dateDebutMatelassage;
        }

        public void setDateDebutMatelassage(LocalDateTime dateDebutMatelassage) {
            this.dateDebutMatelassage = dateDebutMatelassage;
        }

        public LocalDateTime getDateFinMatelassage() {
            return dateFinMatelassage;
        }

        public void setDateFinMatelassage(LocalDateTime dateFinMatelassage) {
            this.dateFinMatelassage = dateFinMatelassage;
        }

        public LocalDateTime getDateDebutCoupe() {
            return dateDebutCoupe;
        }

        public void setDateDebutCoupe(LocalDateTime dateDebutCoupe) {
            this.dateDebutCoupe = dateDebutCoupe;
        }

        public LocalDateTime getDateFinCoupe() {
            return dateFinCoupe;
        }

        public void setDateFinCoupe(LocalDateTime dateFinCoupe) {
            this.dateFinCoupe = dateFinCoupe;
        }

        public Boolean getIsLocked() {
            return isLocked;
        }

        public void setIsLocked(Boolean isLocked) {
            this.isLocked = isLocked;
        }
    }

    // Getters and Setters
    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDueShift() {
        return dueShift;
    }

    public void setDueShift(String dueShift) {
        this.dueShift = dueShift;
    }

    public Integer getBoxCount() {
        return boxCount;
    }

    public void setBoxCount(Integer boxCount) {
        this.boxCount = boxCount;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getMinStartDate() {
        return minStartDate;
    }

    public void setMinStartDate(LocalDateTime minStartDate) {
        this.minStartDate = minStartDate;
    }

    public LocalDateTime getMaxEndDate() {
        return maxEndDate;
    }

    public void setMaxEndDate(LocalDateTime maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

    public Double getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Double durationHours) {
        this.durationHours = durationHours;
    }

    public List<SerieInfo> getSeries() {
        return series;
    }

    public void setSeries(List<SerieInfo> series) {
        this.series = series;
    }
}
