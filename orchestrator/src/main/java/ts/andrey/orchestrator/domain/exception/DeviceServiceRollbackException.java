package ts.andrey.orchestrator.domain.exception;

public class DeviceServiceRollbackException extends RuntimeException {

    public DeviceServiceRollbackException() {
        super();
    }

    public DeviceServiceRollbackException(String message) {
        super(message);
    }

    public DeviceServiceRollbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceServiceRollbackException(Throwable cause) {
        super(cause);
    }

    protected DeviceServiceRollbackException(String message, Throwable cause,
                                             boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
