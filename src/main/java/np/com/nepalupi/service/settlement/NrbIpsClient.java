package np.com.nepalupi.service.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * NRB-IPS (Inter-bank Payment System) Client — handles actual inter-bank
 * fund transfers for UPI settlement.
 * <p>
 * Section 3.5: Settlement Layer
 * - In India: NPCI uses RBI RTGS/NEFT for inter-bank settlement
 * - In Nepal: NCHL uses NRB-operated IPS for real-time gross settlement
 * <p>
 * After the SettlementEngine calculates net positions and generates the
 * settlement file, this service initiates the actual wire transfers:
 * 1. Banks with positive net position → PAY to NCHL settlement account
 * 2. NCHL distributes to banks with negative net position → RECEIVE
 * <p>
 * PRODUCTION NOTE: Real NRB-IPS integration requires:
 * - NRB API credentials and certificate
 * - SWIFT/BIC codes for participating banks
 * - Real-time settlement queue management
 * - Retry and reconciliation for failed transfers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NrbIpsClient {

    @Value("${nepalupi.nrb-ips.endpoint:https://ips.nrb.org.np/api/v1}")
    private String nrbIpsEndpoint;

    @Value("${nepalupi.nrb-ips.settlement-account:NCHL-SETTLEMENT-001}")
    private String nchlSettlementAccount;

    /**
     * Submit net settlement positions to NRB-IPS for actual fund transfer.
     *
     * @param netPositions Per-bank net positions (positive = bank pays, negative = bank receives)
     * @param settlementRef  Unique settlement reference (from SettlementReport)
     * @return true if all settlement instructions were accepted
     */
    public boolean submitNetSettlement(Map<String, Long> netPositions, String settlementRef) {
        log.info("═══ NRB-IPS Settlement Submission ═══");
        log.info("Settlement ref: {}", settlementRef);
        log.info("Banks in settlement: {}", netPositions.size());

        boolean allSuccess = true;

        for (Map.Entry<String, Long> entry : netPositions.entrySet()) {
            String bankCode = entry.getKey();
            long position = entry.getValue();

            if (position == 0) {
                log.info("  Bank {} has zero net position — no transfer needed", bankCode);
                continue;
            }

            boolean success;
            if (position > 0) {
                // Bank owes money → initiate debit from bank's settlement account
                success = initiateDebitTransfer(bankCode, position, settlementRef);
            } else {
                // Bank is owed money → initiate credit to bank's settlement account
                success = initiateCreditTransfer(bankCode, Math.abs(position), settlementRef);
            }

            if (!success) {
                allSuccess = false;
                log.error("  NRB-IPS transfer FAILED for bank {}", bankCode);
            }
        }

        log.info("═══ NRB-IPS Settlement {} ═══",
                allSuccess ? "COMPLETED" : "PARTIALLY FAILED");
        return allSuccess;
    }

    /**
     * Initiate debit from a bank's NRB settlement account.
     * Bank pays its net debit position to NCHL settlement pool.
     */
    private boolean initiateDebitTransfer(String bankCode, long amountPaisa, String settlementRef) {
        BigDecimal amountNpr = BigDecimal.valueOf(amountPaisa).divide(BigDecimal.valueOf(100));

        log.info("  NRB-IPS DEBIT: bank={} → NCHL settlement | amount=NPR {} | ref={}",
                bankCode, amountNpr, settlementRef);

        // PRODUCTION: Call NRB-IPS API
        // POST {nrbIpsEndpoint}/settlement/debit
        // {
        //   "debitAccount": "{bankCode}-SETTLEMENT",
        //   "creditAccount": "{nchlSettlementAccount}",
        //   "amount": amountNpr,
        //   "currency": "NPR",
        //   "reference": settlementRef,
        //   "purpose": "UPI_NET_SETTLEMENT"
        // }

        // Dev mode: simulate success
        log.info("  NRB-IPS DEBIT simulated (dev mode): {} owes NPR {}", bankCode, amountNpr);
        return true;
    }

    /**
     * Initiate credit to a bank's NRB settlement account.
     * NCHL pays the bank its net credit position from the settlement pool.
     */
    private boolean initiateCreditTransfer(String bankCode, long amountPaisa, String settlementRef) {
        BigDecimal amountNpr = BigDecimal.valueOf(amountPaisa).divide(BigDecimal.valueOf(100));

        log.info("  NRB-IPS CREDIT: NCHL settlement → bank={} | amount=NPR {} | ref={}",
                bankCode, amountNpr, settlementRef);

        // PRODUCTION: Call NRB-IPS API
        // POST {nrbIpsEndpoint}/settlement/credit
        // {
        //   "debitAccount": "{nchlSettlementAccount}",
        //   "creditAccount": "{bankCode}-SETTLEMENT",
        //   "amount": amountNpr,
        //   "currency": "NPR",
        //   "reference": settlementRef,
        //   "purpose": "UPI_NET_SETTLEMENT"
        // }

        // Dev mode: simulate success
        log.info("  NRB-IPS CREDIT simulated (dev mode): {} receives NPR {}", bankCode, amountNpr);
        return true;
    }

    /**
     * Check NRB-IPS gateway availability.
     */
    public boolean isAvailable() {
        log.debug("NRB-IPS availability check: endpoint={}", nrbIpsEndpoint);
        // PRODUCTION: HTTP health check to NRB-IPS endpoint
        // In dev mode: always available
        return true;
    }
}
