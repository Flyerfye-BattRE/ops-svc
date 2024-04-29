package com.battre.opssvc.service;

import com.battre.opssvc.model.OrderRecordType;
import com.battre.stubs.services.OpsSvcGrpc;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

@GrpcService
public class OpsSvcImpl extends OpsSvcGrpc.OpsSvcImplBase {
    private static final Logger logger = Logger.getLogger(OpsSvcImpl.class.getName());

    private final OpsSvc opsSvc;

    @Autowired
    public OpsSvcImpl(OpsSvc opsSvc) {
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
}