package Models;

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

    public RegistrationNumberQuestionnaire(Long id, String registrationNum, Long questionnaireId) {
        this.id = id;
        this.registrationNum = registrationNum;
        questionnaireId = questionnaireId;
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
}
