package ts.andrey.eventcollector.exception;

public class EventCollectorException extends RuntimeException {

    public EventCollectorException() {
        super();
    }

    public EventCollectorException(String message) {
        super(message);
    }

    public EventCollectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventCollectorException(Throwable cause) {
        super(cause);
    }

    protected EventCollectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
