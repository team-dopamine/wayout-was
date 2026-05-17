package kr.wayout.domain.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.problem.Problem;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ProblemDto {

    @Getter
    @AllArgsConstructor
    @Schema(name = "ProblemListResponse")
    public static class List {

        private Long problemId;
        private Integer problemNo;
        private String title;
        private Platform platform;
        private long totalSubmissions;
        private long foundSubmissions;

    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "ProblemSearchResponse")
    public static class Search {

        private Long problemId;
        private Integer problemNo;
        private String title;
        private Platform platform;

        public static Search from(Problem problem) {
            return new Search(
                    problem.getId(),
                    problem.getProblemNo(),
                    problem.getTitle(),
                    problem.getPlatform()
            );
        }

    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "ProblemDetailResponse")
    public static class Detail {

        private Long problemId;
        private Integer problemNo;
        private String title;
        private Platform platform;
        private long totalSubmissions;
        private long foundSubmissions;

    }

}
