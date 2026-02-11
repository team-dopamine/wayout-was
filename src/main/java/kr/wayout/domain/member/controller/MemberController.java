package kr.wayout.domain.member.controller;

import jakarta.validation.Valid;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.member.dto.NicknameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController implements MemberApi {

    private final MemberService memberService;

    @Override
    @PatchMapping("/me")
    public ResponseEntity<?> update(@AuthenticationPrincipal String email, @Valid @RequestBody NicknameDto.UpdateRequest dto) {
        return ResponseEntity.ok(memberService.changeNickname(email, dto));
    }

    @Override
    @GetMapping("/me/nickname")
    public ResponseEntity<?> getNickname(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(memberService.getNickname(email));
    }
}
