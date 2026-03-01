package np.com.nepalupi.launch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.launch.entity.IncentiveProgram;
import np.com.nepalupi.launch.enums.IncentiveProgramType;
import np.com.nepalupi.launch.repository.IncentiveProgramRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncentiveProgramService {

    private final IncentiveProgramRepository incentiveProgramRepository;

    @Transactional
    public IncentiveProgram createProgram(IncentiveProgram program) {
        log.info("Creating incentive program: {} ({})", program.getProgramName(), program.getProgramType());
        return incentiveProgramRepository.save(program);
    }

    @Transactional
    public IncentiveProgram recordRedemption(UUID programId, long amountPaisa) {
        IncentiveProgram program = findById(programId);

        if (!program.getIsActive()) {
            throw new IllegalStateException("Program " + program.getProgramCode() + " is not active");
        }

        long newSpent = program.getSpentPaisa() + amountPaisa;
        if (newSpent > program.getBudgetPaisa()) {
            throw new IllegalStateException("Redemption would exceed budget for program " + program.getProgramCode());
        }

        program.setSpentPaisa(newSpent);
        program.setTotalRedemptions(program.getTotalRedemptions() + 1);

        // Check if budget is 90%+ exhausted
        double utilization = (double) newSpent / program.getBudgetPaisa() * 100;
        if (utilization >= 90) {
            log.warn("Incentive program {} is {}% utilized", program.getProgramCode(), String.format("%.1f", utilization));
        }

        return incentiveProgramRepository.save(program);
    }

    @Transactional
    public IncentiveProgram deactivateProgram(UUID programId) {
        IncentiveProgram program = findById(programId);
        program.setIsActive(false);
        log.info("Deactivated incentive program: {}", program.getProgramCode());
        return incentiveProgramRepository.save(program);
    }

    public List<IncentiveProgram> getActivePrograms() {
        return incentiveProgramRepository.findByIsActiveTrue();
    }

    public List<IncentiveProgram> getProgramsByType(IncentiveProgramType type) {
        return incentiveProgramRepository.findByProgramType(type);
    }

    public List<IncentiveProgram> getProgramsByPsp(String pspId) {
        return incentiveProgramRepository.findByPspId(pspId);
    }

    // ─── Scheduled: Auto-deactivate exhausted/expired programs ──────

    @Scheduled(cron = "0 30 0 * * *") // Daily at 12:30 AM
    @Transactional
    public void autoDeactivatePrograms() {
        log.info("Running auto-deactivation check for incentive programs");

        List<IncentiveProgram> exhausted = incentiveProgramRepository.findExhaustedPrograms();
        for (IncentiveProgram program : exhausted) {
            program.setIsActive(false);
            incentiveProgramRepository.save(program);
            log.info("Auto-deactivated exhausted program: {}", program.getProgramCode());
        }

        List<IncentiveProgram> expired = incentiveProgramRepository.findExpiredButActivePrograms();
        for (IncentiveProgram program : expired) {
            program.setIsActive(false);
            incentiveProgramRepository.save(program);
            log.info("Auto-deactivated expired program: {}", program.getProgramCode());
        }

        if (!exhausted.isEmpty() || !expired.isEmpty()) {
            log.info("Deactivated {} exhausted and {} expired incentive programs",
                    exhausted.size(), expired.size());
        }
    }

    private IncentiveProgram findById(UUID id) {
        return incentiveProgramRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incentive program not found: " + id));
    }
}
