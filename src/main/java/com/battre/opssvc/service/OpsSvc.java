package com.battre.opssvc.service;

import com.battre.grpcifc.GrpcMethodInvoker;
import com.battre.opssvc.enums.BatteryStatusEnum;
import com.battre.opssvc.enums.OrderTypeEnum;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.CustomerDataType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.BatteryStatusRepository;
import com.battre.opssvc.repository.CustomerDataRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.BatteryIdType;
import com.battre.stubs.services.BatteryStorageInfo;
import com.battre.stubs.services.BatteryTypeTierCount;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessLabBatteriesRequest;
import com.battre.stubs.services.ProcessLabBatteriesResponse;
import com.battre.stubs.services.RemoveLabBatteryRequest;
import com.battre.stubs.services.RemoveLabBatteryResponse;
import com.battre.stubs.services.RemoveStorageBatteryRequest;
import com.battre.stubs.services.RemoveStorageBatteryResponse;
import com.battre.stubs.services.StoreBatteryRequest;
import com.battre.stubs.services.StoreBatteryResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OpsSvc {
    private static final Logger logger = Logger.getLogger(OpsSvc.class.getName());
    private final BatteryInventoryRepository batInvRepo;
    private final BatteryStatusRepository batStatusRepo;
    private final CustomerDataRepository customerDataRepo;
    private final OrderRecordsRepository ordRecRepo;
    private final GrpcMethodInvoker grpcMethodInvoker;

    @Autowired
    public OpsSvc(BatteryInventoryRepository batInvRepo,
                  BatteryStatusRepository batStatusRepo,
                  CustomerDataRepository customerDataRepo,
                  OrderRecordsRepository ordRecRepo,
                  GrpcMethodInvoker grpcMethodInvoker
    ) {
        this.batInvRepo = batInvRepo;
        this.batStatusRepo = batStatusRepo;
        this.customerDataRepo = customerDataRepo;
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

        StoreBatteryResponse response = grpcMethodInvoker.invokeNonblock(
                "storagesvc",
                "tryStoreBatteries",
                request
        );

        boolean tryStoreBatteriesSuccess = response.getSuccess();
        if (tryStoreBatteriesSuccess) {
            // Order completed => True
            ordRecRepo.setOrderCompleted(orderId);

            // Store battery status => Storage
            batInvRepo.setBatteryStatusesForIntakeOrder(
                    orderId,
                    BatteryStatusEnum.STORAGE.toString()
            );
        } else {
            logger.severe("Order could not be marked as completed: " + orderId);
            tryStoreBatteriesSuccess = false;

            // Store battery status => Rejected
            batInvRepo.setBatteryStatusesForIntakeOrder(
                    orderId,
                    BatteryStatusEnum.REJECTED.toString()
            );
        }

        return tryStoreBatteriesSuccess;
    }

    public boolean addBatteriesToLabBacklog(int orderId) {
        List<Object[]> batteryIdTypeIdList = batInvRepo.getBatteryIdTypeIdsForIntakeOrder(orderId);
        List<BatteryIdType> processLabBatteriesList = convertToProcessLabBatteriesList(batteryIdTypeIdList);

        ProcessLabBatteriesRequest request = ProcessLabBatteriesRequest.newBuilder()
                .addAllBatteryIdTypes(processLabBatteriesList)
                .build();

        ProcessLabBatteriesResponse response = grpcMethodInvoker.invokeNonblock(
                "labsvc",
                "processLabBatteries",
                request
        );

        boolean addBatteriesToLabBacklogSuccess = response.getSuccess();

        if (addBatteriesToLabBacklogSuccess) {
            // Store battery status => Testing
            batInvRepo.setBatteryStatusesForIntakeOrder(
                    orderId,
                    BatteryStatusEnum.TESTING.toString()
            );
        } else {
            logger.severe("Order could not be marked as testing: " + orderId);
            addBatteriesToLabBacklogSuccess = false;
        }

        return addBatteriesToLabBacklogSuccess;
    }

    @Transactional
    public boolean updateBatteryStatus(int batteryId, BatteryStatusEnum batteryStatusEnum) {
        batInvRepo.setBatteryStatusesForIntakeOrder(batteryId, batteryStatusEnum.toString());

        return true;
    }

    public OrderRecordType createNewOrderRecord(ProcessIntakeBatteryOrderRequest request) {
        Random random = new Random();

        // Create new order entry
        int orderTypeId = OrderTypeEnum.INTAKE.getStatusCode();
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
                BatteryInventoryType newBattery = createNewBatteryEntry(orderId, batteryTypeTierCount.getBatteryType());
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

    private BatteryInventoryType createNewBatteryEntry(int orderId, int typeId) {
        // Create new battery entry
        int batteryStatusId = BatteryStatusEnum.INTAKE.getStatusCode();
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

    @Transactional
    public boolean destroyBattery(Integer batteryId) {
        // check if battery is present in inventory before deleting
        Optional<BatteryInventoryType> optionalBattery = batInvRepo.findById(batteryId);
        if (optionalBattery.isPresent() &&
                (
                        // if the battery is still physically in the factory
                        optionalBattery.get().getBatteryStatusId() == BatteryStatusEnum.INTAKE.getStatusCode() ||
                                optionalBattery.get().getBatteryStatusId() == BatteryStatusEnum.TESTING.getStatusCode() ||
                                optionalBattery.get().getBatteryStatusId() == BatteryStatusEnum.REFURB.getStatusCode() ||
                                optionalBattery.get().getBatteryStatusId() == BatteryStatusEnum.STORAGE.getStatusCode() ||
                                optionalBattery.get().getBatteryStatusId() == BatteryStatusEnum.HOLD.getStatusCode()
                )
        ) {
            BatteryInventoryType existingBattery = optionalBattery.get();
            int batId = existingBattery.getBatteryId();

            boolean labSvcSuccess = true;
            // if battery is being processed in the lab, remove it
            if (existingBattery.getBatteryStatusId() == BatteryStatusEnum.TESTING.getStatusCode() ||
                    existingBattery.getBatteryStatusId() == BatteryStatusEnum.REFURB.getStatusCode()) {
                //labSvc.removeBattery
                labSvcSuccess = removeBatteryFromLab(batId);
                if (labSvcSuccess) {
                    logger.info("Battery " + batId + " successfully removed from lab.");
                } else {
                    logger.severe("Battery " + batId + " NOT successfully removed from lab.");
                }
            }

            // remove battery from storage
            boolean storageSvcSuccess = removeBatteryFromStorage(batId);
            if (storageSvcSuccess) {
                logger.info("Battery " + batId + " successfully removed from storage.");
            } else {
                logger.severe("Battery " + batId + " NOT successfully removed from storage.");
            }

            // TODO: Check/Update ShippingSvc in the future, if necessary

            existingBattery.setHoldId(null);
            existingBattery.setBatteryStatusId(BatteryStatusEnum.DESTROYED.getStatusCode());
            batInvRepo.save(existingBattery);

            return storageSvcSuccess && labSvcSuccess;
        } else {
            return false;
        }
    }

    protected boolean removeBatteryFromStorage(int batteryId) {
        RemoveStorageBatteryRequest request = RemoveStorageBatteryRequest.newBuilder()
                .setBatteryId(batteryId)
                .build();

        RemoveStorageBatteryResponse response = grpcMethodInvoker.invokeNonblock(
                "storagesvc",
                "removeStorageBattery",
                request
        );

        return response.getSuccess();
    }

    protected boolean removeBatteryFromLab(int batteryId) {
        RemoveLabBatteryRequest request = RemoveLabBatteryRequest.newBuilder()
                .setBatteryId(batteryId)
                .build();

        RemoveLabBatteryResponse response = grpcMethodInvoker.invokeNonblock(
                "labsvc",
                "removeLabBattery",
                request
        );

        return response.getSuccess();
    }

    public List<BatteryInventoryType> getCurrentBatteryInventory() {
        return batInvRepo.getCurrentBatteryInventory();
    }

    public List<BatteryInventoryType> getBatteryInventory() {
        return batInvRepo.getBatteryInventory();
    }

    @Transactional
    public List<CustomerDataType> getCustomerList() {
        return customerDataRepo.getCustomerList();
    }

    @Transactional
    public boolean addCustomer(CustomerDataType customerData) {
        customerDataRepo.save(customerData);

        return true;
    }

    @Transactional
    public boolean removeCustomer(Integer customerId) {
        customerDataRepo.deleteById(customerId);

        return true;
    }

    @Transactional
    public boolean updateCustomer(Integer customerId, CustomerDataType newCustomerData) {
        Optional<CustomerDataType> optionalCustomer = customerDataRepo.findById(customerId);
        if (optionalCustomer.isPresent()) {
            customerDataRepo.save(newCustomerData);

            return true;
        } else {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }
    }
}