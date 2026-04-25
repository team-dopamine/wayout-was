package kr.wayout.domain.testcase;

import kr.wayout.domain.problem.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Long> {

    boolean existsByProblem(Problem problem);

    List<Testcase> findAllByProblemOrderByIdAsc(Problem problem);
}
