package np.com.nepalupi.exception;

public class BankNotSupportedException extends RuntimeException {
    public BankNotSupportedException(String message) {
        super(message);
    }
}
