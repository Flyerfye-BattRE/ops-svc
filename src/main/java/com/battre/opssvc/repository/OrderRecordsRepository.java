package com.battre.opssvc.repository;

import com.battre.opssvc.model.OrderRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderRecordsRepository extends JpaRepository<OrderRecordType, Integer> {
  @Query("SELECT COUNT(*) FROM OrderRecordType")
  Integer countOrderRecords();

  @Query("SELECT completed " + "FROM OrderRecordType " + "WHERE orderId = :orderId")
  Boolean getOrderCompleted(@Param("orderId") int orderId);

  @Transactional
  @Modifying
  @Query("UPDATE OrderRecordType " + "SET completed = true " + "WHERE orderId = :orderId")
  void setOrderCompleted(@Param("orderId") int orderId);
}
