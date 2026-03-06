package kr.wayout.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode, path, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message) {
        String resolvedMessage = (message == null || message.isBlank())
                ? errorCode.getMessage()
                : message;

        return new ErrorResponse(
                errorCode.getCode(),
                resolvedMessage,
                path,
                LocalDateTime.now()
        );
    }

}
