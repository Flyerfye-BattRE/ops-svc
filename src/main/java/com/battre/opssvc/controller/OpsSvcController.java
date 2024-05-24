package com.battre.opssvc.controller;

import com.battre.opssvc.enums.BatteryStatus;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.service.OpsSvc;
import com.battre.stubs.services.OpsSvcGrpc;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import com.battre.stubs.services.UpdateBatteriesStatusesRequest;
import com.battre.stubs.services.UpdateBatteriesStatusesResponse;
import com.battre.stubs.services.UpdateBatteryStatusRequest;
import com.battre.stubs.services.UpdateBatteryStatusResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

@GrpcService
public class OpsSvcController extends OpsSvcGrpc.OpsSvcImplBase {
    private static final Logger logger = Logger.getLogger(OpsSvcController.class.getName());

    private final OpsSvc opsSvc;

    @Autowired
    public OpsSvcController(OpsSvc opsSvc) {
        this.opsSvc = opsSvc;
    }

    @Override
    public void processIntakeBatteryOrder(ProcessIntakeBatteryOrderRequest request, StreamObserver<ProcessIntakeBatteryOrderResponse> responseObserver) {
        logger.info("processIntakeBatteryOrder() invoked");
        OrderRecordType savedOrderRecord = opsSvc.createNewOrderRecord(request);

        boolean storeBatteriesSuccess = opsSvc.attemptStoreBatteries(savedOrderRecord.getOrderId(), request.getBatteriesList());

        boolean addBatteriesToLabBacklogSuccess = false;
        if (storeBatteriesSuccess) {
            addBatteriesToLabBacklogSuccess = opsSvc.addBatteriesToLabBacklog(savedOrderRecord.getOrderId());
        }

        ProcessIntakeBatteryOrderResponse response = ProcessIntakeBatteryOrderResponse.newBuilder()
                .setSuccess(addBatteriesToLabBacklogSuccess)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("processIntakeBatteryOrder() finished");
    }

    @Override
    public void updateBatteryStatus(UpdateBatteryStatusRequest request, StreamObserver<UpdateBatteryStatusResponse> responseObserver) {
        int batteryId = request.getBattery().getBatteryId();
        String batteryStatus = request.getBattery().getBatteryStatus().toString();
        logger.info("updateBatteryStatus() invoked for [" + batteryId + "] and status [" + batteryStatus + "]");

        boolean updateBatteryStatusSuccess = false;
        switch (batteryStatus) {
            case "UNKNOWN":
            case "INTAKE":
            case "REJECTED":
            case "TESTING":
            case "REFURB":
            case "STORAGE":
            case "HOLD":
            case "SHIPPING":
            case "RECEIVED":
            case "DESTROYED":
            case "LOST":
                updateBatteryStatusSuccess = opsSvc.updateBatteryStatus(batteryId, BatteryStatus.valueOf(batteryStatus));
                break;
            default:
                logger.severe("This battery status [" + batteryStatus + "] is currently not implemented");
                break;
        }

        UpdateBatteryStatusResponse response = UpdateBatteryStatusResponse.newBuilder()
                .setSuccess(updateBatteryStatusSuccess)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("updateBatteryStatus() finished");
    }

    @Override
    public void updateBatteriesStatuses(UpdateBatteriesStatusesRequest request, StreamObserver<UpdateBatteriesStatusesResponse> responseObserver) {

    }
}