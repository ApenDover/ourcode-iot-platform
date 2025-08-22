package ts.andrey.devicecollector.exception;

public class DeviceCollectorException extends RuntimeException {

    public DeviceCollectorException() {
        super();
    }

    public DeviceCollectorException(String message) {
        super(message);
    }

    public DeviceCollectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceCollectorException(Throwable cause) {
        super(cause);
    }

    protected DeviceCollectorException(String message, Throwable cause,
                                       boolean enableSuppression,
                                       boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
