package com.battre.opssvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.battre.grpcifc.GrpcMethodInvoker;
import com.battre.grpcifc.GrpcTestMethodInvoker;
import com.battre.opssvc.controller.OpsSvcController;
import com.battre.opssvc.enums.BatteryStatusEnum;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.CustomerDataType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.repository.BatteryInventoryRepository;
import com.battre.opssvc.repository.BatteryStatusRepository;
import com.battre.opssvc.repository.CustomerDataRepository;
import com.battre.opssvc.repository.OrderRecordsRepository;
import com.battre.stubs.services.AddCustomerRequest;
import com.battre.stubs.services.AddCustomerResponse;
import com.battre.stubs.services.BatteryIdStatus;
import com.battre.stubs.services.BatteryStatus;
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
import com.battre.stubs.services.ProcessLabBatteriesRequest;
import com.battre.stubs.services.ProcessLabBatteriesResponse;
import com.battre.stubs.services.RemoveCustomerRequest;
import com.battre.stubs.services.RemoveCustomerResponse;
import com.battre.stubs.services.RemoveLabBatteryRequest;
import com.battre.stubs.services.RemoveLabBatteryResponse;
import com.battre.stubs.services.RemoveStorageBatteryRequest;
import com.battre.stubs.services.RemoveStorageBatteryResponse;
import com.battre.stubs.services.StoreBatteryRequest;
import com.battre.stubs.services.StoreBatteryResponse;
import com.battre.stubs.services.UpdateBatteryStatusRequest;
import com.battre.stubs.services.UpdateBatteryStatusResponse;
import com.battre.stubs.services.UpdateCustomerRequest;
import com.battre.stubs.services.UpdateCustomerResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "grpc.server.port=9023")
@ExtendWith(MockitoExtension.class)
public class OpsSvcIntegrationTests {
  private static final Logger logger = Logger.getLogger(OpsSvcIntegrationTests.class.getName());

  @MockBean private BatteryInventoryRepository batteryInventoryRepository;
  @MockBean private BatteryStatusRepository batteryStatusRepository;
  @MockBean private CustomerDataRepository customerDataRepository;
  @MockBean private OrderRecordsRepository orderRecordsRepository;
  @MockBean private GrpcMethodInvoker grpcMethodInvoker;
  @Autowired private OpsSvcController opsSvcController;
  private final GrpcTestMethodInvoker grpcTestMethodInvoker = new GrpcTestMethodInvoker();

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testProcessIntakeBatteryOrder_Success() throws NoSuchMethodException {
    // Test create new order record
    when(orderRecordsRepository.save(any(OrderRecordType.class))).thenReturn(new OrderRecordType());

    // Test attempt to store batteries
    StoreBatteryResponse tryStoreBatteriesResponse =
        StoreBatteryResponse.newBuilder().setSuccess(true).build();
    when(grpcMethodInvoker.invokeNonblock(
            eq("storagesvc"), eq("tryStoreBatteries"), any(StoreBatteryRequest.class)))
        .thenReturn(tryStoreBatteriesResponse);
    doNothing().when(orderRecordsRepository).setOrderCompleted(anyInt());
    doNothing()
        .when(batteryInventoryRepository)
        .setBatteryStatusesForIntakeOrder(anyInt(), eq(BatteryStatusEnum.STORAGE.toString()));

    // Test add batteries to lab backlog
    when(batteryInventoryRepository.getBatteryIdTypeIdsForIntakeOrder(anyInt()))
        .thenReturn(List.of());
    ProcessLabBatteriesResponse tryProcessLabBatteriesResponse =
        ProcessLabBatteriesResponse.newBuilder().setSuccess(true).build();
    when(grpcMethodInvoker.invokeNonblock(
            eq("labsvc"), eq("processLabBatteries"), any(ProcessLabBatteriesRequest.class)))
        .thenReturn(tryProcessLabBatteriesResponse);
    doNothing()
        .when(batteryInventoryRepository)
        .setBatteryStatusesForIntakeOrder(anyInt(), eq(BatteryStatusEnum.TESTING.toString()));

    // Request
    ProcessIntakeBatteryOrderRequest request =
        ProcessIntakeBatteryOrderRequest.newBuilder().build();
    ProcessIntakeBatteryOrderResponse response =
        grpcTestMethodInvoker.invokeNonblock(
            opsSvcController, "processIntakeBatteryOrder", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(orderRecordsRepository).save(any(OrderRecordType.class));
    verify(orderRecordsRepository).countOrderRecords();
    verify(orderRecordsRepository).setOrderCompleted(anyInt());
    verify(batteryInventoryRepository)
        .setBatteryStatusesForIntakeOrder(anyInt(), eq(BatteryStatusEnum.STORAGE.toString()));
    verify(batteryInventoryRepository)
        .setBatteryStatusesForIntakeOrder(anyInt(), eq(BatteryStatusEnum.TESTING.toString()));
    verify(grpcMethodInvoker)
        .invokeNonblock(eq("storagesvc"), eq("tryStoreBatteries"), any(StoreBatteryRequest.class));
    verify(grpcMethodInvoker)
        .invokeNonblock(
            eq("labsvc"), eq("processLabBatteries"), any(ProcessLabBatteriesRequest.class));
  }

  @Test
  public void testUpdateBatteryStatus_Success() throws NoSuchMethodException {
    // Test
    doNothing()
        .when(batteryInventoryRepository)
        .setBatteryStatusForBatteryId(anyInt(), eq(BatteryStatus.INTAKE.toString()));

    // Request
    BatteryIdStatus batteryIdStatus =
        BatteryIdStatus.newBuilder().setBatteryStatus(BatteryStatus.INTAKE).build();
    UpdateBatteryStatusRequest request =
        UpdateBatteryStatusRequest.newBuilder().setBatteryIdStatus(batteryIdStatus).build();
    UpdateBatteryStatusResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "updateBatteryStatus", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(batteryInventoryRepository)
        .setBatteryStatusForBatteryId(anyInt(), eq(BatteryStatus.INTAKE.toString()));
  }

  @Test
  public void testGetCurrentBatteryInventory_Success() throws NoSuchMethodException {
    // Test
    when(batteryInventoryRepository.getCurrentBatteryInventory())
        .thenReturn(List.of(new BatteryInventoryType()));

    // Request
    GetBatteryInventoryRequest request = GetBatteryInventoryRequest.newBuilder().build();
    GetBatteryInventoryResponse response =
        grpcTestMethodInvoker.invokeNonblock(
            opsSvcController, "getCurrentBatteryInventory", request);
    assertEquals(response.getBatteryListCount(), 1);

    // Verify
    verify(batteryInventoryRepository).getCurrentBatteryInventory();
  }

  @Test
  public void testGetBatteryInventory_Success() throws NoSuchMethodException {
    // Test
    when(batteryInventoryRepository.getBatteryInventory())
        .thenReturn(List.of(new BatteryInventoryType()));

    // Request
    GetBatteryInventoryRequest request = GetBatteryInventoryRequest.newBuilder().build();
    GetBatteryInventoryResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "getBatteryInventory", request);
    assertEquals(response.getBatteryListCount(), 1);

    // Verify
    verify(batteryInventoryRepository).getBatteryInventory();
  }

  @Test
  public void testDestroyBattery_Success() throws NoSuchMethodException {
    // Test
    Optional<BatteryInventoryType> testBattery =
        Optional.of(new BatteryInventoryType(BatteryStatusEnum.TESTING.getStatusCode(), 1, 1));
    when(batteryInventoryRepository.findById(anyInt())).thenReturn(testBattery);

    RemoveLabBatteryResponse tryRemoveLabBatteryResponse =
        RemoveLabBatteryResponse.newBuilder().setSuccess(true).build();
    when(grpcMethodInvoker.invokeNonblock(
            eq("labsvc"), eq("removeLabBattery"), any(RemoveLabBatteryRequest.class)))
        .thenReturn(tryRemoveLabBatteryResponse);

    RemoveStorageBatteryResponse tryRemoveStorageBatteryResponse =
        RemoveStorageBatteryResponse.newBuilder().setSuccess(true).build();
    when(grpcMethodInvoker.invokeNonblock(
            eq("storagesvc"), eq("removeStorageBattery"), any(RemoveStorageBatteryRequest.class)))
        .thenReturn(tryRemoveStorageBatteryResponse);

    // Request
    DestroyBatteryRequest request = DestroyBatteryRequest.newBuilder().setBatteryId(0).build();
    DestroyBatteryResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "destroyBattery", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(batteryInventoryRepository).findById(anyInt());
    verify(grpcMethodInvoker)
        .invokeNonblock(eq("labsvc"), eq("removeLabBattery"), any(RemoveLabBatteryRequest.class));
    verify(grpcMethodInvoker)
        .invokeNonblock(
            eq("storagesvc"), eq("removeStorageBattery"), any(RemoveStorageBatteryRequest.class));
    verify(batteryInventoryRepository).save(any(BatteryInventoryType.class));
  }

  @Test
  public void testGetCustomerList_Success() throws NoSuchMethodException {
    // Test
    CustomerDataType customerData =
        new CustomerDataType(
            1, "John", "Doe", "e@mail.com", "555-555-5555", "555 North St", UUID.randomUUID());
    when(customerDataRepository.getCustomerList()).thenReturn(List.of(customerData));

    // Request
    GetCustomerListRequest request = GetCustomerListRequest.newBuilder().build();
    GetCustomerListResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "getCustomerList", request);
    assertEquals(response.getCustomerListCount(), 1);

    // Verify
    verify(customerDataRepository).getCustomerList();
  }

  @Test
  public void testAddCustomer_Success() throws NoSuchMethodException {
    // Test
    when(customerDataRepository.save(any(CustomerDataType.class)))
        .thenReturn(new CustomerDataType());

    // Request
    AddCustomerRequest request = AddCustomerRequest.newBuilder().build();
    AddCustomerResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "addCustomer", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(customerDataRepository).save(any(CustomerDataType.class));
  }

  @Test
  public void testRemoveCustomer_Success() throws NoSuchMethodException {
    // Test
    doNothing().when(customerDataRepository).deleteById(eq(1));

    // Request
    RemoveCustomerRequest request = RemoveCustomerRequest.newBuilder().setCustomerId(1).build();
    RemoveCustomerResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "removeCustomer", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(customerDataRepository).deleteById(eq(1));
  }

  @Test
  public void testUpdateCustomer_Success() throws NoSuchMethodException {
    // Test
    when(customerDataRepository.findById(eq(1))).thenReturn(Optional.of(new CustomerDataType()));
    when(customerDataRepository.save(any(CustomerDataType.class)))
        .thenReturn(new CustomerDataType());

    // Request
    UpdateCustomerRequest request =
        UpdateCustomerRequest.newBuilder()
            .setCustomer(Customer.newBuilder().setCustomerId(1).build())
            .build();
    UpdateCustomerResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "updateCustomer", request);
    assertTrue(response.getSuccess());

    // Verify
    verify(customerDataRepository).findById(eq(1));
    verify(customerDataRepository).save(any(CustomerDataType.class));
  }

  @Test
  public void testGetOpsSvcOverview_Success() throws NoSuchMethodException {
    // Test
    when(customerDataRepository.countCustomers()).thenReturn(5);
    when(batteryInventoryRepository.getBatteryStatusCounts())
        .thenReturn(List.of(new Object[] {"1", 10L}, new Object[] {"2", 20L}));

    // Request
    GetOpsSvcOverviewRequest request = GetOpsSvcOverviewRequest.newBuilder().build();
    GetOpsSvcOverviewResponse response =
        grpcTestMethodInvoker.invokeNonblock(opsSvcController, "getOpsSvcOverview", request);
    assertEquals(response.getCustomerCount(), 5);
    assertEquals(response.getBatteryStatusCountListCount(), 2);

    // Verify
    verify(customerDataRepository).countCustomers();
    verify(batteryInventoryRepository).getBatteryStatusCounts();
  }
}
