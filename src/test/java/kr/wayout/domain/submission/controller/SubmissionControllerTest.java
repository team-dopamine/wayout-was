package kr.wayout.domain.submission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.submission.Language;
import kr.wayout.domain.submission.SubmissionService;
import kr.wayout.domain.submission.dto.SubmissionDto;
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
                .andExpect(jsonPath("$.content[0].executionTime").value(1.5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
