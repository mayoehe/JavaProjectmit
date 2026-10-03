package com.canteen.repository;

import com.canteen.entity.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
  List<MenuItem> findByAvailableTrueOrderByCategoryAscNameAsc();
  List<MenuItem> findAllByOrderByCategoryAscNameAsc();
  List<MenuItem> findAllByOrderByIdAsc();
  default List<MenuItem> findAllByOrderById() { return findAllByOrderByIdAsc(); }
}
