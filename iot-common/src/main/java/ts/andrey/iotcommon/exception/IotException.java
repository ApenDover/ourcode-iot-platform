package ts.andrey.iotcommon.exception;

public class IotException extends RuntimeException {
    public IotException() {
        super();
    }

    public IotException(String message) {
        super(message);
    }

    public IotException(String message, Throwable cause) {
        super(message, cause);
    }

    public IotException(Throwable cause) {
        super(cause);
    }

    protected IotException(String message, Throwable cause,
                           boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
