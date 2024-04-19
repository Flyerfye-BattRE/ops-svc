package com.battre.opssvc.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class OrderRecordsRepositoryTest {
    private static final Logger logger = Logger.getLogger(OrderRecordsRepositoryTest.class.getName());

    @Autowired
    private OrderRecordsRepository ordRecRepo;

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql"})
    public void testCountOrderRecords() {
        int count = ordRecRepo.countOrderRecords();

        // Verify the result
        assertEquals(4, count);
    }

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql"})
    public void testGetOrderCompleted() {
        boolean completedTrue = ordRecRepo.getOrderCompleted(4);

        // Verify the result
        assertEquals(true, completedTrue);

        boolean completedFalse = ordRecRepo.getOrderCompleted(2);

        // Verify the result
        assertEquals(false, completedFalse);
    }

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql"})
    public void testSetOrderCompleted() {
        ordRecRepo.setOrderCompleted(3);

        // Verify the result
        boolean isCompleted = ordRecRepo.getOrderCompleted(3);
        assertEquals(true, isCompleted);
    }
}
