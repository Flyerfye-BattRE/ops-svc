package com.battre.opssvc.repository;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class BatteryStatusRepositoryTests {
  private static final Logger logger =
      Logger.getLogger(BatteryStatusRepositoryTests.class.getName());

  @Autowired private BatteryStatusRepository batStatusRepo;

  @Test
  public void testGetBatteryStatusTypes() {
    // TODO: Implement test
  }
}
