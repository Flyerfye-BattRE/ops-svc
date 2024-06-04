package com.battre.opssvc.repository;

import com.battre.opssvc.model.BatteryStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatteryStatusRepository extends JpaRepository<BatteryStatusType, Integer> {
    @Query("SELECT bst FROM BatteryStatusType bst ORDER BY batteryStatusId")
    List<BatteryStatusType> getBatteryStatusTypes();
}
