package ts.andrey.failedeventsprocessor.exception;

public class FailedEventException extends RuntimeException {

    public FailedEventException() {
        super();
    }

    public FailedEventException(String message) {
        super(message);
    }

    public FailedEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedEventException(Throwable cause) {
        super(cause);
    }

    protected FailedEventException(String message, Throwable cause,
                                   boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
