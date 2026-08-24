package self.dev.payment.repository;

import self.dev.payment.domain.Account;

import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountRepository extends JpaRepository<Account, String> {
}
