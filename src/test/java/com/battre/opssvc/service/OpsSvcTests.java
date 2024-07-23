package com.battre.opssvc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.battre.grpcifc.GrpcMethodInvoker;
import com.battre.opssvc.enums.BatteryStatusEnum;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.CustomerDataType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.BatteryStatusRepository;
import com.battre.opssvc.repository.CustomerDataRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.BatteryIdType;
import com.battre.stubs.services.BatteryStatusCount;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "grpc.server.port=9022")
class OpsSvcTests {
  @Mock private BatteryInventoryRepository batInvRepo;
  @Mock private BatteryStatusRepository batStatusRepo;
  @Mock private CustomerDataRepository customerDataRepo;
  @Mock private OrderRecordsRepository ordRecRepo;
  @Mock private GrpcMethodInvoker grpcMethodInvoker;

  @InjectMocks private OpsSvc opsSvc;

  private AutoCloseable closeable;

  public void mockTryStoreBatteries(StoreBatteryResponse response) {
    when(grpcMethodInvoker.invokeNonblock(
            eq("storagesvc"), eq("tryStoreBatteries"), any(StoreBatteryRequest.class)))
        .thenReturn(response);
  }

  public void mockRemoveBatteryFromStorage(RemoveStorageBatteryResponse response) {
    when(grpcMethodInvoker.invokeNonblock(
            eq("storagesvc"), eq("removeStorageBattery"), any(RemoveStorageBatteryRequest.class)))
        .thenReturn(response);
  }

  public void mockRemoveBatteryFromLab(RemoveLabBatteryResponse response) {
    when(grpcMethodInvoker.invokeNonblock(
            eq("labsvc"), eq("removeLabBattery"), any(RemoveLabBatteryRequest.class)))
            .thenReturn(response);
  }

  public void mockProcessLabBatteries(ProcessLabBatteriesResponse response) {
    when(grpcMethodInvoker.invokeNonblock(
            eq("labsvc"), eq("processLabBatteries"), any(ProcessLabBatteriesRequest.class)))
            .thenReturn(response);
  }

  @BeforeEach
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
    opsSvc = new OpsSvc(batInvRepo, batStatusRepo, customerDataRepo, ordRecRepo, grpcMethodInvoker);
  }

  @AfterEach
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void testCreateNewOrderRecord() {
    ProcessIntakeBatteryOrderRequest request =
        ProcessIntakeBatteryOrderRequest.newBuilder()
            .addBatteries(
                BatteryTypeTierCount.newBuilder().setBatteryType(3).setBatteryCount(2).build())
            .build();

    OrderRecordType savedOrderRecord = new OrderRecordType(1, 3, 2, false, "test notes");
    doReturn(savedOrderRecord).when(ordRecRepo).save(any(OrderRecordType.class));

    OrderRecordType result = opsSvc.createNewOrderRecord(request);
    // validate that a non-empty result was received
    assertNotNull(result);

    // check the contents of the order record that was actually generated for saving
    ArgumentCaptor<OrderRecordType> captor = ArgumentCaptor.forClass(OrderRecordType.class);
    verify(ordRecRepo).save(captor.capture());
    assertEquals(1, captor.getValue().getOrderTypeId());
    assertFalse(captor.getValue().isCompleted());
    assertEquals(captor.getValue().getNotes(), "[3]:x2,");
  }

  @Test
  public void testAttemptStoreBatteries_Success() {
    int orderId = 3;
    int batteryTypeId = 2;
    int batteryStatusId = 1;
    List<BatteryTypeTierCount> batteryList =
        List.of(
            BatteryTypeTierCount.newBuilder()
                .setBatteryType(batteryTypeId)
                .setBatteryTier(2)
                .setBatteryCount(2)
                .build());

    BatteryInventoryType returnedBattery =
        new BatteryInventoryType(batteryStatusId, batteryTypeId, orderId);
    doReturn(returnedBattery).when(batInvRepo).save(any(BatteryInventoryType.class));
    doReturn(1).when(batInvRepo).countBatteryInventory();

    StoreBatteryResponse successResponse =
        StoreBatteryResponse.newBuilder().setSuccess(true).build();
    mockTryStoreBatteries(successResponse);

    boolean result = opsSvc.attemptStoreBatteries(orderId, batteryList);

    assertTrue(result);
    verify(ordRecRepo).setOrderCompleted(orderId);
    verify(batInvRepo).setBatteryStatusesForIntakeOrder(orderId, "STORAGE");

    // check the contents of battery inventory entry that was actually generated for saving
    ArgumentCaptor<BatteryInventoryType> captor =
        ArgumentCaptor.forClass(BatteryInventoryType.class);
    verify(batInvRepo, times(2)).save(captor.capture());
    assertEquals(batteryStatusId, captor.getValue().getBatteryStatusId());
    assertEquals(batteryTypeId, captor.getValue().getBatteryTypeId());
    assertEquals(orderId, captor.getValue().getIntakeOrderId());
  }

  @Test
  public void testAttemptStoreBatteries_Failure() {
    int orderId = 4;
    int batteryTypeId = 5;
    int batteryStatusId = 1;
    List<BatteryTypeTierCount> batteryList =
        List.of(
            BatteryTypeTierCount.newBuilder()
                .setBatteryType(batteryTypeId)
                .setBatteryTier(2)
                .setBatteryCount(1)
                .build());

    BatteryInventoryType returnedBattery =
        new BatteryInventoryType(batteryStatusId, batteryTypeId, orderId);
    doReturn(returnedBattery).when(batInvRepo).save(any(BatteryInventoryType.class));
    doReturn(1).when(batInvRepo).countBatteryInventory();

    StoreBatteryResponse failureResponse =
        StoreBatteryResponse.newBuilder().setSuccess(false).build();
    mockTryStoreBatteries(failureResponse);

    boolean result = opsSvc.attemptStoreBatteries(orderId, batteryList);

    assertFalse(result);
    verify(batInvRepo).setBatteryStatusesForIntakeOrder(orderId, "REJECTED");

    // check the contents of battery inventory entry that was actually generated for saving
    ArgumentCaptor<BatteryInventoryType> captor =
        ArgumentCaptor.forClass(BatteryInventoryType.class);
    verify(batInvRepo, times(1)).save(captor.capture());
    assertEquals(batteryStatusId, captor.getValue().getBatteryStatusId());
    assertEquals(batteryTypeId, captor.getValue().getBatteryTypeId());
    assertEquals(orderId, captor.getValue().getIntakeOrderId());
  }

  @Test
  void testAddBatteriesToLabBacklog_Success() {
    int orderId = 1;
    List<Object[]> batteryIdTypeIdList = List.of(new Object[] {1, 2}, new Object[] {2, 4});

    List<BatteryIdType> processLabBatteriesList =
        List.of(
            BatteryIdType.newBuilder().setBatteryId(1).setBatteryTypeId(2).build(),
            BatteryIdType.newBuilder().setBatteryId(2).setBatteryTypeId(4).build());

    doReturn(batteryIdTypeIdList).when(batInvRepo).getBatteryIdTypeIdsForIntakeOrder(orderId);

    ProcessLabBatteriesResponse successResponse =
        ProcessLabBatteriesResponse.newBuilder().setSuccess(true).build();
    mockProcessLabBatteries(successResponse);

    boolean result = opsSvc.addBatteriesToLabBacklog(orderId);

    assertTrue(result);
    // verify the processLabBatteries grpc call
    ArgumentCaptor<ProcessLabBatteriesRequest> captor =
        ArgumentCaptor.forClass(ProcessLabBatteriesRequest.class);
    verify(grpcMethodInvoker)
        .invokeNonblock(eq("labsvc"), eq("processLabBatteries"), captor.capture());
    assertEquals(processLabBatteriesList, captor.getValue().getBatteryIdTypesList());
  }

  @Test
  void testAddBatteriesToLabBacklog_Failure() {
    int orderId = 2;
    List<Object[]> batteryIdTypeIdList = List.of(new Object[] {3, 5}, new Object[] {4, 6});

    List<BatteryIdType> processLabBatteriesList =
        List.of(
            BatteryIdType.newBuilder().setBatteryId(3).setBatteryTypeId(5).build(),
            BatteryIdType.newBuilder().setBatteryId(4).setBatteryTypeId(6).build());

    doReturn(batteryIdTypeIdList).when(batInvRepo).getBatteryIdTypeIdsForIntakeOrder(orderId);

    ProcessLabBatteriesResponse failResponse =
        ProcessLabBatteriesResponse.newBuilder().setSuccess(false).build();
    mockProcessLabBatteries(failResponse);

    boolean result = opsSvc.addBatteriesToLabBacklog(orderId);

    assertFalse(result);
    // verify the processLabBatteries grpc call
    ArgumentCaptor<ProcessLabBatteriesRequest> captor =
        ArgumentCaptor.forClass(ProcessLabBatteriesRequest.class);
    verify(grpcMethodInvoker)
        .invokeNonblock(eq("labsvc"), eq("processLabBatteries"), captor.capture());
    assertEquals(processLabBatteriesList, captor.getValue().getBatteryIdTypesList());
  }

  @Test
  void testUpdateBatteryStatus() {
    int batteryId = 1;
    BatteryStatusEnum newStatus = BatteryStatusEnum.TESTING;

    boolean result = opsSvc.updateBatteryStatus(batteryId, newStatus);

    assertTrue(result);
    verify(batInvRepo).setBatteryStatusForBatteryId(batteryId, newStatus.toString());
  }

  @Test
  void testDestroyBattery() {
    int batteryId = 1;
    BatteryInventoryType mockBattery = new BatteryInventoryType();
    mockBattery.setBatteryStatusId(BatteryStatusEnum.TESTING.getStatusCode());
    mockBattery.setBatteryId(batteryId);

    Optional<BatteryInventoryType> optionalBattery = Optional.of(mockBattery);
    doReturn(optionalBattery).when(batInvRepo).findById(batteryId);

    RemoveStorageBatteryResponse successStorageResponse =
            RemoveStorageBatteryResponse.newBuilder().setSuccess(true).build();
    mockRemoveBatteryFromStorage(successStorageResponse);

    RemoveLabBatteryResponse successLabResponse =
            RemoveLabBatteryResponse.newBuilder().setSuccess(true).build();
    mockRemoveBatteryFromLab(successLabResponse);

    boolean result = opsSvc.destroyBattery(batteryId);

    assertTrue(result);

    // verify the removeStorageBattery grpc call
    ArgumentCaptor<RemoveStorageBatteryRequest> storageCaptor =
            ArgumentCaptor.forClass(RemoveStorageBatteryRequest.class);
    verify(grpcMethodInvoker)
            .invokeNonblock(eq("storagesvc"), eq("removeStorageBattery"), storageCaptor.capture());
    assertEquals(batteryId, storageCaptor.getValue().getBatteryId());

    // verify the removeLabBattery grpc call
    ArgumentCaptor<RemoveLabBatteryRequest> labCaptor =
            ArgumentCaptor.forClass(RemoveLabBatteryRequest.class);
    verify(grpcMethodInvoker)
            .invokeNonblock(eq("labsvc"), eq("removeLabBattery"), labCaptor.capture());
    assertEquals(batteryId, labCaptor.getValue().getBatteryId());
  }

  @Test
  void testGetCurrentBatteryInventory() {
    List<BatteryInventoryType> mockInventory = Arrays.asList(
            new BatteryInventoryType(1, 1, 1),
            new BatteryInventoryType(2, 2, 2)
    );
    doReturn(mockInventory).when(batInvRepo).getCurrentBatteryInventory();

    List<BatteryInventoryType> result = opsSvc.getCurrentBatteryInventory();

    assertNotNull(result);
    assertEquals(mockInventory.size(), result.size());
    verify(batInvRepo).getCurrentBatteryInventory();
  }

  @Test
  void testGetBatteryInventory() {
    List<BatteryInventoryType> mockInventory = Arrays.asList(
            new BatteryInventoryType(1, 1, 1),
            new BatteryInventoryType(2, 2, 2)
    );
    doReturn(mockInventory).when(batInvRepo).getBatteryInventory();

    List<BatteryInventoryType> result = opsSvc.getBatteryInventory();

    assertNotNull(result);
    assertEquals(mockInventory.size(), result.size());
    verify(batInvRepo).getBatteryInventory();
  }

  @Test
  void testGetBatteryStatusCounts() {
    List<Object[]> mockCounts = Arrays.asList(
            new Object[] {BatteryStatusEnum.STORAGE.toString(), 10L},
            new Object[] {BatteryStatusEnum.TESTING.toString(), 5L}
    );
    doReturn(mockCounts).when(batInvRepo).getBatteryStatusCounts();

    List<BatteryStatusCount> result = opsSvc.getBatteryStatusCounts();

    assertNotNull(result);
    assertEquals(mockCounts.size(), result.size());

    assertEquals(BatteryStatusEnum.STORAGE.getGrpcStatus(), result.get(0).getBatteryStatus());
    assertEquals(10, result.get(0).getCount());
    assertEquals(BatteryStatusEnum.TESTING.getGrpcStatus(), result.get(1).getBatteryStatus());
    assertEquals(5, result.get(1).getCount());

    verify(batInvRepo).getBatteryStatusCounts();
  }


  @Test
  void testCountCustomers() {
    int mockCount = 5;
    doReturn(mockCount).when(customerDataRepo).countCustomers();

    Integer result = opsSvc.countCustomers();

    assertNotNull(result);
    assertEquals(mockCount, result.intValue());

    verify(customerDataRepo).countCustomers();
  }


  @Test
  void testGetCustomerList() {
    List<CustomerDataType> mockCustomers = Arrays.asList(
            new CustomerDataType("John", "Doe", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID()),
            new CustomerDataType("Jane", "Smith", "other.e@mail.com", "444-444-4444", "444 North St", UUID.randomUUID())
    );
    doReturn(mockCustomers).when(customerDataRepo).getCustomerList();

    List<CustomerDataType> result = opsSvc.getCustomerList();

    assertNotNull(result);
    assertEquals(mockCustomers.size(), result.size());

    verify(customerDataRepo).getCustomerList();
  }


  @Test
  void testAddCustomer() {
    CustomerDataType newCustomer = new CustomerDataType("John", "Doe", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID());

    boolean result = opsSvc.addCustomer(newCustomer);

    assertTrue(result);
    verify(customerDataRepo).save(newCustomer);
  }

  @Test
  void testRemoveCustomer() {
    int customerId = 1;

    boolean result = opsSvc.removeCustomer(customerId);

    assertTrue(result);
    verify(customerDataRepo).deleteById(customerId);
  }


  @Test
  void testUpdateCustomer() {
    int customerId = 1;
    CustomerDataType updatedCustomer = new CustomerDataType("John", "Doe", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID());

    doReturn(Optional.of(new CustomerDataType("Johnny", "Doey", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID()))).when(customerDataRepo).findById(customerId);

    boolean result = opsSvc.updateCustomer(customerId, updatedCustomer);

    assertTrue(result);
    verify(customerDataRepo).findById(customerId);
    verify(customerDataRepo).save(updatedCustomer);
  }

}
