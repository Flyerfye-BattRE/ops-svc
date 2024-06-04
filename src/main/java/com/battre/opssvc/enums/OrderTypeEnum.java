package com.battre.opssvc.enums;

public enum OrderTypeEnum {
    UNKNOWN(0),
    INTAKE(1),
    OUTPUT(2),
    DEMO(3);

    private final int statusCode;

    OrderTypeEnum(int statusCode) {
        this.statusCode = statusCode;
    }

    public static OrderTypeEnum fromStatusCode(int statusCode) {
        for (OrderTypeEnum status : values()) {
            if (status.statusCode == statusCode) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
