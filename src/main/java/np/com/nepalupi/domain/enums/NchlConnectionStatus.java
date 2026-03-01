package np.com.nepalupi.domain.enums;

/**
 * NCHL TCP channel connection status.
 */
public enum NchlConnectionStatus {

    /** TCP connection not established */
    DISCONNECTED,

    /** TCP socket connected, sign-on not yet done */
    CONNECTED,

    /** Sign-on 0800 sent, waiting for 0810 */
    SIGNING_ON,

    /** Signed on — ready to send financial messages */
    SIGNED_ON;

    public boolean canSendTransactions() {
        return this == SIGNED_ON;
    }
}
