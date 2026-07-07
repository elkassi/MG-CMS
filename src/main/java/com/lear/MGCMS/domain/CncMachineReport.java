package com.lear.MGCMS.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CncMachineReport")
public class CncMachineReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String machineName;

    @Column(nullable = false)
    private String boxId;

    private String programNumber;
    private String partNumber;
    private String operator;
    private String productionOperator;
    private String productionStatus;

    private Integer quantiteImp;
    private Integer totalPieces;
    private Integer okPieces;
    private Integer defautPieces;
    private Integer scrapPieces;

    private Integer shiftNumber;
    private String shiftDate;

    private LocalDateTime startProductionDate;
    private LocalDateTime endProductionDate;
    private LocalDateTime sessionCreatedAt;

    private LocalDateTime importedAt;
    private String importedBy;

    // Source tracking
    private Long sourceSessionId;

    public CncMachineReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }

    public String getBoxId() { return boxId; }
    public void setBoxId(String boxId) { this.boxId = boxId; }

    public String getProgramNumber() { return programNumber; }
    public void setProgramNumber(String programNumber) { this.programNumber = programNumber; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getProductionOperator() { return productionOperator; }
    public void setProductionOperator(String productionOperator) { this.productionOperator = productionOperator; }

    public String getProductionStatus() { return productionStatus; }
    public void setProductionStatus(String productionStatus) { this.productionStatus = productionStatus; }

    public Integer getQuantiteImp() { return quantiteImp; }
    public void setQuantiteImp(Integer quantiteImp) { this.quantiteImp = quantiteImp; }

    public Integer getTotalPieces() { return totalPieces; }
    public void setTotalPieces(Integer totalPieces) { this.totalPieces = totalPieces; }

    public Integer getOkPieces() { return okPieces; }
    public void setOkPieces(Integer okPieces) { this.okPieces = okPieces; }

    public Integer getDefautPieces() { return defautPieces; }
    public void setDefautPieces(Integer defautPieces) { this.defautPieces = defautPieces; }

    public Integer getScrapPieces() { return scrapPieces; }
    public void setScrapPieces(Integer scrapPieces) { this.scrapPieces = scrapPieces; }

    public Integer getShiftNumber() { return shiftNumber; }
    public void setShiftNumber(Integer shiftNumber) { this.shiftNumber = shiftNumber; }

    public String getShiftDate() { return shiftDate; }
    public void setShiftDate(String shiftDate) { this.shiftDate = shiftDate; }

    public LocalDateTime getStartProductionDate() { return startProductionDate; }
    public void setStartProductionDate(LocalDateTime startProductionDate) { this.startProductionDate = startProductionDate; }

    public LocalDateTime getEndProductionDate() { return endProductionDate; }
    public void setEndProductionDate(LocalDateTime endProductionDate) { this.endProductionDate = endProductionDate; }

    public LocalDateTime getSessionCreatedAt() { return sessionCreatedAt; }
    public void setSessionCreatedAt(LocalDateTime sessionCreatedAt) { this.sessionCreatedAt = sessionCreatedAt; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }

    public Long getSourceSessionId() { return sourceSessionId; }
    public void setSourceSessionId(Long sourceSessionId) { this.sourceSessionId = sourceSessionId; }
}
