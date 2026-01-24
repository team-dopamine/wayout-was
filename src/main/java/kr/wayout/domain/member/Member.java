package kr.wayout.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import kr.wayout.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 64)
    private String email;

    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    @Column(name = "role", nullable = false)
    private Role role;

    @Builder
    public Member(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

}
