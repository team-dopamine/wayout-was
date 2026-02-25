package kr.wayout.domain.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.wayout.domain.submission.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SubmissionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCounterExampleRequest {

        @NotNull
        private Long problemId;

        @NotNull
        private Language language;

        @NotBlank
        private String sourceCode;

        @NotNull
        private Boolean isOpen;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateCounterExampleResponse {
        private String status;
        private String message;
    }
}
