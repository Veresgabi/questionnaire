package Repositories;

import Models.Answer;
import Models.Questionnaire;
import Models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnswerRepository extends CrudRepository<Answer, Long> {

  List<Answer> findAll();
  List<Answer> findByTextualQuestionId(Long textualQuestionId);
  List<Answer> findByScaleQuestionId(Long scaleQuestionId);
  List<Answer> findByChoiceQuestionId(Long choiceQuestionId);
  List<Answer> deleteByTextualQuestionId(Long textualQuestionId);
  List<Answer> deleteByScaleQuestionId(Long scaleQuestionId);
  List<Answer> deleteByChoiceQuestionId(Long choiceQuestionId);

  @Modifying
  void deleteAll();
}
