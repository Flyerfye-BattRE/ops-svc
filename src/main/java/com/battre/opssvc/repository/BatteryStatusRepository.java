package com.battre.opssvc.repository;

import com.battre.opssvc.model.BatteryStatusType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BatteryStatusRepository extends JpaRepository<BatteryStatusType, Integer> {
  @Query("SELECT bst FROM BatteryStatusType bst ORDER BY batteryStatusId")
  List<BatteryStatusType> getBatteryStatusTypes();
}
