package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.security.entity.CertificateInventory;
import np.com.nepalupi.security.enums.CertificateStatus;
import np.com.nepalupi.security.enums.CertificateType;
import np.com.nepalupi.security.repository.CertificateInventoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Certificate lifecycle management — tracks all TLS, mTLS, HSM,
 * and code-signing certificates. Monitors expiry, triggers rotation,
 * and alerts 60 days before expiry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateManagementService {

    private final CertificateInventoryRepository certRepository;

    @Transactional
    public CertificateInventory registerCertificate(String certName, CertificateType certType,
                                                      String subjectCn, String issuer,
                                                      String serialNumber, String fingerprint,
                                                      Instant validFrom, Instant validUntil,
                                                      String associatedService, String keyStoreLocation,
                                                      boolean autoRotate, Integer rotationPeriodDays) {
        CertificateInventory cert = CertificateInventory.builder()
                .certName(certName)
                .certType(certType)
                .subjectCn(subjectCn)
                .issuer(issuer)
                .serialNumber(serialNumber)
                .fingerprintSha256(fingerprint)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .associatedService(associatedService)
                .keyStoreLocation(keyStoreLocation)
                .autoRotate(autoRotate)
                .rotationPeriodDays(rotationPeriodDays)
                .status(CertificateStatus.ACTIVE)
                .build();

        if (autoRotate && rotationPeriodDays != null) {
            cert.setNextRotationDue(Instant.now().plus(rotationPeriodDays, ChronoUnit.DAYS));
        }

        cert = certRepository.save(cert);
        log.info("Certificate registered: name={}, type={}, service={}, expiresAt={}",
                certName, certType, associatedService, validUntil);
        return cert;
    }

    @Transactional
    public CertificateInventory rotateCertificate(UUID certId, String newSerialNumber,
                                                    String newFingerprint,
                                                    Instant newValidFrom, Instant newValidUntil) {
        CertificateInventory cert = certRepository.findById(certId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certId));

        // Mark old as rotated
        cert.setStatus(CertificateStatus.ROTATED);
        certRepository.save(cert);

        // Create new cert record
        CertificateInventory newCert = CertificateInventory.builder()
                .certName(cert.getCertName())
                .certType(cert.getCertType())
                .subjectCn(cert.getSubjectCn())
                .issuer(cert.getIssuer())
                .serialNumber(newSerialNumber)
                .fingerprintSha256(newFingerprint)
                .validFrom(newValidFrom)
                .validUntil(newValidUntil)
                .associatedService(cert.getAssociatedService())
                .keyStoreLocation(cert.getKeyStoreLocation())
                .autoRotate(cert.getAutoRotate())
                .rotationPeriodDays(cert.getRotationPeriodDays())
                .status(CertificateStatus.ACTIVE)
                .lastRotatedAt(Instant.now())
                .build();

        if (cert.getAutoRotate() && cert.getRotationPeriodDays() != null) {
            newCert.setNextRotationDue(Instant.now().plus(cert.getRotationPeriodDays(), ChronoUnit.DAYS));
        }

        newCert = certRepository.save(newCert);
        log.info("Certificate rotated: name={}, oldId={}, newId={}", cert.getCertName(), certId, newCert.getId());
        return newCert;
    }

    @Transactional
    public void revokeCertificate(UUID certId) {
        CertificateInventory cert = certRepository.findById(certId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        cert.setStatus(CertificateStatus.REVOKED);
        certRepository.save(cert);
        log.warn("Certificate REVOKED: name={}, service={}", cert.getCertName(), cert.getAssociatedService());
    }

    /**
     * Daily at 8 AM: check for certificates expiring within 60 days.
     * Alert if not already alerted.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpiringCertificates() {
        Instant sixtyDaysFromNow = Instant.now().plus(60, ChronoUnit.DAYS);
        List<CertificateInventory> expiring = certRepository.findExpiringBefore(sixtyDaysFromNow);

        for (CertificateInventory cert : expiring) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), cert.getValidUntil());

            if (daysUntilExpiry <= 0) {
                cert.setStatus(CertificateStatus.EXPIRED);
                log.error("CERTIFICATE EXPIRED: name={}, service={}, expired={}",
                        cert.getCertName(), cert.getAssociatedService(), cert.getValidUntil());
            } else if (!cert.getAlertSent()) {
                cert.setStatus(CertificateStatus.EXPIRING_SOON);
                cert.setAlertSent(true);
                log.warn("Certificate expiring in {} days: name={}, service={}",
                        daysUntilExpiry, cert.getCertName(), cert.getAssociatedService());
            }
            certRepository.save(cert);
        }
    }

    /**
     * Daily: trigger auto-rotation for certificates due for rotation.
     */
    @Scheduled(cron = "0 30 8 * * *")
    @Transactional(readOnly = true)
    public void checkAutoRotation() {
        List<CertificateInventory> dueForRotation = certRepository.findDueForAutoRotation(Instant.now());
        for (CertificateInventory cert : dueForRotation) {
            log.warn("Certificate due for auto-rotation: name={}, service={}, nextRotation={}",
                    cert.getCertName(), cert.getAssociatedService(), cert.getNextRotationDue());
            // In production: trigger automated rotation via HSM/PKI integration
        }
    }

    public List<CertificateInventory> getActiveCertificates() {
        return certRepository.findByStatusOrderByValidUntil(CertificateStatus.ACTIVE);
    }

    public List<CertificateInventory> getCertificatesForService(String service) {
        return certRepository.findByAssociatedServiceOrderByValidUntil(service);
    }
}
