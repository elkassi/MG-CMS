package com.lear.MGCMS.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class MaintenanceIntervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String machine;
    private LocalDateTime date;
    private String userName;
    private String service;
    private String serviceType;
    private Integer time;
    private Double counter;
    private String comment;

    @Override
    public String toString() {
        return "MaintenanceIntervention{" +
                "id=" + id +
                ", machine='" + machine + '\'' +
                ", date=" + date +
                ", userName='" + userName + '\'' +
                ", service='" + service + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", counter=" + counter +
                '}';
    }

    public MaintenanceIntervention() {

    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Double getCounter() {
        return counter;
    }

    public void setCounter(Double counter) {
        this.counter = counter;
    }
}
