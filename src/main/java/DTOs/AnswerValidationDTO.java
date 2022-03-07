package DTOs;

import Models.Answer;
import Models.ChoiceQuestion;
import Models.ScaleQuestion;
import Models.TextualQuestion;
import Utils.Enums;

import java.util.List;

public class AnswerValidationDTO {
    private List<Answer> validAnswers;
    private List<TextualQuestion> missedTextualQuestions;
    private List<ScaleQuestion> missedScaleQuestions;
    private List<ChoiceQuestion> missedChoiceQuestions;

    public AnswerValidationDTO(List<Answer> validAnswers, List<TextualQuestion> missedTextualQuestions,
                               List<ScaleQuestion> missedScaleQuestions, List<ChoiceQuestion> missedChoiceQuestions) {
        this.validAnswers = validAnswers;
        this.missedTextualQuestions = missedTextualQuestions;
        this.missedScaleQuestions = missedScaleQuestions;
        this.missedChoiceQuestions = missedChoiceQuestions;
    }

    public AnswerValidationDTO() { }

    public List<Answer> getValidAnswers() {
        return validAnswers;
    }

    public void setValidAnswers(List<Answer> validAnswers) {
        this.validAnswers = validAnswers;
    }

    public List<TextualQuestion> getMissedTextualQuestions() {
        return missedTextualQuestions;
    }

    public void setMissedTextualQuestions(List<TextualQuestion> missedTextualQuestions) {
        this.missedTextualQuestions = missedTextualQuestions;
    }

    public List<ScaleQuestion> getMissedScaleQuestions() {
        return missedScaleQuestions;
    }

    public void setMissedScaleQuestions(List<ScaleQuestion> missedScaleQuestions) {
        this.missedScaleQuestions = missedScaleQuestions;
    }

    public List<ChoiceQuestion> getMissedChoiceQuestions() {
        return missedChoiceQuestions;
    }

    public void setMissedChoiceQuestions(List<ChoiceQuestion> missedChoiceQuestions) {
        this.missedChoiceQuestions = missedChoiceQuestions;
    }
}
