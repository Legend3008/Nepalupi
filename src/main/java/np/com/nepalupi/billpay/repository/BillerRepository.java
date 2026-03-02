package np.com.nepalupi.billpay.repository;

import np.com.nepalupi.billpay.entity.Biller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillerRepository extends JpaRepository<Biller, UUID> {

    Optional<Biller> findByBillerId(String billerId);

    List<Biller> findByCategoryAndIsActiveTrue(String category);

    List<Biller> findByIsActiveTrue();

    List<Biller> findByBillerNameContainingIgnoreCaseAndIsActiveTrue(String name);
}
