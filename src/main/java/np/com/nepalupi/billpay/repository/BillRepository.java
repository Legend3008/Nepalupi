package np.com.nepalupi.billpay.repository;

import np.com.nepalupi.billpay.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    List<Bill> findByBillerIdAndCustomerIdentifierAndStatus(UUID billerId, String customerIdentifier, String status);

    List<Bill> findByPayerVpaAndStatusOrderByCreatedAtDesc(String payerVpa, String status);

    Optional<Bill> findByBillNumber(String billNumber);

    @Query("SELECT b FROM Bill b WHERE b.status = 'PENDING' AND b.dueDate IS NOT NULL AND b.dueDate < CURRENT_DATE")
    List<Bill> findOverdueBills();

    @Query("SELECT b FROM Bill b WHERE b.payerVpa = :payerVpa ORDER BY b.createdAt DESC")
    List<Bill> findByPayerVpa(@Param("payerVpa") String payerVpa);
}
