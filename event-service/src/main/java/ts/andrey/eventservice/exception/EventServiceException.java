package ts.andrey.eventservice.exception;

import org.springframework.http.HttpStatus;

public class EventServiceException extends RuntimeException {

    private HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public EventServiceException() {
        super();
    }

    public EventServiceException(String message) {
        super(message);
    }

    public EventServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventServiceException(ErrorExceptionMessages errorExceptionMessages, Object... args) {
        super(errorExceptionMessages.format(args));
        status = errorExceptionMessages.getHttpStatus();
    }

    public EventServiceException(Throwable cause) {
        super(cause);
    }

    protected EventServiceException(String message, Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
