package kr.wayout.domain.submission.runner;

import kr.wayout.domain.submission.Language;

public interface CounterExampleRunner {

    CounterExampleRunResult run(Long problemId, String sourceCode, Language language);
}
