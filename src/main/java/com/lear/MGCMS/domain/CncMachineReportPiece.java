package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CncMachineReportPiece")
public class CncMachineReportPiece {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CncMachineReport report;

    private String programNumber;
    private String status;
    private String qualityStatus;
    private String codeDefaut;
    private String codeScrap;
    private String qualityComment;
    private String operatorUsername;
    private Integer imageCount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long sourcePieceId;

    public CncMachineReportPiece() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CncMachineReport getReport() { return report; }
    public void setReport(CncMachineReport report) { this.report = report; }

    public String getProgramNumber() { return programNumber; }
    public void setProgramNumber(String programNumber) { this.programNumber = programNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

    public String getCodeDefaut() { return codeDefaut; }
    public void setCodeDefaut(String codeDefaut) { this.codeDefaut = codeDefaut; }

    public String getCodeScrap() { return codeScrap; }
    public void setCodeScrap(String codeScrap) { this.codeScrap = codeScrap; }

    public String getQualityComment() { return qualityComment; }
    public void setQualityComment(String qualityComment) { this.qualityComment = qualityComment; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public Integer getImageCount() { return imageCount; }
    public void setImageCount(Integer imageCount) { this.imageCount = imageCount; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Long getSourcePieceId() { return sourcePieceId; }
    public void setSourcePieceId(Long sourcePieceId) { this.sourcePieceId = sourcePieceId; }
}
