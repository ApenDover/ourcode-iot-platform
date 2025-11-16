package ts.andrey.orchestrator.domain.exception;

public class OrchestratorException extends RuntimeException {

    public OrchestratorException() {
        super();
    }

    public OrchestratorException(String message) {
        super(message);
    }

    public OrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrchestratorException(Throwable cause) {
        super(cause);
    }

    protected OrchestratorException(String message, Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
