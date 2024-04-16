package com.battre.opssvc.service;

import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.BatteryStorageInfo;
import com.battre.stubs.services.LabSvcGrpc;
import com.battre.stubs.services.StorageSvcGrpc;
import com.battre.stubs.services.StoreBatteryRequest;
import com.battre.stubs.services.StoreBatteryResponse;
import com.battre.stubs.services.BatteryIdType;
import com.battre.stubs.services.ProcessLabBatteriesRequest;
import com.battre.stubs.services.ProcessLabBatteriesResponse;
import com.battre.stubs.services.BatteryTypeTierCount;
import com.battre.stubs.services.BatteryTypeTierCountRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OpsService {
    public static enum batteryStatus{
        Intake,
        Rejected,
        Testing,
        Refurb,
        Storage,
        Hold,
        Shipping,
        Received,
        Destroyed,
        Lost
    }
    private static final Logger logger = Logger.getLogger(OpsService.class.getName());

    private final BatteryInventoryRepository batteryInventoryRepository;
    private final OrderRecordsRepository orderRecordsRepository;

    @GrpcClient("storageSvc")
    private StorageSvcGrpc.StorageSvcStub storageSvcClient;

    @GrpcClient("labSvc")
    private LabSvcGrpc.LabSvcStub labSvcClient;

    @Autowired
    public OpsService(BatteryInventoryRepository batteryInventoryRepository, OrderRecordsRepository orderRecordsRepository) {
        this.batteryInventoryRepository = batteryInventoryRepository;
        this.orderRecordsRepository = orderRecordsRepository;
    }

    public boolean attemptStoreBatteries(int orderId, List<BatteryStorageInfo> batteryStorageList) {
        StoreBatteryRequest.Builder StoreBatteryRequestBuilder = StoreBatteryRequest.newBuilder();
        StoreBatteryRequestBuilder.setOrderId(orderId);
        StoreBatteryRequestBuilder.addAllBatteries(batteryStorageList);

        CompletableFuture<StoreBatteryResponse> responseFuture = new CompletableFuture<>();
        // Create a StreamObserver to handle the call asynchronously
        StreamObserver<StoreBatteryResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(StoreBatteryResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                // Handle any errors
                logger.severe("tryStoreBatteries() errored: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("tryStoreBatteries() completed");
            }
        };

        storageSvcClient.tryStoreBatteries(StoreBatteryRequestBuilder.build(), responseObserver);

        boolean tryStoreBatteriesSuccess = false;
        try {
            // Blocks until the response is available
            tryStoreBatteriesSuccess = responseFuture.get(5, TimeUnit.SECONDS).getSuccess();
            logger.info("tryStoreBatteries responseFuture response: " + tryStoreBatteriesSuccess);
        } catch (Exception e) {
            logger.severe("tryStoreBatteries responseFuture error: " + e.getMessage());
        }

        // Order completed => True
        orderRecordsRepository.markOrderCompleted(orderId);

        // Store battery status => Storage / Rejected
        batteryInventoryRepository.setBatteryStatusesForOrder(
                orderId,
                tryStoreBatteriesSuccess ? batteryStatus.Storage.toString() : batteryStatus.Rejected.toString()
        );

        return tryStoreBatteriesSuccess;
    }

    public boolean addBatteriesToLabBacklog(int orderId) {
        List<Object[]> batteryIdTypeIdList = batteryInventoryRepository.getBatteryIdTypeIdsForOrder(orderId);
        List<BatteryIdType> processLabBatteriesList = convertToProcessLabBatteriesList(batteryIdTypeIdList);

        ProcessLabBatteriesRequest.Builder ProcessLabBatteriesRequestBuilder = ProcessLabBatteriesRequest.newBuilder();
        ProcessLabBatteriesRequestBuilder.addAllBatteryIdTypes(processLabBatteriesList);

        CompletableFuture<ProcessLabBatteriesResponse> responseFuture = new CompletableFuture<>();
        // Create a StreamObserver to handle the call asynchronously
        StreamObserver<ProcessLabBatteriesResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ProcessLabBatteriesResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                // Handle any errors
                logger.severe("addBatteriesToLabBacklog() errored: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("addBatteriesToLabBacklog() completed");
            }
        };

        labSvcClient.processLabBatteries(ProcessLabBatteriesRequestBuilder.build(), responseObserver);


        boolean addBatteriesToLabBacklogSuccess = false;
        try {
            // Blocks until the response is available
            addBatteriesToLabBacklogSuccess = responseFuture.get(5, TimeUnit.SECONDS).getSuccess();
            logger.info("addBatteriesToLabBacklog responseFuture response: " + addBatteriesToLabBacklogSuccess);
        } catch (Exception e) {
            logger.severe("addBatteriesToLabBacklog responseFuture error: " + e.getMessage());
        }

        return addBatteriesToLabBacklogSuccess;
    }

    public OrderRecordType createNewOrderRecord(BatteryTypeTierCountRequest request) {
        Random random = new Random();

        // Create new order entry
        int orderTypeId = 1; //Always 1 which corresponds to 'intake'
        int orderSectorId = random.nextInt(5) + 1; //Randomly select one of the available sectors
        int customerId = random.nextInt(2) + 1; //Randomly select one of the available customers
        boolean completed = false;
        String notes = "";

        // Concatenate the type/count info in notes
        for(BatteryTypeTierCount entry: request.getBatteriesList()) {
            notes = notes + "[" + entry.getBatteryType() + "]:x" + entry.getBatteryType() + ",";
        }

        OrderRecordType intakeOrderRecord = new OrderRecordType(
                orderTypeId,
                orderSectorId,
                customerId,
                completed,
                notes
        );

        OrderRecordType newOrderRecord = orderRecordsRepository.save(intakeOrderRecord);
        logger.info("After saving order record ["+newOrderRecord.getOrderId()+"], count: " +
                orderRecordsRepository.countOrderRecords());
        return newOrderRecord;
    }

    public List<BatteryStorageInfo> createNewBatteryStorageList(int orderId, List<BatteryTypeTierCount> batteryList) {
        List<BatteryStorageInfo> batteryStorageList = new ArrayList<>();

        // Create new batteries
        for(BatteryTypeTierCount batteryTypeTierCount:batteryList) {
            for(int i=0; i<batteryTypeTierCount.getBatteryCount(); i++){
                BatteryInventoryType newBattery = createNewBattery(orderId, batteryTypeTierCount.getBatteryType());
                batteryStorageList.add(
                        BatteryStorageInfo.newBuilder()
                                .setBatteryId(newBattery.getBatteryId())
                                .setBatteryTier(batteryTypeTierCount.getBatteryTier())
                                .build()
                );
            }
        }

        logger.info("After saving batteries ["+batteryList.size()+"], count: " + batteryInventoryRepository.countBatteryInventory());

        return batteryStorageList;
    }

    private BatteryInventoryType createNewBattery(int orderId, int typeId) {
        // Create new battery entry
        int batteryStatusId = 1; //Always 1 which corresponds to 'intake'
        int intakeOrderId = orderId;
        int batteryTypeId = typeId;

        BatteryInventoryType batteryInventory = new BatteryInventoryType(
                batteryStatusId,
                batteryTypeId,
                intakeOrderId
        );

        logger.info("Creating: " + batteryInventory);
        return batteryInventoryRepository.save(batteryInventory);
    }

    private List<BatteryIdType> convertToProcessLabBatteriesList(List<Object[]> batteryIdTypeIdList) {
        return batteryIdTypeIdList.stream()
                .map(batteryIdTypeId -> BatteryIdType.newBuilder()
                .setBatteryId((Integer) batteryIdTypeId[0])
                .setBatteryTypeId((Integer) batteryIdTypeId[1])
                .build())
                .collect(Collectors.toList());
    }
}