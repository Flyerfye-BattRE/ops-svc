package com.battre.opssvc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.battre.opssvc.enums.BatteryStatusEnum;
import com.battre.opssvc.enums.ProcessOrderStatusEnum;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.CustomerDataType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.service.OpsSvc;
import com.battre.stubs.services.AddCustomerRequest;
import com.battre.stubs.services.AddCustomerResponse;
import com.battre.stubs.services.BatteryIdStatus;
import com.battre.stubs.services.BatteryStatus;
import com.battre.stubs.services.BatteryStatusCount;
import com.battre.stubs.services.Customer;
import com.battre.stubs.services.DestroyBatteryRequest;
import com.battre.stubs.services.DestroyBatteryResponse;
import com.battre.stubs.services.GetBatteryInventoryRequest;
import com.battre.stubs.services.GetBatteryInventoryResponse;
import com.battre.stubs.services.GetCustomerListRequest;
import com.battre.stubs.services.GetCustomerListResponse;
import com.battre.stubs.services.GetOpsSvcOverviewRequest;
import com.battre.stubs.services.GetOpsSvcOverviewResponse;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import com.battre.stubs.services.RemoveCustomerRequest;
import com.battre.stubs.services.RemoveCustomerResponse;
import com.battre.stubs.services.UpdateBatteryStatusRequest;
import com.battre.stubs.services.UpdateBatteryStatusResponse;
import com.battre.stubs.services.UpdateCustomerRequest;
import com.battre.stubs.services.UpdateCustomerResponse;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "grpc.server.port=9021")
public class OpsSvcControllerTests {
  @Mock private OpsSvc opsSvc;

  @Mock
  private StreamObserver<ProcessIntakeBatteryOrderResponse>
      responseProcessIntakeBatteryOrderResponse;
  @Mock
  private StreamObserver<UpdateBatteryStatusResponse> responseUpdateBatteryStatusResponse;
  @Mock
  private StreamObserver<GetBatteryInventoryResponse> responseGetBatteryInventoryResponse;
  @Mock
  private StreamObserver<DestroyBatteryResponse> responseDestroyBatteryResponse;
  @Mock
  private StreamObserver<GetCustomerListResponse> responseGetCustomerListResponse;
  @Mock
  private StreamObserver<AddCustomerResponse> responseAddCustomerResponse;
  @Mock
  private StreamObserver<RemoveCustomerResponse> responseRemoveCustomerResponse;
  @Mock
  private StreamObserver<UpdateCustomerResponse> responseUpdateCustomerResponse;
  @Mock
  private StreamObserver<GetOpsSvcOverviewResponse> responseGetOpsSvcOverviewResponse;

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
    UpdateBatteryStatusRequest request = UpdateBatteryStatusRequest.newBuilder()
            .setBatteryIdStatus(BatteryIdStatus.newBuilder().setBatteryId(1).setBatteryStatus(BatteryStatus.TESTING))
            .build();

    boolean updateSuccess = true;

    when(opsSvc.updateBatteryStatus(1, BatteryStatusEnum.TESTING)).thenReturn(updateSuccess);

    opsSvcController.updateBatteryStatus(request, responseUpdateBatteryStatusResponse);

    verify(opsSvc).updateBatteryStatus(1, BatteryStatusEnum.TESTING);
    verify(responseUpdateBatteryStatusResponse).onNext(UpdateBatteryStatusResponse.newBuilder().setSuccess(updateSuccess).build());
    verify(responseUpdateBatteryStatusResponse).onCompleted();
  }


  @Test
  void testGetCurrentBatteryInventory() {
    GetBatteryInventoryRequest request = GetBatteryInventoryRequest.newBuilder().build();

    List<BatteryInventoryType> batteryInventory = Collections.singletonList(new BatteryInventoryType());

    when(opsSvc.getCurrentBatteryInventory()).thenReturn(batteryInventory);

    opsSvcController.getCurrentBatteryInventory(request, responseGetBatteryInventoryResponse);

    verify(opsSvc).getCurrentBatteryInventory();
    verify(responseGetBatteryInventoryResponse).onNext(any(GetBatteryInventoryResponse.class));
    verify(responseGetBatteryInventoryResponse).onCompleted();
  }


  @Test
  void testGetBatteryInventory() {
    GetBatteryInventoryRequest request = GetBatteryInventoryRequest.newBuilder().build();

    List<BatteryInventoryType> batteryInventory = Collections.singletonList(new BatteryInventoryType());

    when(opsSvc.getBatteryInventory()).thenReturn(batteryInventory);

    opsSvcController.getBatteryInventory(request, responseGetBatteryInventoryResponse);

    verify(opsSvc).getBatteryInventory();
    verify(responseGetBatteryInventoryResponse).onNext(any(GetBatteryInventoryResponse.class));
    verify(responseGetBatteryInventoryResponse).onCompleted();
  }

  @Test
  void testDestroyBattery() {
    DestroyBatteryRequest request = DestroyBatteryRequest.newBuilder().setBatteryId(1).build();

    boolean destroySuccess = true;

    when(opsSvc.destroyBattery(1)).thenReturn(destroySuccess);

    opsSvcController.destroyBattery(request, responseDestroyBatteryResponse);

    verify(opsSvc).destroyBattery(1);
    verify(responseDestroyBatteryResponse).onNext(DestroyBatteryResponse.newBuilder().setSuccess(destroySuccess).build());
    verify(responseDestroyBatteryResponse).onCompleted();
  }


  @Test
  void testGetCustomerList() {
    GetCustomerListRequest request = GetCustomerListRequest.newBuilder().build();

    CustomerDataType customerData = new CustomerDataType(1,"John", "Doe", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID());

    List<CustomerDataType> customerList = Collections.singletonList(customerData);

    when(opsSvc.getCustomerList()).thenReturn(customerList);

    opsSvcController.getCustomerList(request, responseGetCustomerListResponse);

    verify(opsSvc).getCustomerList();
    verify(responseGetCustomerListResponse).onNext(any(GetCustomerListResponse.class));
    verify(responseGetCustomerListResponse).onCompleted();
  }


  @Test
  void testAddCustomer() {
    AddCustomerRequest request = AddCustomerRequest.newBuilder()
            .setCustomer(Customer.newBuilder().setLastName("Doe").setFirstName("John"))
            .build();

    boolean addSuccess = true;

    when(opsSvc.addCustomer(any())).thenReturn(addSuccess);

    opsSvcController.addCustomer(request, responseAddCustomerResponse);

    verify(opsSvc).addCustomer(any());
    verify(responseAddCustomerResponse).onNext(AddCustomerResponse.newBuilder().setSuccess(addSuccess).build());
    verify(responseAddCustomerResponse).onCompleted();
  }


  @Test
  void testRemoveCustomer() {
    RemoveCustomerRequest request = RemoveCustomerRequest.newBuilder().setCustomerId(1).build();

    boolean removeSuccess = true;

    when(opsSvc.removeCustomer(1)).thenReturn(removeSuccess);

    opsSvcController.removeCustomer(request, responseRemoveCustomerResponse);

    verify(opsSvc).removeCustomer(1);
    verify(responseRemoveCustomerResponse).onNext(RemoveCustomerResponse.newBuilder().setSuccess(removeSuccess).build());
    verify(responseRemoveCustomerResponse).onCompleted();
  }


  @Test
  void testUpdateCustomer() {
    UpdateCustomerRequest request = UpdateCustomerRequest.newBuilder()
            .setCustomer(Customer.newBuilder().setCustomerId(1).setLastName("Doe").setFirstName("Jane"))
            .build();

    boolean updateSuccess = true;

    when(opsSvc.updateCustomer(eq(1), any())).thenReturn(updateSuccess);

    opsSvcController.updateCustomer(request, responseUpdateCustomerResponse);

    verify(opsSvc).updateCustomer(eq(1), any());
    verify(responseUpdateCustomerResponse).onNext(UpdateCustomerResponse.newBuilder().setSuccess(updateSuccess).build());
    verify(responseUpdateCustomerResponse).onCompleted();
  }
  @Test
  void testGetOpsSvcOverview() {
    GetOpsSvcOverviewRequest request = GetOpsSvcOverviewRequest.newBuilder().build();

    int numCustomers = 10;
    BatteryStatusCount testBatteryStatusCount = BatteryStatusCount
            .newBuilder()
            .setBatteryStatus(BatteryStatus.TESTING)
            .setCount(3)
            .build();
    List<BatteryStatusCount> batteryStatusCounts = Collections.singletonList(testBatteryStatusCount);

    when(opsSvc.countCustomers()).thenReturn(numCustomers);
    when(opsSvc.getBatteryStatusCounts()).thenReturn(batteryStatusCounts);

    opsSvcController.getOpsSvcOverview(request, responseGetOpsSvcOverviewResponse);

    verify(opsSvc).countCustomers();
    verify(opsSvc).getBatteryStatusCounts();
    verify(responseGetOpsSvcOverviewResponse).onNext(any(GetOpsSvcOverviewResponse.class));
    verify(responseGetOpsSvcOverviewResponse).onCompleted();
  }
}


