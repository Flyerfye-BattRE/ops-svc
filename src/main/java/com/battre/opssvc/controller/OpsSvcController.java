package com.battre.opssvc.controller;

import com.battre.opssvc.enums.BatteryStatusEnum;
import com.battre.opssvc.enums.ProcessOrderStatusEnum;
import com.battre.opssvc.model.BatteryInventoryType;
import com.battre.opssvc.model.CustomerDataType;
import com.battre.opssvc.model.OrderRecordType;
import com.battre.opssvc.service.OpsSvc;
import com.battre.stubs.services.AddCustomerRequest;
import com.battre.stubs.services.AddCustomerResponse;
import com.battre.stubs.services.Battery;
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
import com.battre.stubs.services.OpsSvcGrpc;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import com.battre.stubs.services.RemoveCustomerRequest;
import com.battre.stubs.services.RemoveCustomerResponse;
import com.battre.stubs.services.UpdateBatteriesStatusesRequest;
import com.battre.stubs.services.UpdateBatteriesStatusesResponse;
import com.battre.stubs.services.UpdateBatteryStatusRequest;
import com.battre.stubs.services.UpdateBatteryStatusResponse;
import com.battre.stubs.services.UpdateCustomerRequest;
import com.battre.stubs.services.UpdateCustomerResponse;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class OpsSvcController extends OpsSvcGrpc.OpsSvcImplBase {
  private static final Logger logger = Logger.getLogger(OpsSvcController.class.getName());

  private final OpsSvc opsSvc;

  @Autowired
  public OpsSvcController(OpsSvc opsSvc) {
    this.opsSvc = opsSvc;
  }

  @Override
  public void processIntakeBatteryOrder(
      ProcessIntakeBatteryOrderRequest request,
      StreamObserver<ProcessIntakeBatteryOrderResponse> responseObserver) {
    try {
      logger.info("processIntakeBatteryOrder() invoked");

      // Create new order record
      OrderRecordType savedOrderRecord = opsSvc.createNewOrderRecord(request);
      if (savedOrderRecord == null) {
        handleprocessIntakeBatteryOrderFailure(
            responseObserver, ProcessOrderStatusEnum.OPSSVC_CREATE_RECORD_ERR);
        return;
      }

      // Attempt to store batteries
      boolean storeBatteriesSuccess =
          opsSvc.attemptStoreBatteries(savedOrderRecord.getOrderId(), request.getBatteriesList());
      if (!storeBatteriesSuccess) {
        handleprocessIntakeBatteryOrderFailure(
            responseObserver, ProcessOrderStatusEnum.STORAGESVC_STORE_BATTERIES_ERR);
        return;
      }

      // Add batteries to lab backlog
      boolean addBatteriesToLabBacklogSuccess =
          opsSvc.addBatteriesToLabBacklog(savedOrderRecord.getOrderId());
      if (!addBatteriesToLabBacklogSuccess) {
        handleprocessIntakeBatteryOrderFailure(
            responseObserver, ProcessOrderStatusEnum.LABSVC_BACKLOG_ERR);
        return;
      }

      // If all steps succeed, return success response
      ProcessIntakeBatteryOrderResponse response =
          ProcessIntakeBatteryOrderResponse.newBuilder()
              .setSuccess(true)
              .setStatus(ProcessOrderStatusEnum.SUCCESS.getgrpcStatus())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("processIntakeBatteryOrder() finished");
    } catch (Exception e) {
      logger.severe("processIntakeBatteryOrder() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  private void handleprocessIntakeBatteryOrderFailure(
      StreamObserver<ProcessIntakeBatteryOrderResponse> responseObserver,
      ProcessOrderStatusEnum statusEnum) {
    logger.severe("OpsSvc failure: " + statusEnum.getgrpcStatus().toString());

    ProcessIntakeBatteryOrderResponse response =
        ProcessIntakeBatteryOrderResponse.newBuilder()
            .setSuccess(false)
            .setStatus(statusEnum.getgrpcStatus())
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateBatteryStatus(
      UpdateBatteryStatusRequest request,
      StreamObserver<UpdateBatteryStatusResponse> responseObserver) {
    int batteryId = request.getBattery().getBatteryId();
    String batteryStatus = request.getBattery().getBatteryStatus().toString();
    logger.info(
        "updateBatteryStatus() invoked for [" + batteryId + "] and status [" + batteryStatus + "]");

    try {
      boolean updateBatteryStatusSuccess = false;
      switch (batteryStatus) {
        case "UNKNOWN":
        case "INTAKE":
        case "REJECTED":
        case "TESTING":
        case "REFURB":
        case "STORAGE":
        case "HOLD":
        case "SHIPPING":
        case "RECEIVED":
        case "DESTROYED":
        case "LOST":
          updateBatteryStatusSuccess =
              opsSvc.updateBatteryStatus(batteryId, BatteryStatusEnum.valueOf(batteryStatus));
          break;
        default:
          logger.severe("This battery status [" + batteryStatus + "] is currently not implemented");
          break;
      }

      UpdateBatteryStatusResponse response =
          UpdateBatteryStatusResponse.newBuilder().setSuccess(updateBatteryStatusSuccess).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("updateBatteryStatus() finished");
    } catch (Exception e) {
      logger.severe("updateBatteryStatus() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void updateBatteriesStatuses(
      UpdateBatteriesStatusesRequest request,
      StreamObserver<UpdateBatteriesStatusesResponse> responseObserver) {
    // TODO: Implement bulk battery status update fn
  }

  @Override
  public void getCurrentBatteryInventory(
      GetBatteryInventoryRequest request,
      StreamObserver<GetBatteryInventoryResponse> responseObserver) {
    logger.info("getCurrentBatteryInventory() started");

    try {
      GetBatteryInventoryResponse response =
          buildGetBatteryInventoryResponse(opsSvc.getCurrentBatteryInventory());

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("getCurrentBatteryInventory() completed");
    } catch (Exception e) {
      logger.severe("getCurrentBatteryInventory() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void getBatteryInventory(
      GetBatteryInventoryRequest request,
      StreamObserver<GetBatteryInventoryResponse> responseObserver) {
    logger.info("getBatteryInventory() started");

    try {
      GetBatteryInventoryResponse response =
          buildGetBatteryInventoryResponse(opsSvc.getBatteryInventory());

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("getBatteryInventory() completed");
    } catch (Exception e) {
      logger.severe("getBatteryInventory() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  private GetBatteryInventoryResponse buildGetBatteryInventoryResponse(
      List<BatteryInventoryType> batteryInventory) {
    GetBatteryInventoryResponse.Builder responseBuilder = GetBatteryInventoryResponse.newBuilder();
    for (BatteryInventoryType battery : batteryInventory) {
      BatteryStatusEnum status = BatteryStatusEnum.fromStatusCode(battery.getBatteryStatusId());

      Battery.Builder batteryBuilder =
          Battery.newBuilder()
              .setBatteryId(battery.getBatteryId())
              .setBatteryStatus(status.getGrpcStatus())
              .setBatteryTypeId(battery.getBatteryTypeId())
              .setIntakeOrderId(battery.getIntakeOrderId());

      if (battery.getHoldId() != null) {
        batteryBuilder.setOptionalHoldId(Int32Value.of(battery.getHoldId()));
      }
      if (battery.getOutputOrderId() != null) {
        batteryBuilder.setOptionalOutputOrderId(Int32Value.of(battery.getOutputOrderId()));
      }

      responseBuilder.addBatteryList(batteryBuilder.build());
    }

    return responseBuilder.build();
  }

  @Override
  public void destroyBattery(
      DestroyBatteryRequest request, StreamObserver<DestroyBatteryResponse> responseObserver) {
    logger.info("destroyBattery() started");

    try {
      boolean success = opsSvc.destroyBattery(request.getBatteryId());

      DestroyBatteryResponse response =
          DestroyBatteryResponse.newBuilder().setSuccess(success).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("destroyBattery() completed");
    } catch (Exception e) {
      logger.severe("destroyBattery() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void getCustomerList(
      GetCustomerListRequest request, StreamObserver<GetCustomerListResponse> responseObserver) {
    logger.info("getCustomerList() started");

    try {
      List<CustomerDataType> customerList = opsSvc.getCustomerList();

      GetCustomerListResponse response = buildGetCustomerListResponse(customerList);

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("getCustomerList() completed");
    } catch (Exception e) {
      logger.severe("getCustomerList() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  private GetCustomerListResponse buildGetCustomerListResponse(
      List<CustomerDataType> customerList) {
    GetCustomerListResponse.Builder responseBuilder = GetCustomerListResponse.newBuilder();
    for (CustomerDataType customer : customerList) {
      Customer.Builder customerBuilder =
          Customer.newBuilder()
              .setCustomerId(customer.getCustomerId())
              .setLastName(customer.getLastName())
              .setFirstName(customer.getFirstName())
              .setEmail(customer.getEmail())
              .setPhone(customer.getPhone())
              .setAddress(customer.getAddress())
              .setLoyaltyId(customer.getLoyaltyId().toString());

      responseBuilder.addCustomerList(customerBuilder.build());
    }

    return responseBuilder.build();
  }

  // customerId ignored in request
  @Override
  public void addCustomer(
      AddCustomerRequest request, StreamObserver<AddCustomerResponse> responseObserver) {
    logger.info("addCustomer() started");

    try {
      Customer grpcCustomer = request.getCustomer();
      String newLoyaltyId = grpcCustomer.getLoyaltyId();
      CustomerDataType customer =
          new CustomerDataType(
              grpcCustomer.getLastName(),
              grpcCustomer.getFirstName(),
              grpcCustomer.getEmail(),
              grpcCustomer.getPhone(),
              grpcCustomer.getAddress(),
              newLoyaltyId.isEmpty() ? UUID.randomUUID() : UUID.fromString(newLoyaltyId));

      boolean success = opsSvc.addCustomer(customer);

      AddCustomerResponse response = AddCustomerResponse.newBuilder().setSuccess(success).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("addCustomer() completed");
    } catch (Exception e) {
      logger.severe("addCustomer() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void removeCustomer(
      RemoveCustomerRequest request, StreamObserver<RemoveCustomerResponse> responseObserver) {
    logger.info("removeCustomer() started");

    try {
      boolean success = opsSvc.removeCustomer(request.getCustomerId());

      RemoveCustomerResponse response =
          RemoveCustomerResponse.newBuilder().setSuccess(success).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("removeCustomer() completed");
    } catch (Exception e) {
      logger.severe("removeCustomer() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void updateCustomer(
      UpdateCustomerRequest request, StreamObserver<UpdateCustomerResponse> responseObserver) {
    logger.info("updateCustomer() started");

    try {
      Customer grpcCustomer = request.getCustomer();
      String newLoyaltyId = grpcCustomer.getLoyaltyId();
      CustomerDataType customer =
          new CustomerDataType(
              grpcCustomer.getCustomerId(),
              grpcCustomer.getLastName(),
              grpcCustomer.getFirstName(),
              grpcCustomer.getEmail(),
              grpcCustomer.getPhone(),
              grpcCustomer.getAddress(),
              newLoyaltyId.isEmpty() ? UUID.randomUUID() : UUID.fromString(newLoyaltyId));

      boolean success = opsSvc.updateCustomer(grpcCustomer.getCustomerId(), customer);

      UpdateCustomerResponse response =
          UpdateCustomerResponse.newBuilder().setSuccess(success).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      logger.info("updateCustomer() completed");
    } catch (Exception e) {
      logger.severe("updateCustomer() failed: " + e.getMessage());
      responseObserver.onError(e);
    }
  }

  @Override
  public void getOpsSvcOverview(
      GetOpsSvcOverviewRequest request,
      StreamObserver<GetOpsSvcOverviewResponse> responseObserver) {
    logger.info("getOpsSvcOverview() started");

    Integer numCustomers = opsSvc.countCustomers();
    List<BatteryStatusCount> batteryStatusCountsList = opsSvc.getBatteryStatusCounts();

    GetOpsSvcOverviewResponse.Builder responseBuilder = GetOpsSvcOverviewResponse.newBuilder();
    responseBuilder.setCustomerCount(numCustomers);

    for (BatteryStatusCount batteryStatusCount : batteryStatusCountsList) {
      BatteryStatusCount.Builder batteryStatusCountBuilder =
          BatteryStatusCount.newBuilder()
              .setBatteryStatus(batteryStatusCount.getBatteryStatus())
              .setCount(batteryStatusCount.getCount());

      responseBuilder.addBatteryStatusCountList(batteryStatusCountBuilder.build());
    }

    GetOpsSvcOverviewResponse response = responseBuilder.build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();

    logger.info("getOpsSvcOverview() completed");
  }
}
