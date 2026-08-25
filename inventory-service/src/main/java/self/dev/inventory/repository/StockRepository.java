package self.dev.inventory.repository;

import self.dev.inventory.domain.Stock;

import org.springframework.data.jpa.repository.JpaRepository;


public interface StockRepository extends JpaRepository<Stock, String> {
}
