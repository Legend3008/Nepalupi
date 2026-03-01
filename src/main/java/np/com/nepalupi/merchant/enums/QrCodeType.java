package np.com.nepalupi.merchant.enums;

public enum QrCodeType {
    STATIC,   // Fixed QR — no amount, customer enters amount
    DYNAMIC   // Per-transaction QR — pre-filled amount + reference
}
