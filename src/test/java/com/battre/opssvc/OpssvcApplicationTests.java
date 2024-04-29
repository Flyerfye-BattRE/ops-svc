package com.battre.opssvc;

import com.battre.stubs.services.LabSvcGrpc;
import com.battre.stubs.services.StorageSvcGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OpssvcApplicationTests {
    @MockBean
    private StorageSvcGrpc.StorageSvcStub storageSvcClient;

    @MockBean
    private LabSvcGrpc.LabSvcStub labSvcClient;

    @Test
    void contextLoads() {
    }

}
