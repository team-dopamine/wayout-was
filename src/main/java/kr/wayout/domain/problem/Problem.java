package kr.wayout.domain.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import kr.wayout.domain.validator.Validator;
import kr.wayout.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

    @Column(name = "problem_no", nullable = false)
    private Integer problemNo;

    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Column(name = "platform", nullable = false)
    private Platform platform;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limits", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> limits;

}
