package Repositories;

import Models.Answer;
import Models.Questionnaire;
import Models.User;
import Services.DbSessionConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnswerRepository extends CrudRepository<Answer, Long> {

  List<Answer> findAll();
  List<Answer> findByTextualQuestionId(Long textualQuestionId);

  @Query("SELECT COUNT(a) FROM Answer a where a.textualQuestionId = ?1")
  int getNumberOfTextualAnswers(Long textualQuestionId);

  default List<Answer> findByTextualQuestionIdLimited(Long textualQuestionId, int limit, int offset) {

    Session session = DbSessionConfig.dbSession;

    org.hibernate.query.Query<Answer> query = session.createQuery(
            "select a FROM Answer a where a.textualQuestionId = " + textualQuestionId, Answer.class);
    query.setFirstResult(offset);
    query.setMaxResults(limit);
    List<Answer> list = query.list();

    return list;
  }

  List<Answer> findByScaleQuestionId(Long scaleQuestionId);
  List<Answer> findByChoiceQuestionId(Long choiceQuestionId);
  List<Answer> deleteByTextualQuestionId(Long textualQuestionId);
  List<Answer> deleteByScaleQuestionId(Long scaleQuestionId);
  List<Answer> deleteByChoiceQuestionId(Long choiceQuestionId);

  @Modifying
  void deleteAll();
}
