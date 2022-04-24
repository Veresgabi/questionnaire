package Models;

import Utils.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ChoiceQuestions")
public class ChoiceQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Number", nullable = false)
    private Integer number;

    @Column(name = "Type", nullable = false)
    private Enums.Type type;

    @Column(name = "MultipleChoiceEnabled")
    private boolean isMultipleChoiceEnabled;

    @Column(name = "isUnionMembersOnly")
    private boolean isUnionMembersOnly;

    @Column(name = "Question", nullable = false, length = 1000)
    private String question;

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="choice_quest_Id")
    @OrderBy("Mark")
    private List<Choice> choices = new ArrayList<>();

    @Column(name = "completion")
    private Integer completion;

    @Transient
    private boolean isCompletedByCurrentUser;

    /* @OneToMany(cascade= CascadeType.ALL)
    @JoinColumn(name="Choice_Id")
    @JsonIgnore
    private List<Answer> answers; */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    @JsonIgnore
    private Questionnaire questionnaire;

    public ChoiceQuestion(Long id, Integer number, Enums.Type type, boolean isMultipleChoiceEnabled,
                          boolean isUnionMembersOnly, String question, List<Choice> choices, int completion,
                          boolean isCompletedByCurrentUser, Questionnaire questionnaire) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.isMultipleChoiceEnabled = isMultipleChoiceEnabled;
        this.question = question;
        this.choices = choices;
        // this.answers = answers;
        this.isUnionMembersOnly = isUnionMembersOnly;
        this.completion = completion;
        this.isCompletedByCurrentUser = isCompletedByCurrentUser;
        this.questionnaire = questionnaire;
    }

    public ChoiceQuestion() { }

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

    public boolean isMultipleChoiceEnabled() {
        return isMultipleChoiceEnabled;
    }

    public void setMultipleChoiceEnabled(boolean multipleChoiceEnabled) {
        isMultipleChoiceEnabled = multipleChoiceEnabled;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    /* public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    } */

    public boolean isUnionMembersOnly() {
        return isUnionMembersOnly;
    }

    public void setUnionMembersOnly(boolean unionMembersOnly) {
        isUnionMembersOnly = unionMembersOnly;
    }

    public Integer getCompletion() {
        return completion;
    }

    public void setCompletion(Integer completion) {
        this.completion = completion;
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
