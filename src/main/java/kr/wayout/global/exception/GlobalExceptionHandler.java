package kr.wayout.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(CustomBusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        logException(exception, "WARN", request);
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        logException(exception, "ERROR", request);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        logException(exception, "WARN", request);
        String message = Optional.ofNullable(exception.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_VALUE, request.getRequestURI(), message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        logException(exception, "WARN", request);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_VALUE, request.getRequestURI(), exception.getMessage()));
    }

    private void logException(Exception exception, String logLevel, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (exception instanceof MethodArgumentNotValidException e) {
            MethodParameter param = e.getParameter();
            FieldError fieldError = e.getBindingResult().getFieldError();

            String controller = param.getContainingClass().getSimpleName();
            String method = param.getMethod().getName();

            String field = fieldError != null ? fieldError.getField() : "unknown";
            String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";

            String logMessage = String.format(
                    "Validation failed | path=%s | controller=%s | method=%s | field=%s | message=%s",
                    path, controller, method, field, message
            );

            log(logLevel, logMessage);
        } else {
            String location = exception.getStackTrace().length > 0
                    ? exception.getStackTrace()[0].toString()
                    : "UnknownLocation";

            log(logLevel,
                    "Exception occurred | path={} | location={} | message={}",
                    path,
                    location,
                    exception.getMessage()
            );
        }
    }

    private void log(String level, String format, Object... args) {
        switch (level) {
            case "ERROR" -> log.error(format, args);
            case "WARN" -> log.warn(format, args);
            case "INFO" -> log.info(format, args);
            case "DEBUG" -> log.debug(format, args);
            default -> log.info(format, args);
        }
    }

}
