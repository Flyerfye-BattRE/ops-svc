package com.battre.opssvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "BatteryInventory", schema = "OpsSvcSchema")
public class BatteryInventoryType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battery_id")
  private int batteryId;

  @Column(name = "battery_status_id", nullable = false)
  private int batteryStatusId;

  @Column(name = "battery_type_id", nullable = false)
  private int batteryTypeId;

  @Column(name = "intake_order_id", nullable = false)
  private int intakeOrderId;

  @Column(name = "hold_id")
  private Integer holdId;

  @Column(name = "output_order_id")
  private Integer outputOrderId;

  public BatteryInventoryType() {
    // Default constructor for Spring Data JPA
  }

  public BatteryInventoryType(int batteryStatusId, int batteryTypeId, int intakeOrderId) {
    this.batteryStatusId = batteryStatusId;
    this.batteryTypeId = batteryTypeId;
    this.intakeOrderId = intakeOrderId;
  }

  @Override
  public String toString() {
    return "BatteryInventoryType{"
        + "batteryId="
        + batteryId
        + ", batteryStatusId="
        + batteryStatusId
        + ", batteryTypeId="
        + batteryTypeId
        + ", intakeOrderId="
        + intakeOrderId
        + ", holdId="
        + holdId
        + ", outputOrderId="
        + outputOrderId
        + '}';
  }

  public int getBatteryId() {
    return batteryId;
  }

  // for testing purposes
  public void setBatteryId(int batteryId) {
    this.batteryId = batteryId;
  }

  public int getBatteryStatusId() {
    return batteryStatusId;
  }

  public void setBatteryStatusId(int batteryStatusId) {
    this.batteryStatusId = batteryStatusId;
  }

  public int getBatteryTypeId() {
    return batteryTypeId;
  }

  public void setBatteryTypeId(int batteryTypeId) {
    this.batteryTypeId = batteryTypeId;
  }

  public int getIntakeOrderId() {
    return intakeOrderId;
  }

  public void setIntakeOrderId(int intakeOrderId) {
    this.intakeOrderId = intakeOrderId;
  }

  public Integer getHoldId() {
    return holdId;
  }

  public void setHoldId(Integer holdId) {
    this.holdId = holdId;
  }

  public Integer getOutputOrderId() {
    return outputOrderId;
  }

  public void setOutputOrderId(Integer outputOrderId) {
    this.outputOrderId = outputOrderId;
  }
}
