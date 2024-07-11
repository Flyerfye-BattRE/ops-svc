package com.battre.opssvc.repository;

import com.battre.opssvc.model.CustomerDataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerDataRepository extends JpaRepository<CustomerDataType, Integer> {
  @Query("SELECT cdt " + "FROM CustomerDataType AS cdt " + "ORDER BY customerId ")
  List<CustomerDataType> getCustomerList();

  @Query("SELECT COUNT(DISTINCT cdt.loyaltyId) FROM CustomerDataType AS cdt")
  Integer countCustomers();
}
