package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.merchant.dto.DynamicQrRequest;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.entity.MerchantQrCode;
import np.com.nepalupi.merchant.enums.QrCodeType;
import np.com.nepalupi.merchant.repository.MerchantQrCodeRepository;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * QR Code Service — generates UPI QR codes following BharatQR/NPCI spec.
 * <p>
 * Static QR:  Contains only merchant VPA — customer enters amount.
 * Dynamic QR: Contains amount + txn ref — one-time use, expires.
 * <p>
 * QR payload format (EMVCo / NPCI-compatible):
 * upi://pay?pa={vpa}&pn={name}&mc={mcc}&tid={txnRef}&am={amount}&cu=NPR
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private final MerchantQrCodeRepository qrCodeRepository;
    private final MerchantRepository merchantRepository;

    /**
     * Generate a static QR for a merchant — printed on sticker.
     */
    public String generateStaticQr(Merchant merchant) {
        String qrPayload = buildQrPayload(merchant, null, null);

        MerchantQrCode qrCode = MerchantQrCode.builder()
                .merchantId(merchant.getId())
                .qrType(QrCodeType.STATIC)
                .qrData(qrPayload)
                .build();

        qrCodeRepository.save(qrCode);
        log.info("Static QR generated for merchant={}", merchant.getMerchantId());
        return qrPayload;
    }

    /**
     * Generate a dynamic QR for a specific transaction.
     */
    @Transactional
    public MerchantQrCode generateDynamicQr(DynamicQrRequest request) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        String txnRef = request.getMerchantTxnRef() != null
                ? request.getMerchantTxnRef()
                : "DQR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        int expireSeconds = request.getExpireSeconds() != null ? request.getExpireSeconds() : 900; // 15 min

        String qrPayload = buildQrPayload(merchant, request.getAmountPaisa(), txnRef);

        MerchantQrCode qrCode = MerchantQrCode.builder()
                .merchantId(merchant.getId())
                .qrType(QrCodeType.DYNAMIC)
                .qrData(qrPayload)
                .qrPayload("{\"pa\":\"" + merchant.getMerchantVpa() + "\",\"am\":"
                        + (request.getAmountPaisa() / 100.0) + ",\"ref\":\"" + txnRef + "\"}")
                .amountPaisa(request.getAmountPaisa())
                .merchantTxnRef(txnRef)
                .description(request.getDescription())
                .expiresAt(Instant.now().plusSeconds(expireSeconds))
                .build();

        qrCode = qrCodeRepository.save(qrCode);
        log.info("Dynamic QR generated: ref={} amount={} merchant={}",
                txnRef, request.getAmountPaisa(), merchant.getMerchantId());
        return qrCode;
    }

    /**
     * Mark a QR code as scanned and link to transaction.
     */
    @Transactional
    public MerchantQrCode markScanned(UUID qrCodeId, UUID transactionId) {
        MerchantQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new IllegalArgumentException("QR code not found"));

        qrCode.setScanned(true);
        qrCode.setTransactionId(transactionId);
        return qrCodeRepository.save(qrCode);
    }

    /**
     * Resolve a dynamic QR by merchant transaction reference.
     */
    public MerchantQrCode resolveByTxnRef(String merchantTxnRef) {
        return qrCodeRepository.findByMerchantTxnRef(merchantTxnRef)
                .orElseThrow(() -> new IllegalArgumentException("QR code not found for ref: " + merchantTxnRef));
    }

    // ── Helpers ──

    private String buildQrPayload(Merchant merchant, Long amountPaisa, String txnRef) {
        StringBuilder sb = new StringBuilder("upi://pay?");
        sb.append("pa=").append(merchant.getMerchantVpa());
        sb.append("&pn=").append(urlEncode(merchant.getBusinessName()));
        if (merchant.getMccCode() != null) {
            sb.append("&mc=").append(merchant.getMccCode());
        }
        if (amountPaisa != null) {
            sb.append("&am=").append(String.format("%.2f", amountPaisa / 100.0));
        }
        if (txnRef != null) {
            sb.append("&tid=").append(txnRef);
        }
        sb.append("&cu=NPR");
        return sb.toString();
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20").replace("&", "%26");
    }
}
