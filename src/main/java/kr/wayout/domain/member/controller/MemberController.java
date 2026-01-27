package kr.wayout.domain.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.member.dto.UpdateNicknameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController implements MemberApi {

    private final MemberService memberService;

    @Override
    @PatchMapping("/me")
    public ResponseEntity<?> update(@AuthenticationPrincipal String email, UpdateNicknameDto.Request dto) {
        return ResponseEntity.ok(memberService.changeNickname(email, dto));
    }

}
