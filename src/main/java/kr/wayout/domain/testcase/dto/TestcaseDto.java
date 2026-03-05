package kr.wayout.domain.testcase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TestcaseDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotNull
        private Long problemId;

        @NotNull
        private String input;

        @NotNull
        private String output;

    }

    @Getter
    @AllArgsConstructor
    public static class CreateResponse {
        private String message;

        public static CreateResponse of(String message) {
            return new CreateResponse(message);
        }
    }

}
