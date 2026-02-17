package kr.wayout.domain.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.wayout.domain.submission.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SolutionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private Long problemId;

        private Language language;

        @NotBlank
        private String sourceCode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateResponse {

        private String message;

        public static CreateResponse of(String message) {
            return CreateResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
