package Models;

import Utils.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "TextualQuestions")
public class TextualQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Number", nullable = false)
    private Integer number;

    @Column(name = "Type", nullable = false)
    private Enums.Type type;

    @Column(name = "isOptional", nullable = false)
    private boolean isOptional;

    @Column(name = "isUnionMembersOnly")
    private boolean isUnionMembersOnly;

    @Column(name = "Question", nullable = false, length = 1000)
    private String question;

    @Transient
    private boolean isCompletedByCurrentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    @JsonIgnore
    private Questionnaire questionnaire;

    /* @OneToMany(cascade= CascadeType.ALL)
    @JoinColumn(name="Text_Question_Id")
    @JsonIgnore */
    @Transient
    private List<Answer> answers;

    public TextualQuestion(Long id, Integer number, Enums.Type type, boolean isOptional,
                           boolean isUnionMembersOnly, String question, boolean isCompletedByCurrentUser,
                           Questionnaire questionnaire, List<Answer> answers) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.isOptional = isOptional;
        this.isUnionMembersOnly = isUnionMembersOnly;
        this.question = question;
        this.isCompletedByCurrentUser = isCompletedByCurrentUser;
        this.questionnaire = questionnaire;
        this.answers = answers;
    }

    public TextualQuestion() { }

    public Long getId() {
        return id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enums.Type getType() {
        return type;
    }

    public void setType(Enums.Type type) {
        this.type = type;
    }

    public boolean getOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
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

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public boolean isCompletedByCurrentUser() {
        return isCompletedByCurrentUser;
    }

    public void setCompletedByCurrentUser(boolean completedByCurrentUser) {
        isCompletedByCurrentUser = completedByCurrentUser;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }
}
