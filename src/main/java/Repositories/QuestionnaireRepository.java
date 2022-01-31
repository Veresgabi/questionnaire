package Repositories;

import Models.Questionnaire;
import Models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QuestionnaireRepository extends CrudRepository<Questionnaire, Long> {

  List<Questionnaire> findAll();

  Questionnaire findQuestionnaireById(Long id);

  @Query("SELECT q FROM Questionnaire q order by q.lastModified desc")
  List<Questionnaire> findAllOrderByLastModDesc();

  @Query("SELECT q FROM Questionnaire q where (q.state = 1 and q.id not in " +
          "(select questionnaireId from RegistrationNumberQuestionnaire r where r.registrationNum = ?1)) or q.state = 3 order by q.deadline")
  List<Questionnaire> findPublishedQuestionnaires(String userRegistrationNumber);

  List<Questionnaire> findQuestionnaireByTitle(String title);
}
