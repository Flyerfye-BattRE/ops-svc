package com.battre.opssvc.enums;

import com.battre.stubs.services.BatteryStatus;

public enum BatteryStatusEnum {
    UNKNOWN(0, "UNKNOWN", BatteryStatus.UNKNOWN),
    INTAKE(1, "INTAKE", BatteryStatus.INTAKE),
    REJECTED(2, "REJECTED", BatteryStatus.REJECTED),
    TESTING(3, "TESTING", BatteryStatus.TESTING),
    REFURB(4, "REFURB", BatteryStatus.REFURB),
    STORAGE(5, "STORAGE", BatteryStatus.STORAGE),
    HOLD(6, "HOLD", BatteryStatus.HOLD),
    SHIPPING(7, "SHIPPING", BatteryStatus.SHIPPING),
    RECEIVED(8, "RECEIVED", BatteryStatus.RECEIVED),
    DESTROYED(9, "DESTROYED", BatteryStatus.DESTROYED),
    LOST(10, "LOST", BatteryStatus.LOST);

    private final int statusCode;
    private final String statusDescription;
    private final BatteryStatus grpcStatus;

    BatteryStatusEnum(int statusCode, String statusDescription, BatteryStatus grpcStatus) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.grpcStatus = grpcStatus;
    }

    public static BatteryStatusEnum fromStatusCode(int statusCode) {
        for (BatteryStatusEnum status : values()) {
            if (status.statusCode == statusCode) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public static BatteryStatusEnum fromStatusDescription(String statusDescription) {
        for (BatteryStatusEnum status : values()) {
            if (status.statusDescription.equals(statusDescription)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public static BatteryStatusEnum fromGrpcStatus(BatteryStatus grpcStatus) {
        for (BatteryStatusEnum status : values()) {
            if (status.grpcStatus.equals(grpcStatus)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatusDescription() {
        return this.statusDescription;
    }

    public BatteryStatus getGrpcStatus() {
        return this.grpcStatus;
    }
}
