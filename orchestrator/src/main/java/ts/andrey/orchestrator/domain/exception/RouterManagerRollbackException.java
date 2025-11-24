package ts.andrey.orchestrator.domain.exception;

public class RouterManagerRollbackException extends RuntimeException {

    public RouterManagerRollbackException() {
        super();
    }

    public RouterManagerRollbackException(String message) {
        super(message);
    }

    public RouterManagerRollbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouterManagerRollbackException(Throwable cause) {
        super(cause);
    }

    protected RouterManagerRollbackException(String message,
                                             Throwable cause,
                                             boolean enableSuppression,
                                             boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
