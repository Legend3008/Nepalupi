package np.com.nepalupi.exception;

/**
 * Thrown when a state machine transition is not allowed.
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
