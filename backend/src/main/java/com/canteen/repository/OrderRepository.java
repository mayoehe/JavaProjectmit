package com.canteen.repository;

import com.canteen.entity.CanteenOrder;
import com.canteen.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CanteenOrder, Long> {
  List<CanteenOrder> findByStudentOrderByCreatedAtDesc(User student);
  List<CanteenOrder> findAllByOrderByCreatedAtDesc();
  List<CanteenOrder> findByStatusAndCreatedAtBefore(String status, Instant before);
  long countByStatus(String status);
}
