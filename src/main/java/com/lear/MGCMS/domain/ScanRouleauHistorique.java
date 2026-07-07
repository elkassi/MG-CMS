package com.lear.MGCMS.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Scan_RouleauHistorique")
public class ScanRouleauHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    private String serialId;

    // Constructors
    public ScanRouleauHistorique() {}

    public ScanRouleauHistorique(String content, LocalDateTime date, String serialId) {
        this.content = content;
        this.date = date;
        this.serialId = serialId;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    @Override
    public String toString() {
        return "ScanRouleauHistorique{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", date=" + date +
                ", serialId='" + serialId + '\'' +
                '}';
    }
}
