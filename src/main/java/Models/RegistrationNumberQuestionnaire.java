package Models;

import Utils.Enums;

import javax.persistence.*;

@Entity
@Table(name = "RegistrationNumber_Questionnaire")
public class RegistrationNumberQuestionnaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "RegistrationNumber", nullable = false)
    private String registrationNum;

    @Column(name = "QuestionnaireId", nullable = false)
    private Long questionnaireId;

    @Column(name = "CompletedLevel")
    private Enums.CompletedLevel completedLevel;

    public RegistrationNumberQuestionnaire(Long id, String registrationNum, Long questionnaireId,
                                           Enums.CompletedLevel completedLevel) {
        this.id = id;
        this.registrationNum = registrationNum;
        this.questionnaireId = questionnaireId;
        this.completedLevel = completedLevel;
    }

    public RegistrationNumberQuestionnaire() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNum() {
        return registrationNum;
    }

    public void setRegistrationNum(String registrationNum) {
        this.registrationNum = registrationNum;
    }

    public Long getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(Long questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public Enums.CompletedLevel getCompletedLevel() {
        return completedLevel;
    }

    public void setCompletedLevel(Enums.CompletedLevel completedLevel) {
        this.completedLevel = completedLevel;
    }
}
