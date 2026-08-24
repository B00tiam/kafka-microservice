package self.dev.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import self.dev.order.domain.Order;


// no need for write / override methods, JpaRepository provides all the necessary CRUD operations
public interface OrderRepository extends JpaRepository<Order, Long> {
}
