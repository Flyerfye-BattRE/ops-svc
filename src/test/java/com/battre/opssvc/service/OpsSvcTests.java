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
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.BatteryStatusRepository;
import com.battre.opssvc.repository.CustomerDataRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.BatteryIdType;
import com.battre.stubs.services.BatteryTypeTierCount;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessLabBatteriesRequest;
import com.battre.stubs.services.ProcessLabBatteriesResponse;
import com.battre.stubs.services.StoreBatteryRequest;
import com.battre.stubs.services.StoreBatteryResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    // TODO: Implement test
  }

  @Test
  void testDestroyBattery() {
    // TODO: Implement test
  }

  @Test
  void testGetCurrentBatteryInventory() {
    // TODO: Implement test
  }

  @Test
  void testGetBatteryInventory() {
    // TODO: Implement test
  }

  @Test
  void testGetCustomerList() {
    // TODO: Implement test
  }

  @Test
  void testAddCustomer() {
    // TODO: Implement test
  }

  @Test
  void testRemoveCustomer() {
    // TODO: Implement test
  }

  @Test
  void testUpdateCustomer() {
    // TODO: Implement test
  }
}
