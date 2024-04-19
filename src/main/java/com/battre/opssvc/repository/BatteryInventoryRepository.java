package com.battre.opssvc.repository;

import com.battre.opssvc.model.BatteryInventoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BatteryInventoryRepository extends JpaRepository<BatteryInventoryType, Integer> {
    @Query("SELECT COUNT(*) FROM BatteryInventoryType")
    Integer countBatteryInventory();

    @Transactional
    @Modifying
    @Query("UPDATE BatteryInventoryType " +
            "SET batteryStatusId = (" +
                "SELECT batteryStatusId " +
                "FROM BatteryStatusType " +
                "WHERE status = :batteryStatus" +
            ") " +
            "WHERE intakeOrderId = :orderId")
    void setBatteryStatusesForIntakeOrder(@Param("orderId") int orderId,
                                          @Param("batteryStatus") String batteryStatus);

    @Query("SELECT bst.status " +
            "FROM BatteryInventoryType AS bit " +
            "INNER JOIN BatteryStatusType AS bst ON bst.batteryStatusId = bit.batteryStatusId " +
            "WHERE bit.intakeOrderId = :orderId " +
            "GROUP BY bst.status")
    List<String> getBatteryStatusesForIntakeOrder(@Param("orderId") int orderId);

    @Query("SELECT batteryId, batteryTypeId " +
            "FROM BatteryInventoryType " +
            "WHERE intakeOrderId = :orderId " +
            "GROUP BY batteryId, batteryTypeId")
    List<Object[]> getBatteryIdTypeIdsForIntakeOrder(@Param("orderId") int orderId);
}
