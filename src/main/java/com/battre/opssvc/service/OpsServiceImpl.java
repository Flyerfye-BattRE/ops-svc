package com.battre.opssvc.service;

import com.battre.opssvc.model.OrderRecordType;
import com.battre.stubs.services.BatteryStorageInfo;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import com.battre.stubs.services.OpsSvcGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.logging.Logger;

@GrpcService
public class OpsServiceImpl extends OpsSvcGrpc.OpsSvcImplBase {
    private static final Logger logger = Logger.getLogger(OpsServiceImpl.class.getName());

    private final OpsService opsService;

    @Autowired
    public OpsServiceImpl(OpsService opsService) {
        this.opsService = opsService;
    }

    @Override
    public void processIntakeBatteryOrder(ProcessIntakeBatteryOrderRequest request, StreamObserver<ProcessIntakeBatteryOrderResponse> responseObserver){
        logger.info("processIntakeBatteryOrder() invoked");
        OrderRecordType savedOrderRecord = opsService.createNewOrderRecord(request);

        List<BatteryStorageInfo> batteryStorageList =
                opsService.createNewBatteryStorageList(savedOrderRecord.getOrderId(), request.getBatteriesList());

        boolean storeBatteriesSuccess = opsService.attemptStoreBatteries(savedOrderRecord.getOrderId(), batteryStorageList);

        boolean addBatteriesToLabBacklogSuccess = false;
        if(storeBatteriesSuccess) {
            addBatteriesToLabBacklogSuccess = opsService.addBatteriesToLabBacklog(savedOrderRecord.getOrderId());
        }

        ProcessIntakeBatteryOrderResponse response = ProcessIntakeBatteryOrderResponse.newBuilder()
                .setSuccess(addBatteriesToLabBacklogSuccess)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("processIntakeBatteryOrder() finished");
    }
}