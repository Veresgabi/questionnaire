package Repositories;

import Models.Answer;
import Models.Questionnaire;
import Models.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AnswerRepository extends CrudRepository<Answer, Long> {

  List<Answer> findAll();
  List<Answer> findByTextualQuestionId(Long textualQuestionId);

  /* @Query("SELECT a FROM Answer a where a.textualQuestionId = ?1 LIMIT ?2 OFFSET ?3")
  List<Answer> findByTextualQuestionIdLimited(Long textualQuestionId, Long limit, Long offset); */

  default List<String> findByTextualQuestionIdLimited2(Long textualQuestionId, int limit, int offset) {
    Configuration config = new Configuration();
    config.configure();
    // local SessionFactory bean created
    SessionFactory sessionFactory = config.buildSessionFactory();
    Session session = sessionFactory.openSession();

    org.hibernate.query.Query<String> query = session.createQuery("select content FROM Answer a where a.textualQuestionId = " + textualQuestionId, String.class);
    query.setFirstResult(offset);
    query.setMaxResults(limit);
    List<String> list = query.list();
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
