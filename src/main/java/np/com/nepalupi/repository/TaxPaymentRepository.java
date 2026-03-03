package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.TaxPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxPaymentRepository extends JpaRepository<TaxPayment, UUID> {

    List<TaxPayment> findByUserId(UUID userId);

    Page<TaxPayment> findByUserId(UUID userId, Pageable pageable);

    List<TaxPayment> findByTaxpayerPan(String pan);

    @Query("SELECT t FROM TaxPayment t WHERE t.taxpayerPan = :pan AND t.fiscalYear = :year")
    List<TaxPayment> findByPanAndFiscalYear(@Param("pan") String pan, @Param("year") String fiscalYear);

    Optional<TaxPayment> findByVoucherNumber(String voucherNumber);
}
