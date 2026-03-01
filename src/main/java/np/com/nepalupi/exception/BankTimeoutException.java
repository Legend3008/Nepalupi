package np.com.nepalupi.exception;

public class BankTimeoutException extends RuntimeException {
    public BankTimeoutException(String message) {
        super(message);
    }

    public BankTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
