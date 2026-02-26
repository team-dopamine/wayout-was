package kr.wayout.domain.generator;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratorRepository extends JpaRepository<Generator, Long> {

    Generator findGeneratorByProblem(Problem problem);

}
