package com.battre.opssvc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.battre.opssvc.controller.OpsSvcController;
import com.battre.opssvc.enums.ProcessOrderStatusEnum;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OpsSvcControllerTests {
  @Mock private OpsSvc opsSvc;

  @Mock
  private StreamObserver<ProcessIntakeBatteryOrderResponse>
      responseProcessIntakeBatteryOrderResponse;

  private OpsSvcController opsSvcController;

  private AutoCloseable closeable;

  @BeforeEach
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
    opsSvcController = new OpsSvcController(opsSvc);
  }

  @AfterEach
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  void testProcessIntakeBatteryOrder_StoreBatteryAddToLabBacklog_Success() {
    opsSvcController = new OpsSvcController(opsSvc);

    OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
    when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
    when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()), any())).thenReturn(true);
    when(opsSvc.addBatteriesToLabBacklog(orderRecord.getOrderId())).thenReturn(true);
    ProcessIntakeBatteryOrderRequest request =
        ProcessIntakeBatteryOrderRequest.newBuilder().build();

    opsSvcController.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

    verify(opsSvc).createNewOrderRecord(any());
    verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()), any());
    verify(opsSvc).addBatteriesToLabBacklog(orderRecord.getOrderId());
    verify(responseProcessIntakeBatteryOrderResponse)
        .onNext(
            ProcessIntakeBatteryOrderResponse.newBuilder()
                .setSuccess(true)
                .setStatus(ProcessOrderStatusEnum.SUCCESS.getgrpcStatus())
                .build());
    verify(responseProcessIntakeBatteryOrderResponse).onCompleted();
  }

  @Test
  void testProcessIntakeBatteryOrder_StoreBattery_Failure() {
    opsSvcController = new OpsSvcController(opsSvc);

    OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
    when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
    when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()), any())).thenReturn(false);
    ProcessIntakeBatteryOrderRequest request =
        ProcessIntakeBatteryOrderRequest.newBuilder().build();

    opsSvcController.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

    verify(opsSvc).createNewOrderRecord(any());
    verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()), any());
    verify(responseProcessIntakeBatteryOrderResponse)
        .onNext(
            ProcessIntakeBatteryOrderResponse.newBuilder()
                .setSuccess(false)
                .setStatus(ProcessOrderStatusEnum.STORAGESVC_STORE_BATTERIES_ERR.getgrpcStatus())
                .build());
    verify(responseProcessIntakeBatteryOrderResponse).onCompleted();
  }

  @Test
  void testProcessIntakeBatteryOrder_AddtoLabBacklog_Failure() {
    opsSvcController = new OpsSvcController(opsSvc);

    OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
    when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
    when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()), any())).thenReturn(true);
    when(opsSvc.addBatteriesToLabBacklog(orderRecord.getOrderId())).thenReturn(false);
    ProcessIntakeBatteryOrderRequest request =
        ProcessIntakeBatteryOrderRequest.newBuilder().build();

    opsSvcController.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

    verify(opsSvc).createNewOrderRecord(any());
    verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()), any());
    verify(opsSvc).addBatteriesToLabBacklog(orderRecord.getOrderId());
    verify(responseProcessIntakeBatteryOrderResponse)
        .onNext(
            ProcessIntakeBatteryOrderResponse.newBuilder()
                .setSuccess(false)
                .setStatus(ProcessOrderStatusEnum.LABSVC_BACKLOG_ERR.getgrpcStatus())
                .build());
    verify(responseProcessIntakeBatteryOrderResponse).onCompleted();
  }

  @Test
  void testUpdateBatteryStatus() {
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
  void testDestroyBattery() {
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
