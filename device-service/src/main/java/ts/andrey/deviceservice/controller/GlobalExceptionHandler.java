package ts.andrey.deviceservice.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.dto.DeviceError;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<DeviceError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return buildDeviceError(
                HttpStatus.NOT_FOUND,
                "Entity not found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DeviceError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildDeviceError(
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeviceError> handleException(Exception ex, HttpServletRequest request) {
        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DeviceServiceException.class)
    public ResponseEntity<DeviceError> handleException(DeviceServiceException ex, HttpServletRequest request) {
        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Process exception",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<DeviceError> buildDeviceError(HttpStatus status, String title, String detail, String instance) {
        DeviceError error = new DeviceError();
        error.setType(URI.create("about:blank"));
        error.setTitle(title);
        error.setStatus(status.value());
        error.setDetail(detail);
        error.setInstance(instance);
        error.setTrace(MDC.get("trace-id"));
        return ResponseEntity.status(status).body(error);
    }
}
