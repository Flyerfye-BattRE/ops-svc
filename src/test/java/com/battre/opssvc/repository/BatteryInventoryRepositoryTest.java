package com.battre.opssvc.repository;

import com.battre.opssvc.enums.BatteryStatus;
import com.battre.opssvc.service.OpsSvc;
import com.battre.stubs.services.BatteryIdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.logging.Logger;

import static com.battre.opssvc.service.OpsSvc.convertToProcessLabBatteriesList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class BatteryInventoryRepositoryTest {
    private static final Logger logger = Logger.getLogger(BatteryInventoryRepositoryTest.class.getName());

    @Autowired
    private BatteryInventoryRepository batInvRepo;

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql", "/testdb/test-bir-populateBatteryInventory.sql"})
    public void testCountBatteryInventory() {
        int count = batInvRepo.countBatteryInventory();

        // Verify the result
        assertEquals(4, count);
    }

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql", "/testdb/test-bir-populateBatteryInventory.sql"})
    public void testGetBatteryStatusesForIntakeOrder() {
        // Verify the result
        List<String> statuses = batInvRepo.getBatteryStatusesForIntakeOrder(1);
        assertEquals(1, statuses.size());
        assertEquals(BatteryStatus.TESTING.toString(),
                batInvRepo.getBatteryStatusesForIntakeOrder(1).get(0));
    }

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql", "/testdb/test-bir-populateBatteryInventory.sql"})
    public void testSetBatteryStatusesForIntakeOrder() {
        batInvRepo.setBatteryStatusesForIntakeOrder(2, BatteryStatus.STORAGE.toString());

        // Verify the result
        List<String> statuses = batInvRepo.getBatteryStatusesForIntakeOrder(2);
        assertEquals(1, statuses.size());
        assertEquals(BatteryStatus.STORAGE.toString(),
                statuses.get(0));
    }

    @Test
    @Sql(scripts = {"/testdb/test-bir-populateOrderRecords.sql", "/testdb/test-bir-populateBatteryInventory.sql"})
    public void testGetBatteryIdTypeIdsForIntakeOrder() {
        List<Object[]> resultList = batInvRepo.getBatteryIdTypeIdsForIntakeOrder(1);
        assertEquals(3, resultList.size());

        // Verify the result
        List<BatteryIdType> batteryIdTypeIdList = convertToProcessLabBatteriesList(resultList);
        assertEquals(3, batteryIdTypeIdList.size());

        assertEquals(1, batteryIdTypeIdList.get(0).getBatteryId());
        assertEquals(1, batteryIdTypeIdList.get(0).getBatteryTypeId());
        assertEquals(2, batteryIdTypeIdList.get(1).getBatteryId());
        assertEquals(6, batteryIdTypeIdList.get(1).getBatteryTypeId());
        assertEquals(3, batteryIdTypeIdList.get(2).getBatteryId());
        assertEquals(8, batteryIdTypeIdList.get(2).getBatteryTypeId());
    }
}
