package com.battre.opssvc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "grpc.server.port=9020")
class OpsSvcApplicationTests {
  @Test
  void contextLoads() {}
}
