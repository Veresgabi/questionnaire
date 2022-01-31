package Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name = "Choices")
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Mark", nullable = false, length = 100)
    private String mark;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Transient
    private Integer numberOfSelection;

    @Transient
    private String percentOfSelection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="choice_quest_Id")
    @JsonIgnore
    private ChoiceQuestion choiceQuestion;

    public Choice(Long id, String mark, String text, ChoiceQuestion choiceQuestion,
                  Integer numberOfSelection, String percentOfSelection) {
        this.id = id;
        this.mark = mark;
        this.text = text;
        this.choiceQuestion = choiceQuestion;
        this.numberOfSelection = numberOfSelection;
        this.percentOfSelection = percentOfSelection;
    }

    public Choice() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ChoiceQuestion getChoiceQuestion() {
        return choiceQuestion;
    }

    public void setChoiceQuestion(ChoiceQuestion choiceQuestion) {
        this.choiceQuestion = choiceQuestion;
    }

    public Integer getNumberOfSelection() {
        return numberOfSelection;
    }

    public void setNumberOfSelection(Integer numberOfSelection) {
        this.numberOfSelection = numberOfSelection;
    }

    public String getPercentOfSelection() {
        return percentOfSelection;
    }

    public void setPercentOfSelection(String percentOfSelection) {
        this.percentOfSelection = percentOfSelection;
    }
}
