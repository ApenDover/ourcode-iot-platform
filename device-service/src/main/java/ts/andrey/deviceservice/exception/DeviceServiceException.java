package ts.andrey.deviceservice.exception;

import org.springframework.http.HttpStatus;

public class DeviceServiceException extends RuntimeException {

    private HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public DeviceServiceException() {
        super();
    }

    public DeviceServiceException(ErrorExceptionMessages errorExceptionMessages, Object... args) {
        super(errorExceptionMessages.format(args));
        status = errorExceptionMessages.getHttpStatus();
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

    protected DeviceServiceException(
            String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
