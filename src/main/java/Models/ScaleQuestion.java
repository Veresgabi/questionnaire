package Models;

import Utils.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "ScaleQuestions")
public class ScaleQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Number", nullable = false)
    private Integer number;

    @Column(name = "Type", nullable = false)
    private Enums.Type type;

    @Column(name = "isUnionMembersOnly")
    private boolean isUnionMembersOnly;

    @Column(name = "Question", nullable = false, length = 1000)
    private String question;

    @Column(name = "ScaleMinNum", nullable = false)
    private Integer scaleMinNumber;

    @Column(name = "ScaleMaxNum", nullable = false)
    private Integer scaleMaxNumber;

    @Column(name = "completion")
    private Integer completion;

    @Transient
    private boolean isCompletedByCurrentUser;

    /* @OneToMany(cascade= CascadeType.ALL)
    @JoinColumn(name="Scale_Question_Id")
    @JsonIgnore
    private List<Answer> answers; */

    @Column(name = "averageRate", length = 100)
    private String averageRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    @JsonIgnore
    private Questionnaire questionnaire;

    public ScaleQuestion(Long id, Integer number, Enums.Type type, boolean isUnionMembersOnly,
                         String question, Integer scaleMinNumber, Integer scaleMaxNumber,
                         Questionnaire questionnaire, boolean isCompletedByCurrentUser, int completion, String averageRate) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.isUnionMembersOnly = isUnionMembersOnly;
        this.question = question;
        this.scaleMinNumber = scaleMinNumber;
        this.scaleMaxNumber = scaleMaxNumber;
        // this.answers = answers;
        this.questionnaire = questionnaire;
        this.completion = completion;
        this.isCompletedByCurrentUser = isCompletedByCurrentUser;
        this.averageRate = averageRate;
    }

    public ScaleQuestion() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Enums.Type getType() {
        return type;
    }

    public void setType(Enums.Type type) {
        this.type = type;
    }

    public boolean isUnionMembersOnly() {
        return isUnionMembersOnly;
    }

    public void setUnionMembersOnly(boolean unionMembersOnly) {
        isUnionMembersOnly = unionMembersOnly;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getScaleMinNumber() {
        return scaleMinNumber;
    }

    public void setScaleMinNumber(Integer scaleMinNumber) {
        this.scaleMinNumber = scaleMinNumber;
    }

    public Integer getScaleMaxNumber() {
        return scaleMaxNumber;
    }

    public void setScaleMaxNumber(Integer scaleMaxNumber) {
        this.scaleMaxNumber = scaleMaxNumber;
    }

    /* public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    } */

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public boolean isCompletedByCurrentUser() {
        return isCompletedByCurrentUser;
    }

    public void setCompletedByCurrentUser(boolean completedByCurrentUser) {
        isCompletedByCurrentUser = completedByCurrentUser;
    }

    public Integer getCompletion() {
        return completion;
    }

    public void setCompletion(Integer completion) {
        this.completion = completion;
    }

    public String getAverageRate() {
        return averageRate;
    }

    public void setAverageRate(String averageRate) {
        this.averageRate = averageRate;
    }
}
