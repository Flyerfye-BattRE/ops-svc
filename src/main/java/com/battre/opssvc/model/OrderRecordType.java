package com.battre.opssvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "OrderRecords", schema = "OpsSvcSchema")
public class OrderRecordType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private int orderId;

  @Column(name = "order_date", nullable = false)
  private Timestamp orderDate;

  @Column(name = "order_type_id", nullable = false)
  private int orderTypeId;

  @Column(name = "order_sector_id", nullable = false)
  private int orderSectorId;

  @Column(name = "customer_id", nullable = false)
  private int customerId;

  @Column(name = "shipping_plan_id")
  private Integer shippingPlanId;

  @Column(name = "completed", columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean completed;

  @Column(name = "notes", length = 45)
  private String notes;

  public OrderRecordType() {
    // Default constructor for Spring Data JPA
  }

  public OrderRecordType(
      int orderTypeId, int orderSectorId, int customerId, boolean completed, String notes) {
    this.orderDate = Timestamp.from(Instant.now());
    this.orderTypeId = orderTypeId;
    this.orderSectorId = orderSectorId;
    this.customerId = customerId;
    this.completed = completed;
    this.notes = notes;
  }

  @Override
  public String toString() {
    return "OrderRecordType{"
        + "orderId="
        + orderId
        + ", orderDate="
        + orderDate
        + ", orderTypeId="
        + orderTypeId
        + ", orderSectorId="
        + orderSectorId
        + ", customerId="
        + customerId
        + ", shippingPlanId="
        + shippingPlanId
        + ", completed="
        + completed
        + ", notes='"
        + notes
        + '\''
        + '}';
  }

  public int getOrderId() {
    return orderId;
  }

  public Timestamp getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(Timestamp orderDate) {
    this.orderDate = orderDate;
  }

  public int getOrderTypeId() {
    return orderTypeId;
  }

  public void setOrderTypeId(int orderTypeId) {
    this.orderTypeId = orderTypeId;
  }

  public int getOrderSectorId() {
    return orderSectorId;
  }

  public void setOrderSectorId(int orderSectorId) {
    this.orderSectorId = orderSectorId;
  }

  public int getCustomerId() {
    return customerId;
  }

  public void setCustomerId(int customerId) {
    this.customerId = customerId;
  }

  public Integer getShippingPlanId() {
    return shippingPlanId;
  }

  public void setShippingPlanId(Integer shippingPlanId) {
    this.shippingPlanId = shippingPlanId;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
