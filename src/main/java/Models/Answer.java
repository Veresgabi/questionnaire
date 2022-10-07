package Models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Content", length = 5000)
    private String content;

    @Column(name = "textual_question_id")
    private Long textualQuestionId;

    @Column(name = "scale_question_id")
    private Long scaleQuestionId;

    @Column(name = "choice_question_id")
    private Long choiceQuestionId;

    @Column(name = "isUnionMembersOnly")
    private Boolean isUnionMembersOnly;

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="answer_Id")
    private List<ChoiceAnswer> choiceAnswers = new ArrayList<>();

    /* @ManyToOne
    @JoinColumn(name="Text_Question_Id")
    private TextualQuestion textualQuestion;

    @ManyToOne
    @JoinColumn(name="Scale_Question_Id")
    private ScaleQuestion scaleQuestion;

    @ManyToOne
    @JoinColumn(name="Choice_Question_Id")
    private ChoiceQuestion choiceQuestion; */


    public Answer(Long id, String content, Long textualQuestionId, Long scaleQuestionId,
                  Long choiceQuestionId, Boolean isUnionMembersOnly, List<ChoiceAnswer> choiceAnswers) {
        this.id = id;
        this.content = content;
        // this.textualQuestion = question;
        // this.scaleQuestion = scaleQuestion;
        // this.choiceQuestion = choiceQuestion;
        this.textualQuestionId = textualQuestionId;
        this.scaleQuestionId = scaleQuestionId;
        this.choiceQuestionId = choiceQuestionId;
        this.isUnionMembersOnly = isUnionMembersOnly;
        this.choiceAnswers = choiceAnswers;
    }

    public Answer() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /* public TextualQuestion getTextualQuestion() {
        return textualQuestion;
    }

    public void setTextualQuestion(TextualQuestion textualQuestion) {
        this.textualQuestion = textualQuestion;
    }

    public ScaleQuestion getScaleQuestion() {
        return scaleQuestion;
    }

    public void setScaleQuestion(ScaleQuestion scaleQuestion) {
        this.scaleQuestion = scaleQuestion;
    }

    public ChoiceQuestion getChoiceQuestion() {
        return choiceQuestion;
    }

    public void setChoiceQuestion(ChoiceQuestion choiceQuestion) {
        this.choiceQuestion = choiceQuestion;
    } */

    public Long getTextualQuestionId() {
        return textualQuestionId;
    }

    public void setTextualQuestionId(Long textualQuestionId) {
        this.textualQuestionId = textualQuestionId;
    }

    public Long getScaleQuestionId() {
        return scaleQuestionId;
    }

    public void setScaleQuestionId(Long scaleQuestionId) {
        this.scaleQuestionId = scaleQuestionId;
    }

    public Long getChoiceQuestionId() {
        return choiceQuestionId;
    }

    public void setChoiceQuestionId(Long choiceQuestionId) {
        this.choiceQuestionId = choiceQuestionId;
    }

    public Boolean isUnionMembersOnly() {
        return isUnionMembersOnly;
    }

    public void setUnionMembersOnly(Boolean unionMembersOnly) {
        isUnionMembersOnly = unionMembersOnly;
    }

    public List<ChoiceAnswer> getChoiceAnswers() {
        return choiceAnswers;
    }

    public void setChoiceAnswers(List<ChoiceAnswer> choiceAnswers) {
        this.choiceAnswers = choiceAnswers;
    }
}
