package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PartNumberValidatedWeight")
public class PartNumberValidatedWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partnumber;

    private Double validatedWeight;

    private String validatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validatedAt;

    private String comment;

    public PartNumberValidatedWeight() {}

    public PartNumberValidatedWeight(String partnumber, Double validatedWeight, String validatedBy) {
        this.partnumber = partnumber;
        this.validatedWeight = validatedWeight;
        this.validatedBy = validatedBy;
        this.validatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.validatedAt == null) {
            this.validatedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPartnumber() { return partnumber; }
    public void setPartnumber(String partnumber) { this.partnumber = partnumber; }

    public Double getValidatedWeight() { return validatedWeight; }
    public void setValidatedWeight(Double validatedWeight) { this.validatedWeight = validatedWeight; }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
