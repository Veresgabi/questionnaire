package Repositories;

import Models.RegistrationNumberQuestionnaire;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RegistrationNumberQuestionnaireRepository extends CrudRepository<RegistrationNumberQuestionnaire, Long> {

    @Query("SELECT r FROM RegistrationNumberQuestionnaire r where r.registrationNum = ?1 and r.questionnaireId = ?2")
    List<RegistrationNumberQuestionnaire> findByRegNumberAndQuestionnaireId(String regNumber, Long questionnaireId);

    @Query("SELECT COUNT(id) FROM RegistrationNumberQuestionnaire r where r.questionnaireId = ?1")
    Integer getNumberOfFills(Long questionnaireId);
}
