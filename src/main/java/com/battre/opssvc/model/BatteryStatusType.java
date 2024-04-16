package com.battre.opssvc.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "BatteryStatus", schema = "OpsSvcDb")
public class BatteryStatusType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battery_status_id", nullable = false)
    private int batteryStatusId;

    @Column(name = "status", nullable = false)
    private String status;

    public int getBatteryStatusId() {
        return batteryStatusId;
    }

    public void setBatteryStatusId(int batteryStatusId) {
        this.batteryStatusId = batteryStatusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}