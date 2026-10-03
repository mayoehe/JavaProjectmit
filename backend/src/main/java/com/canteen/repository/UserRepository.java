package com.canteen.repository;

import com.canteen.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByStudentId(String studentId);
  boolean existsByStudentId(String studentId);
  List<User> findByRoleOrderByStudentIdAsc(String role);
  List<User> findByTabBalanceGreaterThanOrderByTabBalanceDesc(double min);
  List<User> findAllByOrderByIdAsc();
  default List<User> findAllByOrderById() { return findAllByOrderByIdAsc(); }
}
