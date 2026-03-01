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
    public GovtPaymentIntegration identifyAgency(String agencyName, String paymentType,
                                                  String contactPerson, String contactEmail,
                                                  Long estimatedMonthlyVolume) {
        GovtPaymentIntegration integration = GovtPaymentIntegration.builder()
                .agencyName(agencyName)
                .paymentType(paymentType)
                .integrationStatus(GovtIntegrationStatus.IDENTIFIED)
                .contactPerson(contactPerson)
                .contactEmail(contactEmail)
                .estimatedMonthlyVolume(estimatedMonthlyVolume != null ? estimatedMonthlyVolume : 0L)
                .build();

        log.info("Identified govt agency for UPI integration: {} ({})", agencyName, paymentType);
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration signAgreement(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.AGREEMENT_SIGNED);
        integration.setAgreementSignedAt(Instant.now());
        log.info("Agreement signed with govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration startIntegration(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.DEVELOPMENT);
        log.info("Integration started with govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration completeUat(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.TESTING);
        log.info("UAT completed for govt agency: {}", integration.getAgencyName());
        return govtPaymentIntegrationRepository.save(integration);
    }

    @Transactional
    public GovtPaymentIntegration goLive(UUID integrationId) {
        GovtPaymentIntegration integration = findById(integrationId);
        integration.setIntegrationStatus(GovtIntegrationStatus.LIVE);
        integration.setGoLiveDate(java.time.LocalDate.now());
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
