package com.battre.opssvc.service;

import com.battre.opssvc.model.OrderRecordType;
import com.battre.stubs.services.ProcessIntakeBatteryOrderRequest;
import com.battre.stubs.services.ProcessIntakeBatteryOrderResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpsSvcImplTests {
    @Mock
    private OpsSvc opsSvc;

    @Mock
    private StreamObserver<ProcessIntakeBatteryOrderResponse> responseProcessIntakeBatteryOrderResponse;

    private OpsSvcImpl opsSvcImpl;

    private AutoCloseable closeable;

    @BeforeEach
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        opsSvcImpl = new OpsSvcImpl(opsSvc);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    void testProcessIntakeBatteryOrder_StoreBatteryAddToLabBacklog_Success(){
        opsSvcImpl = new OpsSvcImpl(opsSvc);

        OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
        when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
        when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()),any())).thenReturn(true);
        when(opsSvc.addBatteriesToLabBacklog(orderRecord.getOrderId())).thenReturn(true);
        ProcessIntakeBatteryOrderRequest request = ProcessIntakeBatteryOrderRequest.newBuilder().build();

        opsSvcImpl.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

        verify(opsSvc).createNewOrderRecord(any());
        verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()),any());
        verify(opsSvc).addBatteriesToLabBacklog(orderRecord.getOrderId());
        verify(responseProcessIntakeBatteryOrderResponse).onNext(ProcessIntakeBatteryOrderResponse.newBuilder().setSuccess(true).build());
        verify(responseProcessIntakeBatteryOrderResponse).onCompleted();
    }

    @Test
    void testProcessIntakeBatteryOrder_StoreBattery_Failure(){
        opsSvcImpl = new OpsSvcImpl(opsSvc);

        OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
        when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
        when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()),any())).thenReturn(false);
        ProcessIntakeBatteryOrderRequest request = ProcessIntakeBatteryOrderRequest.newBuilder().build();

        opsSvcImpl.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

        verify(opsSvc).createNewOrderRecord(any());
        verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()),any());
        verify(responseProcessIntakeBatteryOrderResponse).onNext(ProcessIntakeBatteryOrderResponse.newBuilder().setSuccess(false).build());
        verify(responseProcessIntakeBatteryOrderResponse).onCompleted();
    }

    @Test
    void testProcessIntakeBatteryOrder_AddtoLabBacklog_Failure(){
        opsSvcImpl = new OpsSvcImpl(opsSvc);

        OrderRecordType orderRecord = new OrderRecordType(1, 2, 3, true, "");
        when(opsSvc.createNewOrderRecord(any())).thenReturn(orderRecord);
        when(opsSvc.attemptStoreBatteries(eq(orderRecord.getOrderId()),any())).thenReturn(true);
        when(opsSvc.addBatteriesToLabBacklog(orderRecord.getOrderId())).thenReturn(false);
        ProcessIntakeBatteryOrderRequest request = ProcessIntakeBatteryOrderRequest.newBuilder().build();

        opsSvcImpl.processIntakeBatteryOrder(request, responseProcessIntakeBatteryOrderResponse);

        verify(opsSvc).createNewOrderRecord(any());
        verify(opsSvc).attemptStoreBatteries(eq(orderRecord.getOrderId()),any());
        verify(opsSvc).addBatteriesToLabBacklog(orderRecord.getOrderId());
        verify(responseProcessIntakeBatteryOrderResponse).onNext(ProcessIntakeBatteryOrderResponse.newBuilder().setSuccess(false).build());
        verify(responseProcessIntakeBatteryOrderResponse).onCompleted();

    }
}

