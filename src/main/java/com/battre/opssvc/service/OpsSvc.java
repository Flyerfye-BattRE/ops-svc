package com.battre.opssvc.service;

import com.battre.grpcifc.GrpcMethodInvoker;
import com.battre.opssvc.enums.BatteryStatus;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.BatteryIdType;
import com.battre.stubs.services.BatteryStorageInfo;
import com.battre.stubs.services.BatteryTypeTierCount;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessLabBatteriesRequest;
import com.battre.stubs.services.ProcessLabBatteriesResponse;
import com.battre.stubs.services.StoreBatteryRequest;
import com.battre.stubs.services.StoreBatteryResponse;
import io.grpc.stub.StreamObserver;
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
public class OpsSvc {
    private static final Logger logger = Logger.getLogger(OpsSvc.class.getName());
    private final BatteryInventoryRepository batInvRepo;
    private final OrderRecordsRepository ordRecRepo;
    private final GrpcMethodInvoker grpcMethodInvoker;

    @Autowired
    public OpsSvc(BatteryInventoryRepository batInvRepo,
                  OrderRecordsRepository ordRecRepo,
                  GrpcMethodInvoker grpcMethodInvoker
    ) {
        this.batInvRepo = batInvRepo;
        this.ordRecRepo = ordRecRepo;
        this.grpcMethodInvoker = grpcMethodInvoker;
    }

    public static List<BatteryIdType> convertToProcessLabBatteriesList(List<Object[]> batteryIdTypeIdList) {
        return batteryIdTypeIdList.stream()
                .map(batteryIdTypeId -> BatteryIdType.newBuilder()
                        .setBatteryId((Integer) batteryIdTypeId[0])
                        .setBatteryTypeId((Integer) batteryIdTypeId[1])
                        .build())
                .collect(Collectors.toList());
    }

    public boolean attemptStoreBatteries(int orderId, List<BatteryTypeTierCount> batteryList) {
        //creates the battery entries in the battery inventory table and assembles a list for storage service
        List<BatteryStorageInfo> batteryStorageList = createNewBatteryStorageList(orderId, batteryList);

        StoreBatteryRequest request = StoreBatteryRequest.newBuilder()
                .setOrderId(orderId)
                .addAllBatteries(batteryStorageList)
                .build();

        CompletableFuture<StoreBatteryResponse> responseFuture = new CompletableFuture<>();
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

        grpcMethodInvoker.callMethod(
                "storagesvc",
                "tryStoreBatteries",
                request,
                responseObserver
        );

        boolean tryStoreBatteriesSuccess = false;
        try {
            // Blocks until the response is available
            tryStoreBatteriesSuccess = responseFuture.get(5, TimeUnit.SECONDS).getSuccess();
            logger.info("tryStoreBatteries() responseFuture response: " + tryStoreBatteriesSuccess);
        } catch (Exception e) {
            logger.severe("tryStoreBatteries() responseFuture error: " + e.getMessage());
        }

        // Order completed => True
        ordRecRepo.setOrderCompleted(orderId);

        // Store battery status => Storage / Rejected
        batInvRepo.setBatteryStatusesForIntakeOrder(
                orderId,
                tryStoreBatteriesSuccess ? BatteryStatus.STORAGE.toString() : BatteryStatus.REJECTED.toString()
        );

        return tryStoreBatteriesSuccess;
    }

    public boolean addBatteriesToLabBacklog(int orderId) {
        List<Object[]> batteryIdTypeIdList = batInvRepo.getBatteryIdTypeIdsForIntakeOrder(orderId);
        List<BatteryIdType> processLabBatteriesList = convertToProcessLabBatteriesList(batteryIdTypeIdList);

        ProcessLabBatteriesRequest request = ProcessLabBatteriesRequest.newBuilder()
                .addAllBatteryIdTypes(processLabBatteriesList)
                .build();

        CompletableFuture<ProcessLabBatteriesResponse> responseFuture = new CompletableFuture<>();
        StreamObserver<ProcessLabBatteriesResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ProcessLabBatteriesResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                // Handle any errors
                logger.severe("processLabBatteries() errored: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("processLabBatteries() completed");
            }
        };

        grpcMethodInvoker.callMethod(
                "labsvc",
                "processLabBatteries",
                request,
                responseObserver
        );

        boolean addBatteriesToLabBacklogSuccess = false;
        try {
            // Blocks until the response is available
            addBatteriesToLabBacklogSuccess = responseFuture.get(5, TimeUnit.SECONDS).getSuccess();
            logger.info("addBatteriesToLabBacklog() responseFuture response: " + addBatteriesToLabBacklogSuccess);
        } catch (Exception e) {
            logger.severe("addBatteriesToLabBacklog() responseFuture error: " + e.getMessage());
        }

        return addBatteriesToLabBacklogSuccess;
    }

    public boolean updateBatteryStatus(int batteryId, BatteryStatus batteryStatus) {
        batInvRepo.setBatteryStatusesForIntakeOrder(batteryId, batteryStatus.toString());
        return true;
    }

    public OrderRecordType createNewOrderRecord(ProcessIntakeBatteryOrderRequest request) {
        Random random = new Random();

        // Create new order entry
        int orderTypeId = 1; //Always 1 which corresponds to 'intake'
        int orderSectorId = random.nextInt(5) + 1; //Randomly select one of the available sectors
        int customerId = random.nextInt(2) + 1; //Randomly select one of the available customers
        boolean completed = false;
        String notes = "";

        // Concatenate the type/count info in notes
        for (BatteryTypeTierCount entry : request.getBatteriesList()) {
            notes = notes + "[" + entry.getBatteryType() + "]:x" + entry.getBatteryCount() + ",";
        }

        OrderRecordType intakeOrderRecord = new OrderRecordType(
                orderTypeId,
                orderSectorId,
                customerId,
                completed,
                notes
        );

        OrderRecordType newOrderRecord = ordRecRepo.save(intakeOrderRecord);
        logger.info("After saving order record [" + newOrderRecord.getOrderId() + "], count: " +
                ordRecRepo.countOrderRecords());
        return newOrderRecord;
    }

    private List<BatteryStorageInfo> createNewBatteryStorageList(int orderId, List<BatteryTypeTierCount> batteryList) {
        List<BatteryStorageInfo> batteryStorageList = new ArrayList<>();

        // Create new batteries
        for (BatteryTypeTierCount batteryTypeTierCount : batteryList) {
            for (int i = 0; i < batteryTypeTierCount.getBatteryCount(); i++) {
                BatteryInventoryType newBattery = createNewBattery(orderId, batteryTypeTierCount.getBatteryType());
                batteryStorageList.add(
                        BatteryStorageInfo.newBuilder()
                                .setBatteryId(newBattery.getBatteryId())
                                .setBatteryTier(batteryTypeTierCount.getBatteryTier())
                                .build()
                );
            }
        }

        logger.info("After saving batteries [" + batteryList.size() + "], count: " + batInvRepo.countBatteryInventory());

        return batteryStorageList;
    }

    public BatteryInventoryType createNewBattery(int orderId, int typeId) {
        // Create new battery entry
        int batteryStatusId = 1; //Always 1 which corresponds to 'intake'
        int intakeOrderId = orderId;
        int batteryTypeId = typeId;

        BatteryInventoryType batteryInventoryEntry = new BatteryInventoryType(
                batteryStatusId,
                batteryTypeId,
                intakeOrderId
        );

        logger.info("Creating: " + batteryInventoryEntry);
        return batInvRepo.save(batteryInventoryEntry);
    }
}