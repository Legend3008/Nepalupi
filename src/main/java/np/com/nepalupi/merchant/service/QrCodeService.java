package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.merchant.dto.DynamicQrRequest;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.entity.MerchantQrCode;
import np.com.nepalupi.merchant.enums.QrCodeType;
import np.com.nepalupi.merchant.repository.MerchantQrCodeRepository;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
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

    // ── QR Image Generation (ZXing) ──

    /**
     * Generate a QR code PNG image from a UPI payload string.
     * Returns the image as a Base64-encoded string (for API responses).
     * <p>
     * Section 3.1: QR scanner + generator
     *
     * @param qrPayload UPI payload string (upi://pay?...)
     * @param size      Image size in pixels (width = height)
     * @return Base64-encoded PNG image string
     */
    public String generateQrImage(String qrPayload, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            BitMatrix bitMatrix = writer.encode(qrPayload, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            log.info("QR image generated: {}x{} pixels, {} bytes",
                    size, size, outputStream.size());
            return base64Image;
        } catch (WriterException | IOException e) {
            log.error("QR image generation failed: {}", e.getMessage());
            throw new RuntimeException("QR code image generation failed", e);
        }
    }

    /**
     * Generate a QR code PNG image as raw bytes.
     * For direct HTTP image response (Content-Type: image/png).
     */
    public byte[] generateQrImageBytes(String qrPayload, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            BitMatrix bitMatrix = writer.encode(qrPayload, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("QR image generation failed: {}", e.getMessage());
            throw new RuntimeException("QR code image generation failed", e);
        }
    }

    /**
     * Generate a static QR with image for a merchant.
     * Returns the QR payload + Base64 image.
     */
    public Map<String, String> generateStaticQrWithImage(Merchant merchant) {
        String qrPayload = generateStaticQr(merchant);
        String qrImage = generateQrImage(qrPayload, 400);
        return Map.of(
                "qrPayload", qrPayload,
                "qrImageBase64", qrImage,
                "imageFormat", "PNG",
                "imageSize", "400x400"
        );
    }
}
