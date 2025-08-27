package ts.andrey.deviceservice.exception;

public class DeviceServiceException extends RuntimeException {

    public DeviceServiceException() {
        super();
    }

    public DeviceServiceException(TextException textException, Object... args) {
        super(textException.format(args));
    }

    public DeviceServiceException(String message) {
        super(message);
    }

    public DeviceServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceServiceException(Throwable cause) {
        super(cause);
    }

    protected DeviceServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
