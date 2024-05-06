package com.battre.opssvc.enums;

public enum BatteryStatus {
    UNKNOWN(0),
    INTAKE(1),
    REJECTED(2),
    TESTING(3),
    REFURB(4),
    STORAGE(5),
    HOLD(6),
    SHIPPING(7),
    RECEIVED(8),
    DESTROYED(9),
    LOST(10);

    private final int statusCode;

    BatteryStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
