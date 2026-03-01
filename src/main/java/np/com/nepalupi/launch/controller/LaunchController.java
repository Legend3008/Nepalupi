package np.com.nepalupi.launch.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.launch.entity.*;
import np.com.nepalupi.launch.enums.*;
import np.com.nepalupi.launch.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/launch")
@RequiredArgsConstructor
public class LaunchController {

    private final LaunchPhaseService launchPhaseService;
    private final LaunchMetricsService launchMetricsService;
    private final MerchantAcquisitionService merchantAcquisitionService;
    private final GovtPaymentIntegrationService govtPaymentIntegrationService;
    private final IncentiveProgramService incentiveProgramService;

    // ═══════════════════════════════════════════════════════════════
    // LAUNCH PHASES
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/phases/initialize")
    public ResponseEntity<LaunchPhase> initializePhases() {
        return ResponseEntity.ok(launchPhaseService.initializePhases());
    }

    @GetMapping("/phases")
    public ResponseEntity<List<LaunchPhase>> getAllPhases() {
        return ResponseEntity.ok(launchPhaseService.getAllPhases());
    }

    @GetMapping("/phases/active")
    public ResponseEntity<LaunchPhase> getActivePhase() {
        return ResponseEntity.ok(launchPhaseService.getActivePhase());
    }

    @PostMapping("/phases/{phaseName}/activate")
    public ResponseEntity<LaunchPhase> activatePhase(@PathVariable LaunchPhaseName phaseName) {
        return ResponseEntity.ok(launchPhaseService.activatePhase(phaseName));
    }

    @PostMapping("/phases/{phaseName}/complete")
    public ResponseEntity<LaunchPhase> completePhase(@PathVariable LaunchPhaseName phaseName) {
        return ResponseEntity.ok(launchPhaseService.completePhase(phaseName));
    }

    // ─── Checklist ──────────────────────────────────────────────────

    @GetMapping("/phases/{phaseName}/checklist")
    public ResponseEntity<List<LaunchChecklistItem>> getChecklist(@PathVariable LaunchPhaseName phaseName) {
        return ResponseEntity.ok(launchPhaseService.getChecklistByPhaseName(phaseName));
    }

    @GetMapping("/phases/{phaseId}/checklist/blocking")
    public ResponseEntity<List<LaunchChecklistItem>> getBlockingItems(@PathVariable UUID phaseId) {
        return ResponseEntity.ok(launchPhaseService.getIncompleteBlockingItems(phaseId));
    }

    @PutMapping("/checklist/{itemId}/status")
    public ResponseEntity<LaunchChecklistItem> updateChecklistItem(
            @PathVariable UUID itemId,
            @RequestParam ChecklistItemStatus status) {
        return ResponseEntity.ok(launchPhaseService.updateChecklistItem(itemId, status));
    }

    // ═══════════════════════════════════════════════════════════════
    // LAUNCH METRICS
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/metrics")
    public ResponseEntity<LaunchMetric> recordMetrics(@RequestBody LaunchMetric metric) {
        return ResponseEntity.ok(launchMetricsService.recordDailyMetrics(metric));
    }

    @PutMapping("/metrics/{date}")
    public ResponseEntity<LaunchMetric> updateMetrics(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody LaunchMetric updates) {
        return ResponseEntity.ok(launchMetricsService.updateDailyMetrics(date, updates));
    }

    @GetMapping("/metrics/latest")
    public ResponseEntity<?> getLatestMetrics() {
        return launchMetricsService.getLatestMetrics()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/metrics/range")
    public ResponseEntity<List<LaunchMetric>> getMetricsRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(launchMetricsService.getMetricsRange(start, end));
    }

    @GetMapping("/metrics/recent/{days}")
    public ResponseEntity<List<LaunchMetric>> getRecentMetrics(@PathVariable int days) {
        return ResponseEntity.ok(launchMetricsService.getRecentMetrics(days));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(launchMetricsService.getDashboardSummary());
    }

    // ═══════════════════════════════════════════════════════════════
    // MERCHANT ACQUISITION
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/merchants")
    public ResponseEntity<MerchantAcquisition> onboardMerchant(
            @RequestParam String merchantName,
            @RequestParam String city,
            @RequestParam(required = false) String category,
            @RequestParam FootfallCategory footfallCategory,
            @RequestParam AcquisitionChannel channel,
            @RequestParam String acquiredBy) {
        return ResponseEntity.ok(merchantAcquisitionService.onboardMerchant(
                merchantName, city, category, footfallCategory, channel, acquiredBy));
    }

    @PostMapping("/merchants/{merchantId}/deploy-qr")
    public ResponseEntity<MerchantAcquisition> deployQr(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(merchantAcquisitionService.deployQr(merchantId));
    }

    @PostMapping("/merchants/{merchantId}/first-transaction")
    public ResponseEntity<MerchantAcquisition> recordFirstTransaction(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(merchantAcquisitionService.recordFirstTransaction(merchantId));
    }

    @PostMapping("/merchants/{merchantId}/churn")
    public ResponseEntity<MerchantAcquisition> recordChurn(
            @PathVariable UUID merchantId,
            @RequestParam String reason) {
        return ResponseEntity.ok(merchantAcquisitionService.recordChurn(merchantId, reason));
    }

    @GetMapping("/merchants/active")
    public ResponseEntity<List<MerchantAcquisition>> getActiveMerchants() {
        return ResponseEntity.ok(merchantAcquisitionService.getActiveMerchants());
    }

    @GetMapping("/merchants/city/{city}")
    public ResponseEntity<List<MerchantAcquisition>> getMerchantsByCity(@PathVariable String city) {
        return ResponseEntity.ok(merchantAcquisitionService.getMerchantsByCity(city));
    }

    @GetMapping("/merchants/qr-no-txn")
    public ResponseEntity<List<MerchantAcquisition>> getQrNoTransaction() {
        return ResponseEntity.ok(merchantAcquisitionService.getQrDeployedNoTransaction());
    }

    @GetMapping("/merchants/churned")
    public ResponseEntity<List<MerchantAcquisition>> getChurnedMerchants() {
        return ResponseEntity.ok(merchantAcquisitionService.getChurnedMerchants());
    }

    @GetMapping("/merchants/by-city")
    public ResponseEntity<List<Object[]>> getMerchantCountByCity() {
        return ResponseEntity.ok(merchantAcquisitionService.getMerchantCountByCity());
    }

    // ═══════════════════════════════════════════════════════════════
    // GOVERNMENT PAYMENT INTEGRATION
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/govt")
    public ResponseEntity<GovtPaymentIntegration> identifyAgency(
            @RequestParam String agencyName,
            @RequestParam String agencyCode,
            @RequestParam String paymentType,
            @RequestParam(required = false) Long estimatedAnnualVolumePaisa,
            @RequestParam(required = false) Long estimatedAnnualTxnCount) {
        return ResponseEntity.ok(govtPaymentIntegrationService.identifyAgency(
                agencyName, agencyCode, paymentType, estimatedAnnualVolumePaisa, estimatedAnnualTxnCount));
    }

    @PostMapping("/govt/{id}/sign-mou")
    public ResponseEntity<GovtPaymentIntegration> signMou(
            @PathVariable UUID id,
            @RequestParam String technicalContact) {
        return ResponseEntity.ok(govtPaymentIntegrationService.signMou(id, technicalContact));
    }

    @PostMapping("/govt/{id}/start-integration")
    public ResponseEntity<GovtPaymentIntegration> startIntegration(@PathVariable UUID id) {
        return ResponseEntity.ok(govtPaymentIntegrationService.startIntegration(id));
    }

    @PostMapping("/govt/{id}/complete-uat")
    public ResponseEntity<GovtPaymentIntegration> completeUat(@PathVariable UUID id) {
        return ResponseEntity.ok(govtPaymentIntegrationService.completeUat(id));
    }

    @PostMapping("/govt/{id}/go-live")
    public ResponseEntity<GovtPaymentIntegration> goLive(@PathVariable UUID id) {
        return ResponseEntity.ok(govtPaymentIntegrationService.goLive(id));
    }

    @GetMapping("/govt")
    public ResponseEntity<List<GovtPaymentIntegration>> getAllGovtIntegrations() {
        return ResponseEntity.ok(govtPaymentIntegrationService.getAll());
    }

    @GetMapping("/govt/status/{status}")
    public ResponseEntity<List<GovtPaymentIntegration>> getGovtByStatus(@PathVariable GovtIntegrationStatus status) {
        return ResponseEntity.ok(govtPaymentIntegrationService.getByStatus(status));
    }

    // ═══════════════════════════════════════════════════════════════
    // INCENTIVE PROGRAMS
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/incentives")
    public ResponseEntity<IncentiveProgram> createProgram(@RequestBody IncentiveProgram program) {
        return ResponseEntity.ok(incentiveProgramService.createProgram(program));
    }

    @PostMapping("/incentives/{programId}/redeem")
    public ResponseEntity<IncentiveProgram> recordRedemption(
            @PathVariable UUID programId,
            @RequestParam long amountPaisa) {
        return ResponseEntity.ok(incentiveProgramService.recordRedemption(programId, amountPaisa));
    }

    @PostMapping("/incentives/{programId}/deactivate")
    public ResponseEntity<IncentiveProgram> deactivateProgram(@PathVariable UUID programId) {
        return ResponseEntity.ok(incentiveProgramService.deactivateProgram(programId));
    }

    @GetMapping("/incentives/active")
    public ResponseEntity<List<IncentiveProgram>> getActivePrograms() {
        return ResponseEntity.ok(incentiveProgramService.getActivePrograms());
    }

    @GetMapping("/incentives/type/{type}")
    public ResponseEntity<List<IncentiveProgram>> getProgramsByType(@PathVariable IncentiveProgramType type) {
        return ResponseEntity.ok(incentiveProgramService.getProgramsByType(type));
    }

    @GetMapping("/incentives/psp/{pspId}")
    public ResponseEntity<List<IncentiveProgram>> getProgramsByPsp(@PathVariable String pspId) {
        return ResponseEntity.ok(incentiveProgramService.getProgramsByPsp(pspId));
    }
}
