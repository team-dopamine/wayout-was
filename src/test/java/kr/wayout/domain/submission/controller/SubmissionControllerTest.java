package kr.wayout.domain.submission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.submission.Language;
import kr.wayout.domain.submission.SubmissionService;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
import kr.wayout.global.auth.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("반례 탐색 요청 API - 익명 사용자도 호출 가능")
    void createCounterExample_success_for_anonymous() throws Exception {
        // given
        SubmissionDto.CreateCounterExampleRequest request =
                new SubmissionDto.CreateCounterExampleRequest(1L, Language.JAVA, "public class Main {}", true);
        SubmissionDto.CreateCounterExampleResponse response = SubmissionDto.CreateCounterExampleResponse.builder()
                .status("PENDING")
                .message("반례 탐색 요청이 접수되었습니다.")
                .build();

        given(submissionService.createCounterExample(isNull(), any(SubmissionDto.CreateCounterExampleRequest.class)))
                .willReturn(response);

        // when & then
                mockMvc.perform(post("/submissions/counter-examples")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("제출 목록 조회 API - 회원/비회원 제출 이력을 페이지 형태로 반환한다")
    void list_success() throws Exception {
        // given
        SubmissionDto.ListResponse item = SubmissionDto.ListResponse.builder()
                .id(1L)
                .nickname("익명")
                .title("A+B")
                .language(Language.JAVA)
                .platform(Platform.SWEA)
                .executionTime(1.5)
                .createdAt(LocalDateTime.of(2026, 3, 8, 12, 0))
                .build();

        PageRequest pageable = PageRequest.of(0, 8);
        given(submissionService.list(any())).willReturn(new PageImpl<>(List.of(item), pageable, 1));

        // when & then
        mockMvc.perform(get("/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nickname").value("익명"))
                .andExpect(jsonPath("$.content[0].title").value("A+B"))
                .andExpect(jsonPath("$.content[0].language").value("JAVA"))
                .andExpect(jsonPath("$.content[0].platform").value("SWEA"))
                .andExpect(jsonPath("$.content[0].executionTime").value(1.5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("특정 문제 제출 목록 조회 API - 해당 문제 제출 이력을 페이지 형태로 반환한다")
    void list_by_problem_success() throws Exception {
        // given
        SubmissionDto.ListResponse item = SubmissionDto.ListResponse.builder()
                .id(10L)
                .nickname("Jsplix")
                .title("A+B")
                .language(Language.CPP)
                .platform(Platform.SWEA)
                .executionTime(0.7)
                .createdAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build();

        PageRequest pageable = PageRequest.of(0, 8);
        given(submissionService.listByProblemId(any(), eq(1L)))
                .willReturn(new PageImpl<>(List.of(item), pageable, 1));

        // when & then
        mockMvc.perform(get("/problems/{problemId}/submissions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].nickname").value("Jsplix"))
                .andExpect(jsonPath("$.content[0].title").value("A+B"))
                .andExpect(jsonPath("$.content[0].language").value("CPP"))
                .andExpect(jsonPath("$.content[0].platform").value("SWEA"))
                .andExpect(jsonPath("$.content[0].executionTime").value(0.7))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("특정 문제 제출 목록 조회 API - 존재하지 않는 문제면 404를 반환한다")
    void list_by_problem_not_found() throws Exception {
        // given
        given(submissionService.listByProblemId(any(), eq(999L)))
                .willThrow(new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/problems/{problemId}/submissions", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("B002"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 문제입니다."))
                .andExpect(jsonPath("$.path").value("/problems/999/submissions"));
    }

    @Test
    @DisplayName("제출 기록 상세 조회 API - 상세 정보를 반환한다")
    void detail_success() throws Exception {
        // given
        SubmissionDto.CounterExampleCase counterExample = SubmissionDto.CounterExampleCase.builder()
                .input("3\n1 2 3")
                .expectedOutput("6")
                .actualOutput("5")
                .build();

        SubmissionDto.Detail response = SubmissionDto.Detail.builder()
                .id(1L)
                .problemNo(1001L)
                .title("A+B")
                .sourceCode("public class Main {}")
                .counterExamples(List.of(counterExample))
                .language(Language.JAVA)
                .executionTime(1.23)
                .createdAt(LocalDateTime.of(2026, 3, 23, 11, 0))
                .build();

        given(submissionService.detail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/submissions/{submissionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.problemNo").value(1001))
                .andExpect(jsonPath("$.title").value("A+B"))
                .andExpect(jsonPath("$.sourceCode").value("public class Main {}"))
                .andExpect(jsonPath("$.language").value("JAVA"))
                .andExpect(jsonPath("$.executionTime").value(1.23))
                .andExpect(jsonPath("$.counterExamples[0].input").value("3\n1 2 3"))
                .andExpect(jsonPath("$.counterExamples[0].expectedOutput").value("6"))
                .andExpect(jsonPath("$.counterExamples[0].actualOutput").value("5"));
    }

    @Test
    @DisplayName("제출 기록 상세 조회 API - 존재하지 않는 제출이면 404를 반환한다")
    void detail_not_found() throws Exception {
        // given
        given(submissionService.detail(999L))
                .willThrow(new CustomBusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/submissions/{submissionId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("B004"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 제출 기록입니다."))
                .andExpect(jsonPath("$.path").value("/submissions/999"));
    }

    @Test
    @DisplayName("제출 기록 상세 조회 API - 비공개 제출이면 403을 반환한다")
    void detail_forbidden_when_not_opened() throws Exception {
        // given
        given(submissionService.detail(2L))
                .willThrow(new CustomBusinessException(ErrorCode.SUBMISSION_NOT_OPENED));

        // when & then
        mockMvc.perform(get("/submissions/{submissionId}", 2L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("B005"))
                .andExpect(jsonPath("$.message").value("공개가 허용되지 않은 제출 기록입니다."))
                .andExpect(jsonPath("$.path").value("/submissions/2"));
    }
}
