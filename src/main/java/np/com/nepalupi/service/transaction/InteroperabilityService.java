package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Section 20.4: Interoperability Standards Service.
 * <p>
 * Ensures Nepal UPI switch is interoperable with:
 * <ul>
 *   <li>EMVCo QR code standards (Merchant-Presented & Consumer-Presented)</li>
 *   <li>ISO 20022 payment messaging (future NCHL integration)</li>
 *   <li>ISO 8583 financial messaging (current bank integration)</li>
 *   <li>Cross-border payment protocols (SAARC corridor)</li>
 *   <li>ConnectIPS Nepal interop</li>
 *   <li>NRB regulatory data exchange format</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteroperabilityService {

    @Value("${nepalupi.interop.emvco.aid:A000000999}")
    private String emvcoAid;

    @Value("${nepalupi.interop.country-code:NP}")
    private String countryCode;

    @Value("${nepalupi.interop.currency-code:524}")
    private String currencyCode; // NPR = 524

    /**
     * Validate that a QR code payload follows EMVCo Merchant-Presented format.
     */
    public EmvCoValidationResult validateEmvCoQr(String qrPayload) {
        List<String> errors = new ArrayList<>();
        Map<String, String> parsedFields = new LinkedHashMap<>();

        try {
            // Parse TLV (Tag-Length-Value) structure
            int index = 0;
            while (index < qrPayload.length() - 4) {
                String tag = qrPayload.substring(index, index + 2);
                int length = Integer.parseInt(qrPayload.substring(index + 2, index + 4));
                if (index + 4 + length > qrPayload.length()) break;
                String value = qrPayload.substring(index + 4, index + 4 + length);
                parsedFields.put(tag, value);
                index += 4 + length;
            }

            // Validate mandatory EMVCo fields
            if (!parsedFields.containsKey("00")) errors.add("Missing Payload Format Indicator (tag 00)");
            else if (!"01".equals(parsedFields.get("00"))) errors.add("Invalid Payload Format Indicator");

            if (!parsedFields.containsKey("01")) errors.add("Missing Point of Initiation (tag 01)");

            if (!parsedFields.containsKey("52")) errors.add("Missing Merchant Category Code (tag 52)");
            if (!parsedFields.containsKey("53")) errors.add("Missing Transaction Currency (tag 53)");
            else if (!currencyCode.equals(parsedFields.get("53"))) {
                errors.add("Currency mismatch: expected " + currencyCode + ", got " + parsedFields.get("53"));
            }

            if (!parsedFields.containsKey("58")) errors.add("Missing Country Code (tag 58)");
            else if (!countryCode.equals(parsedFields.get("58"))) {
                errors.add("Country code mismatch: expected " + countryCode);
            }

            if (!parsedFields.containsKey("59")) errors.add("Missing Merchant Name (tag 59)");
            if (!parsedFields.containsKey("63")) errors.add("Missing CRC (tag 63)");

        } catch (Exception e) {
            errors.add("QR parsing error: " + e.getMessage());
        }

        boolean valid = errors.isEmpty();
        return new EmvCoValidationResult(valid, parsedFields, errors);
    }

    /**
     * Build ISO 20022 pain.001 payment initiation message.
     */
    public String buildIso20022PaymentInitiation(String debtorAccount, String debtorBank,
                                                   String creditorAccount, String creditorBank,
                                                   Long amountPaisa, String currency, String reference) {
        long amountUnits = amountPaisa / 100;
        long amountCents = amountPaisa % 100;
        String amount = amountUnits + "." + String.format("%02d", amountCents);

        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.09">
                  <CstmrCdtTrfInitn>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <NbOfTxs>1</NbOfTxs>
                      <CtrlSum>%s</CtrlSum>
                    </GrpHdr>
                    <PmtInf>
                      <PmtInfId>NPI-%s</PmtInfId>
                      <PmtMtd>TRF</PmtMtd>
                      <DbtrAcct><Id><IBAN>%s</IBAN></Id></DbtrAcct>
                      <DbtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></DbtrAgt>
                      <CdtTrfTxInf>
                        <Amt><InstdAmt Ccy="%s">%s</InstdAmt></Amt>
                        <CdtrAcct><Id><IBAN>%s</IBAN></Id></CdtrAcct>
                        <CdtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></CdtrAgt>
                        <RmtInf><Ustrd>%s</Ustrd></RmtInf>
                      </CdtTrfTxInf>
                    </PmtInf>
                  </CstmrCdtTrfInitn>
                </Document>
                """,
                UUID.randomUUID(), java.time.Instant.now(),
                amount, reference,
                debtorAccount, debtorBank,
                currency, amount,
                creditorAccount, creditorBank,
                reference);
    }

    /**
     * Map NPI transaction status to ISO 20022 status code.
     */
    public String mapToIso20022Status(String npiStatus) {
        return switch (npiStatus) {
            case "INITIATED"  -> "PDNG";  // Pending
            case "DEBITED"    -> "ACSP";  // Accepted Settlement in Process
            case "COMPLETED"  -> "ACSC";  // Accepted Settlement Completed
            case "FAILED"     -> "RJCT";  // Rejected
            case "REVERSED"   -> "RJCT";  // Rejected (reversed)
            case "EXPIRED"    -> "CANC";  // Cancelled
            default           -> "PDNG";
        };
    }

    /**
     * Build NRB regulatory data exchange format.
     */
    public Map<String, Object> buildNrbReport(String reportType, List<Map<String, Object>> transactions) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", reportType);
        report.put("switchOperator", "NPI");
        report.put("countryCode", countryCode);
        report.put("currencyCode", currencyCode);
        report.put("reportDate", java.time.LocalDate.now().toString());
        report.put("totalTransactions", transactions.size());

        long totalVolume = transactions.stream()
                .mapToLong(t -> (Long) t.getOrDefault("amountPaisa", 0L))
                .sum();
        report.put("totalVolumePaisa", totalVolume);
        report.put("totalVolumeNPR", totalVolume / 100.0);
        report.put("transactions", transactions);

        return report;
    }

    /**
     * Get interoperability capabilities of the NPI switch.
     */
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "emvco", Map.of(
                        "merchantPresented", true,
                        "consumerPresented", true,
                        "aid", emvcoAid
                ),
                "iso8583", Map.of(
                        "version", "1987",
                        "macSupport", "HMAC-SHA256",
                        "supportedMTIs", List.of("0100", "0110", "0200", "0210", "0400", "0410", "0800", "0810")
                ),
                "iso20022", Map.of(
                        "pain001", true,
                        "pain002", true,
                        "camt053", true
                ),
                "qrStandards", Map.of(
                        "emvco", true,
                        "nepalQr", true,
                        "signedQr", true
                ),
                "crossBorder", Map.of(
                        "inr", "via NCHL bilateral",
                        "usd", "via SWIFT/correspondent",
                        "saarcCorridor", "planned"
                ),
                "regulatoryCompliance", Map.of(
                        "nrb", true,
                        "fatf", true,
                        "sebon", true
                )
        );
    }

    // --- Records ---

    public record EmvCoValidationResult(
            boolean valid,
            Map<String, String> parsedFields,
            List<String> errors
    ) {}
}
