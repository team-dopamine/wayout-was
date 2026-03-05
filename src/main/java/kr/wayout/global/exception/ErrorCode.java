package kr.wayout.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // A: 인증 관련 오류
    UNAUTHORIZED_ACCESS("A001", HttpStatus.UNAUTHORIZED, "인증이 필요한 접근입니다."),
    INVALID_REFRESH_TOKEN("A002", HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND("A003", HttpStatus.UNAUTHORIZED, "Refresh Token이 존재하지 않습니다."),
    MEMBER_RESTORE_FAILED("A004", HttpStatus.FORBIDDEN, "탈퇴 후 30일이 경과하여 복구할 수 없습니다."),

    // B: 비즈니스 로직 오류
    MEMBER_NOT_FOUND("B001", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    PROBLEM_NOT_FOUND("B002", HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."),
    MEMBER_ALREADY_DELETED("B001", HttpStatus.CONFLICT, "이미 삭제 된 사용자입니다."),

    // V: 요청 관련 오류
    INVALID_VALUE("V001", HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    INVALID_TESTCASE_INPUT("V002", HttpStatus.BAD_REQUEST, "문제 조건에 맞지 않는 입력입니다."),

    // Z: 서버(시스템) 오류
    INTERNAL_SERVER_ERROR("Z001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생하였습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

}
