package kr.wayout.domain.validator;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidatorRepository extends JpaRepository<Validator, Long> {

    Validator findValidatorByProblem(Problem problem);

}
