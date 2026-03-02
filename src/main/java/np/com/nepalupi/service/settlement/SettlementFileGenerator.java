package np.com.nepalupi.service.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SettlementReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Settlement File Generator — produces NCHL-compatible settlement files.
 * <p>
 * Section 3.5 of the UPI architecture spec:
 * - NCHL calculates net positions per bank
 * - Settlement files are generated in NCHL fixed-width text format
 * - Files are delivered via SFTP to the NCHL settlement gateway
 * - An ISO 20022 XML variant is also produced for NRB reporting
 * <p>
 * File format (NCHL Settlement Text):
 * <pre>
 * HDR|NEPAL-UPI|{date}|{batchId}|{bankCount}|{totalVolume}
 * NET|{bankCode}|{direction}|{amountPaisa}|{txnCount}
 * NET|{bankCode}|{direction}|{amountPaisa}|{txnCount}
 * TRL|{totalRecords}|{netZeroCheck}
 * </pre>
 * <p>
 * ISO 20022 XML (pain.002 — settlement status):
 * Simplified variant for NRB regulatory reporting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementFileGenerator {

    @Value("${nepalupi.settlement.file-dir:./settlement-files}")
    private String fileDirectory;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate NCHL-compatible fixed-width settlement text file.
     *
     * @param report       The settlement report with net positions
     * @param netPositions Per-bank net positions (positive = owes, negative = owed)
     * @return Path to the generated file
     */
    public Path generateNchlTextFile(SettlementReport report, Map<String, Long> netPositions) {
        String batchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String dateStr = report.getSettlementDate().format(DATE_FMT);

        StringBuilder sb = new StringBuilder();

        // Header record
        sb.append(String.format("HDR|NEPAL-UPI|%s|%s|%d|%d%n",
                dateStr, batchId, netPositions.size(), report.getTotalVolumePaisa()));

        // Net position records
        long netZeroCheck = 0;
        for (Map.Entry<String, Long> entry : netPositions.entrySet()) {
            String bankCode = entry.getKey();
            long position = entry.getValue();
            String direction = position > 0 ? "PAY" : "RECEIVE";
            long absAmount = Math.abs(position);
            netZeroCheck += position;

            sb.append(String.format("NET|%-10s|%-7s|%015d|%06d%n",
                    bankCode, direction, absAmount, 0)); // txnCount per bank not tracked at report level
        }

        // Trailer record — net-zero check (sum must be 0 for balanced settlement)
        sb.append(String.format("TRL|%d|%d%n", netPositions.size(), netZeroCheck));

        // Write to file
        Path filePath = Paths.get(fileDirectory,
                String.format("SETTLEMENT_%s_%s.txt", dateStr, batchId));
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
            log.info("Settlement text file generated: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write settlement file: {}", e.getMessage());
            throw new RuntimeException("Settlement file generation failed", e);
        }

        return filePath;
    }

    /**
     * Generate ISO 20022 XML settlement report for NRB regulatory submission.
     * Based on pain.002.001.03 (Payment Status Report) schema.
     *
     * @param report       The settlement report
     * @param netPositions Per-bank net positions
     * @return Path to the generated XML file
     */
    public Path generateIso20022Xml(SettlementReport report, Map<String, Long> netPositions) {
        String dateStr = report.getSettlementDate().format(DATE_FMT);
        String msgId = "NUPI-" + dateStr + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.002.001.03\">\n");
        xml.append("  <CstmrPmtStsRpt>\n");

        // Group Header
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(msgId).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(Instant.now().toString()).append("</CreDtTm>\n");
        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>Nepal UPI Settlement Switch</Nm>\n");
        xml.append("        <Id><OrgId><BICOrBEI>NCHLNPKA</BICOrBEI></OrgId></Id>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");

        // Original Group Information
        xml.append("    <OrgnlGrpInfAndSts>\n");
        xml.append("      <OrgnlMsgId>SETTLEMENT-").append(dateStr).append("</OrgnlMsgId>\n");
        xml.append("      <OrgnlMsgNmId>pain.001.001.03</OrgnlMsgNmId>\n");
        xml.append("      <GrpSts>ACCP</GrpSts>\n");
        xml.append("      <NbOfTxsPerSts>\n");
        xml.append("        <DtldNbOfTxs>").append(report.getTotalTransactions()).append("</DtldNbOfTxs>\n");
        xml.append("        <DtldSts>ACCP</DtldSts>\n");
        xml.append("        <DtldCtrlSum>").append(String.format("%.2f", report.getTotalVolumePaisa() / 100.0)).append("</DtldCtrlSum>\n");
        xml.append("      </NbOfTxsPerSts>\n");
        xml.append("    </OrgnlGrpInfAndSts>\n");

        // Per-bank settlement entries
        for (Map.Entry<String, Long> entry : netPositions.entrySet()) {
            String bankCode = entry.getKey();
            long position = entry.getValue();
            String direction = position > 0 ? "DEBIT" : "CREDIT";
            long absAmount = Math.abs(position);

            xml.append("    <OrgnlPmtInfAndSts>\n");
            xml.append("      <OrgnlPmtInfId>").append(bankCode).append("-").append(dateStr).append("</OrgnlPmtInfId>\n");
            xml.append("      <PmtInfSts>ACCP</PmtInfSts>\n");
            xml.append("      <TxInfAndSts>\n");
            xml.append("        <StsId>").append(bankCode).append("-NET</StsId>\n");
            xml.append("        <OrgnlEndToEndId>NET-").append(bankCode).append("</OrgnlEndToEndId>\n");
            xml.append("        <TxSts>ACCP</TxSts>\n");
            xml.append("        <OrgnlTxRef>\n");
            xml.append("          <Amt><InstdAmt Ccy=\"NPR\">").append(String.format("%.2f", absAmount / 100.0)).append("</InstdAmt></Amt>\n");
            xml.append("          <CdtDbtInd>").append(direction).append("</CdtDbtInd>\n");
            xml.append("        </OrgnlTxRef>\n");
            xml.append("      </TxInfAndSts>\n");
            xml.append("    </OrgnlPmtInfAndSts>\n");
        }

        xml.append("  </CstmrPmtStsRpt>\n");
        xml.append("</Document>\n");

        // Write to file
        Path filePath = Paths.get(fileDirectory,
                String.format("SETTLEMENT_%s_ISO20022.xml", dateStr));
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, xml.toString(), StandardCharsets.UTF_8);
            log.info("ISO 20022 settlement XML generated: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write ISO 20022 XML: {}", e.getMessage());
            throw new RuntimeException("ISO 20022 XML generation failed", e);
        }

        return filePath;
    }
}
