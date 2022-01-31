package DTOs;

import Models.Answer;
import Models.Questionnaire;
import Models.User;

import java.util.List;
import java.util.UUID;

public class QuestionnaireDTO extends AbstractDTO {
    private Questionnaire questionnaire;
    private List<Questionnaire> questionnaireList;
    private List<Answer> answers;


    public QuestionnaireDTO(Questionnaire questionnaire, List<Questionnaire> questionnaireList,
                            List<Answer> answers) {
        this.questionnaire = questionnaire;
        this.questionnaireList = questionnaireList;
        this.answers = answers;
    }

    public QuestionnaireDTO() {
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public List<Questionnaire> getQuestionnaireList() {
        return questionnaireList;
    }

    public void setQuestionnaireList(List<Questionnaire> questionnaireList) {
        this.questionnaireList = questionnaireList;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
}
