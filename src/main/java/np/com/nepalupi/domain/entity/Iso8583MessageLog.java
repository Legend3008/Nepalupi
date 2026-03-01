package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Logs every ISO 8583 message sent to / received from NCHL.
 * <p>
 * This is your definitive proof of what was communicated with the banking network.
 * NRB auditors will ask to see these records. Append-only — never delete or modify.
 */
@Entity
@Table(name = "iso8583_message_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Iso8583MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Message Type Indicator: 0200, 0210, 0400, 0800, etc. */
    @Column(nullable = false, length = 4)
    private String mti;

    /** OUTBOUND (we sent) or INBOUND (we received) */
    @Column(nullable = false, length = 10)
    private String direction;

    /** Hex-encoded bitmap */
    @Column(length = 64)
    private String bitmap;

    /** Field 3: Processing code (000000=debit, 200000=credit, 400000=reversal) */
    @Column(name = "processing_code", length = 6)
    private String processingCode;

    /** Field 4: Amount in paisa, 12-digit zero-padded */
    @Column(name = "amount_paisa")
    private Long amountPaisa;

    /** Field 11: System Trace Audit Number — 6-digit unique per message */
    @Column(length = 6)
    private String stan;

    /** Field 37: Retrieval Reference Number — how we match req/resp */
    @Column(length = 12)
    private String rrn;

    /** Field 39: Response code ("00" = approved) */
    @Column(name = "response_code", length = 2)
    private String responseCode;

    /** Field 38: Authorization code from bank */
    @Column(name = "auth_code", length = 6)
    private String authCode;

    /** Field 49: Currency code (524 = NPR) */
    @Column(name = "currency_code", length = 3)
    @Builder.Default
    private String currencyCode = "524";

    /** Full raw message in hex for forensic analysis */
    @Column(name = "raw_hex", columnDefinition = "TEXT")
    private String rawHex;

    /** Link to our internal transaction */
    @Column(name = "transaction_id")
    private java.util.UUID transactionId;

    /** Which NCHL channel: PRIMARY or DR */
    @Column(length = 20)
    @Builder.Default
    private String channel = "PRIMARY";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
