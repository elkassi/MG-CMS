package com.lear.splice.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "calibration_log")
public class CalibrationLog {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "station_number")
    private String stationNumber;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "left_sensor_value")
    private String leftSensorValue;
    @Column(name = "right_sensor_value")
    private String rightSensorValue;
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd, HH:mm")
    private LocalDateTime updatedAt;
    @Column(name = "sync")
    private String sync;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStationNumber() {
        return stationNumber;
    }

    public void setStationNumber(String stationNumber) {
        this.stationNumber = stationNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLeftSensorValue() {
        return leftSensorValue;
    }

    public void setLeftSensorValue(String leftSensorValue) {
        this.leftSensorValue = leftSensorValue;
    }

    public String getRightSensorValue() {
        return rightSensorValue;
    }

    public void setRightSensorValue(String rightSensorValue) {
        this.rightSensorValue = rightSensorValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSync() {
        return sync;
    }

    public void setSync(String sync) {
        this.sync = sync;
    }
}