package kr.wayout.domain.submission.runner;

import kr.wayout.domain.submission.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CounterExampleRunCommand {

    private Long problemId;
    private Long submissionId;
    private String userSourceCode;
    private Language userLanguage;

    private String solutionSourceCode;
    private Language solutionLanguage;

    private String generatorSourceCode;
    private String validatorSourceCode;

    private int maxTrials;
}
