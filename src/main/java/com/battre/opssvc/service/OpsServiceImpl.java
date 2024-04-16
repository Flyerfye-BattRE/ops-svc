package com.battre.opssvc.service;

import com.battre.opssvc.model.OrderRecordType;
import com.battre.stubs.services.BatteryStorageInfo;
import com.battre.stubs.services.BatteryTypeTierCountRequest;
import com.battre.stubs.services.OpsSvcEmptyResponse;
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
    public void processIntakeBatteryOrder(BatteryTypeTierCountRequest incomingRequest, StreamObserver<OpsSvcEmptyResponse> outgoingResponse){
        logger.info("processIntakeBatteryOrder invoked");
        OrderRecordType savedOrderRecord = opsService.createNewOrderRecord(incomingRequest);

        List<BatteryStorageInfo> batteryStorageList =
                opsService.createNewBatteryStorageList(savedOrderRecord.getOrderId(), incomingRequest.getBatteriesList());

        boolean storeBatteriesSuccess = opsService.attemptStoreBatteries(savedOrderRecord.getOrderId(), batteryStorageList);

        if(storeBatteriesSuccess) {
            opsService.addBatteriesToLabBacklog(savedOrderRecord.getOrderId());
        }

        OpsSvcEmptyResponse myResponse = OpsSvcEmptyResponse.newBuilder()
                .build();

        outgoingResponse.onNext(myResponse);
        outgoingResponse.onCompleted();

        logger.info("processIntakeBatteryOrder finished");
    }
}