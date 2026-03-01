package np.com.nepalupi.merchant.repository;

import np.com.nepalupi.merchant.entity.MerchantQrCode;
import np.com.nepalupi.merchant.enums.QrCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantQrCodeRepository extends JpaRepository<MerchantQrCode, UUID> {

    List<MerchantQrCode> findByMerchantIdAndQrType(UUID merchantId, QrCodeType qrType);

    Optional<MerchantQrCode> findByMerchantTxnRef(String merchantTxnRef);

    List<MerchantQrCode> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
