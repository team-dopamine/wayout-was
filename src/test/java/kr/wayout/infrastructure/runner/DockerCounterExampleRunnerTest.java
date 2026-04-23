package kr.wayout.infrastructure.runner;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class DockerCounterExampleRunnerTest {

    private final DockerCounterExampleRunner runner = new DockerCounterExampleRunner(null, null, null, null);

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("Docker 출력 파일 읽기 - case 파일을 순서대로 읽는다")
    void read_case_outputs_success() throws IOException {
        // given
        Path outputsDir = tempDir.resolve("outputs");
        Files.createDirectories(outputsDir);
        Files.writeString(outputsDir.resolve("case_0.txt"), "first\n", StandardCharsets.UTF_8);
        Files.writeString(outputsDir.resolve("case_1.txt"), "second\n", StandardCharsets.UTF_8);

        // when
        List<String> result = runner.readCaseOutputs(outputsDir, 2);

        // then
        Assertions.assertThat(result).containsExactly("first\n", "second\n");
    }

    @Test
    @DisplayName("Docker 출력 파일 읽기 - 기대한 case 파일이 없으면 기존 메시지로 실패한다")
    void read_case_outputs_missing_file() throws IOException {
        // given
        Path outputsDir = tempDir.resolve("outputs");
        Files.createDirectories(outputsDir);
        Files.writeString(outputsDir.resolve("case_0.txt"), "first\n", StandardCharsets.UTF_8);

        // when & then
        Assertions.assertThatThrownBy(() -> runner.readCaseOutputs(outputsDir, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Docker 실행 결과 파일이 없습니다. file=" + outputsDir.resolve("case_1.txt"));
    }

    @Test
    @DisplayName("Validator 결과 파싱 - exit code를 boolean 결과로 변환한다")
    void parse_validation_results_success() throws IOException {
        // given
        Path outputsDir = tempDir.resolve("outputs");
        Files.createDirectories(outputsDir);

        // when
        List<Boolean> result = runner.parseValidationResults("0\n1 0\n", outputsDir, 3);

        // then
        Assertions.assertThat(result).containsExactly(true, false, true);
    }

    @Test
    @DisplayName("Validator 결과 파싱 - 실패한 case는 경고 로그를 남긴다")
    void parse_validation_results_logs_warn_for_failed_case() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(DockerCounterExampleRunner.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            List<Boolean> result = runner.parseValidationResults("0 1 0", tempDir, 3);

            // then
            Assertions.assertThat(result).containsExactly(true, false, true);
            Assertions.assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains("Validator failed: index=1, exitCode=1");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("Validator 결과 파싱 - 결과 개수가 다르면 기존 메시지로 실패한다")
    void parse_validation_results_count_mismatch() throws IOException {
        // given
        Path outputsDir = tempDir.resolve("outputs");
        Files.createDirectories(outputsDir);

        // when & then
        Assertions.assertThatThrownBy(() -> runner.parseValidationResults("0 1", outputsDir, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validator 결과 개수가 일치하지 않습니다. expected=3 actual=2");
    }

    @Test
    @DisplayName("Validator 결과 파싱 - 숫자가 아닌 exit code면 기존 메시지로 실패한다")
    void parse_validation_results_non_numeric_token() throws IOException {
        // given
        Path outputsDir = tempDir.resolve("outputs");
        Files.createDirectories(outputsDir);

        // when & then
        Assertions.assertThatThrownBy(() -> runner.parseValidationResults("0 nope", outputsDir, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validator exit code 파싱에 실패했습니다. value=nope")
                .hasCauseInstanceOf(NumberFormatException.class);
    }

}
