package np.com.nepalupi.launch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.launch.entity.GovtPaymentIntegration;
import np.com.nepalupi.launch.enums.GovtIntegrationStatus;
import np.com.nepalupi.launch.repository.GovtPaymentIntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovtPaymentIntegrationService {

    private final GovtPaymentIntegrationRepository govtPaymentIntegrationRepository;

    @Transactional
    public GovtPaymentIntegration identifyAgency(String agencyName, String agencyCode,
                                                  String paymentType, Long estimatedAnnualVolumePaisa,
                                                  Long estimatedAnnualTxnCount) {
        GovtPaymentIntegration integration = GovtPaymentIntegration.builder()
                .agencyName(agencyName)
                .agencyCode(agencyCode)
                .paymentType(paymentType)
                .integrationStatus(GovtIntegrationStatus.IDENTIFIED)
                .estimatedAnnualVolumePaisa(estimatedAnnualVolumePaisa)
                .estimatedAnnualTxnCount(estimatedAnnualTxnCount)
                .build();

        log.info("Identified govt agency for UPI integration: {} ({})", agencyName, paymentType);
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration signMou(UUID integrationId, String technicalContact) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.AGREEMENT_SIGNED);
        integration.setMouSignedAt(Instant.now());
        integration.setTechnicalContact(technicalContact);
        log.info("MOU signed with govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration startIntegration(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.DEVELOPMENT);
        integration.setIntegrationStartedAt(Instant.now());
        log.info("Integration started with govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration completeUat(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.TESTING);
        integration.setUatCompletedAt(Instant.now());
        log.info("UAT completed for govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration goLive(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.LIVE);
        integration.setLiveAt(Instant.now());
        log.info("Govt payment integration LIVE: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    public List<GovtPaymentIntegration> getByStatus(GovtIntegrationStatus status) {
        return govtPaymentIntegrationRepository.findByIntegrationStatusOrderByAgencyNameAsc(status);
    }

    public List<GovtPaymentIntegration> getAll() {
        return govtPaymentIntegrationRepository.findAll();
    }

    private GovtPaymentIntegration findById(UUID id) {
        return govtPaymentIntegrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Govt payment integration not found: " + id));
    }
}
