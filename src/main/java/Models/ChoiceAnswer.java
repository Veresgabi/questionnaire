package Models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "ChoiceAnswers")
public class ChoiceAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Id", nullable = false)
  private Long id;

  @Column(name = "Content", nullable = false)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="answer_Id")
  @JsonIgnore
  private Answer answer;

  public ChoiceAnswer(Long id, String content, Answer answer) {
    this.id = id;
    this.content = content;
    this.answer = answer;
  }

  public ChoiceAnswer() { }

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

  public Answer getAnswer() {
    return answer;
  }

  public void setAnswer(Answer answer) {
    this.answer = answer;
  }
}
