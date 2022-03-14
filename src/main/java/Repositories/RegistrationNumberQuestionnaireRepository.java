package Repositories;

import Models.Questionnaire;
import Models.RegistrationNumberQuestionnaire;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationNumberQuestionnaireRepository extends CrudRepository<RegistrationNumberQuestionnaire, Long> {

    List<RegistrationNumberQuestionnaire> findAll();

    @Query("SELECT r FROM RegistrationNumberQuestionnaire r where r.registrationNum = ?1 and r.questionnaireId = ?2")
    List<RegistrationNumberQuestionnaire> findByRegNumberAndQuestionnaireId(String regNumber, Long questionnaireId);

    @Query("SELECT COUNT(id) FROM RegistrationNumberQuestionnaire r where r.questionnaireId = ?1")
    Integer getNumberOfFills(Long questionnaireId);

    // @Query("delete FROM RegistrationNumberQuestionnaire r where r.questionnaireId = ?1")
    void deleteByQuestionnaireId(Long questionnaireId);

    @Query("select questionnaireId from RegistrationNumberQuestionnaire r where r.registrationNum = ?1 and r.completedLevel = 0")
    List<Long> findQuestionnaireIdsByRegistrationNumber(String userRegistrationNumber);

    @Modifying
    void deleteAll();
}
