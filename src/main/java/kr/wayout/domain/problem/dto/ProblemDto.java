package kr.wayout.domain.problem.dto;

import kr.wayout.domain.problem.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ProblemDto {

    @Getter
    @AllArgsConstructor
    public static class List {

        private Long problemId;
        private Integer problemNo;
        private String title;
        private Platform platform;
        private long totalSubmissions;
        private long foundSubmissions;

    }

}
