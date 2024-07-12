package com.battre.opssvc.repository;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class CustomerDataRepositoryTests {
  private static final Logger logger = Logger.getLogger(CustomerDataRepositoryTests.class.getName());

  @Autowired private CustomerDataRepository custDataRepo;

  @Test
  public void testGetCustomerList() {
    // TODO: Implement test
  }
}
