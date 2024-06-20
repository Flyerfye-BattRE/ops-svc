package com.battre.opssvc.enums;

import com.battre.stubs.services.ProcessOrderStatus;

public enum ProcessOrderStatusEnum {
    LABSVC_BACKLOG_ERR(4, "LABSVC_BACKLOG_ERR", ProcessOrderStatus.LABSVC_BACKLOG_ERR),
    OPSSVC_CREATE_RECORD_ERR(2, "OPSSVC_CREATE_RECORD_ERR", ProcessOrderStatus.OPSSVC_CREATE_RECORD_ERR),
    STORAGESVC_STORE_BATTERIES_ERR(3, "STORAGESVC_STORE_BATTERIES_ERR", ProcessOrderStatus.STORAGESVC_STORE_BATTERIES_ERR),
    SUCCESS(1, "SUCCESS", ProcessOrderStatus.SUCCESS),
    UNKNOWN_ERR(0, "UNKNOWN_ERR", ProcessOrderStatus.UNKNOWN_ERR);

    private final int statusCode;
    private final String statusDescription;
    private final ProcessOrderStatus grpcStatus;

    ProcessOrderStatusEnum(int statusCode, String statusDescription, ProcessOrderStatus grpcStatus) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.grpcStatus = grpcStatus;
    }

    public static ProcessOrderStatusEnum fromStatusCode(int statusCode) {
        for (ProcessOrderStatusEnum status : values()) {
            if (status.statusCode == statusCode) {
                return status;
            }
        }
        return UNKNOWN_ERR;
    }

    public static ProcessOrderStatusEnum fromStatusDescription(String statusDescription) {
        for (ProcessOrderStatusEnum status : values()) {
            if (status.statusDescription.equals(statusDescription)) {
                return status;
            }
        }
        return UNKNOWN_ERR;
    }

    public static ProcessOrderStatusEnum fromGrpcStatus(ProcessOrderStatus grpcStatus) {
        for (ProcessOrderStatusEnum status : values()) {
            if (status.grpcStatus.equals(grpcStatus)) {
                return status;
            }
        }
        return UNKNOWN_ERR;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatusDescription() {
        return this.statusDescription;
    }

    public ProcessOrderStatus getgrpcStatus() {
        return this.grpcStatus;
    }
}
