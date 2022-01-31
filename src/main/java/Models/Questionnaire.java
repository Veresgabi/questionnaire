package Models;

import Utils.Enums;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Questionnaires")
public class Questionnaire implements Comparable<Questionnaire> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "isUnionMembersOnly")
    private boolean isUnionMembersOnly;

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    private List<ChoiceQuestion> choiceQuestions = new ArrayList<>();

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    private List<ScaleQuestion> scaleQuestions = new ArrayList<>();

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="questionnaire_Id")
    private List<TextualQuestion> textualQuestions = new ArrayList<>();

    @Column(name = "isPublished")
    private boolean isPublished;

    @Column(name = "state")
    private Enums.State state;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Transient
    private String formattedCreatedAt;

    @Column(name = "lastModified")
    private LocalDateTime lastModified;

    @Transient
    private String formattedLastModified;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Transient
    private String formattedDeadline;

    @Transient
    private boolean isStateChange;

    @Transient
    private Integer completion;

    @Transient
    private String completionRate;

    @Transient
    private Integer relatedUsers;

    @Transient
    private Integer completionMako;

    @Transient
    private Integer completionMakoBorrowed;

    @Transient
    private Integer completionVac;

    @Transient
    private Integer completionVacBorrowed;

    public Questionnaire(Long id, String title, boolean isUnionMembersOnly,
                         List<ChoiceQuestion> choiceQuestions, List<ScaleQuestion> scaleQuestions,
                         List<TextualQuestion> textualQuestions, boolean isPublished, Enums.State state,
                         LocalDateTime createdAt, String formattedCreatedAt, LocalDateTime lastModified,
                         String formattedLastModified, LocalDateTime deadline, String formattedDeadline,
                         boolean isStateChange, Integer completion, String completionRate, Integer completionMako,
                         Integer relatedUsers, Integer completionMakoBorrowed, Integer completionVac, Integer completionVacBorrowed) {
        this.id = id;
        this.title = title;
        this.isUnionMembersOnly = isUnionMembersOnly;
        this.choiceQuestions = choiceQuestions;
        this.scaleQuestions = scaleQuestions;
        this.textualQuestions = textualQuestions;
        this.isPublished = isPublished;
        this.state = state;
        this.createdAt = createdAt;
        this.formattedCreatedAt = formattedCreatedAt;
        this.lastModified = lastModified;
        this.formattedLastModified = formattedLastModified;
        this.deadline = deadline;
        this.formattedDeadline = formattedDeadline;
        this.isStateChange = isStateChange;
        this.completion = completion;
        this.relatedUsers = relatedUsers;
        this.completionRate = completionRate;
        this.completionMako = completionMako;
        this.completionMakoBorrowed = completionMakoBorrowed;
        this.completionVac = completionVac;
        this.completionVacBorrowed = completionVacBorrowed;
    }

    public Questionnaire() { }

    public int compareTo(Questionnaire e) {
        return this.getLastModified().compareTo(e.getLastModified());
    }

    @Transient
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isUnionMembersOnly() {
        return isUnionMembersOnly;
    }

    public void setUnionMembersOnly(boolean unionMembersOnly) {
        isUnionMembersOnly = unionMembersOnly;
    }

    public List<ChoiceQuestion> getChoiceQuestions() {
        return choiceQuestions;
    }

    public void setChoiceQuestions(List<ChoiceQuestion> choiceQuestions) {
        this.choiceQuestions = choiceQuestions;
    }

    public List<ScaleQuestion> getScaleQuestions() {
        return scaleQuestions;
    }

    public void setScaleQuestions(List<ScaleQuestion> scaleQuestions) {
        this.scaleQuestions = scaleQuestions;
    }

    public List<TextualQuestion> getTextualQuestions() {
        return textualQuestions;
    }

    public void setTextualQuestions(List<TextualQuestion> textualQuestions) {
        this.textualQuestions = textualQuestions;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public Enums.State getState() {
        return state;
    }

    public void setState(Enums.State state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        if (createdAt != null) this.formattedCreatedAt = createdAt.format(formatter);
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        if (lastModified != null) this.formattedLastModified = lastModified.format(formatter);
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getFormattedDeadline() {
        if (deadline != null) this.formattedDeadline = deadline.format(formatter).split(" ")[0];
        return formattedDeadline;
    }

    public void setFormattedDeadline(String formattedDeadline) {
        this.formattedDeadline = formattedDeadline;
    }

    public boolean isStateChange() {
        return isStateChange;
    }

    public void setStateChange(boolean stateChange) {
        isStateChange = stateChange;
    }

    public Integer getCompletion() {
        return completion;
    }

    public void setCompletion(Integer completion) {
        this.completion = completion;
    }

    public String getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(String completionRate) {
        this.completionRate = completionRate;
    }

    public Integer getRelatedUsers() {
        return relatedUsers;
    }

    public void setRelatedUsers(Integer relatedUsers) {
        this.relatedUsers = relatedUsers;
    }

    public Integer getCompletionMako() {
        return completionMako;
    }

    public void setCompletionMako(Integer completionMako) {
        this.completionMako = completionMako;
    }

    public Integer getCompletionMakoBorrowed() {
        return completionMakoBorrowed;
    }

    public void setCompletionMakoBorrowed(Integer completionMakoBorrowed) {
        this.completionMakoBorrowed = completionMakoBorrowed;
    }

    public Integer getCompletionVac() {
        return completionVac;
    }

    public void setCompletionVac(Integer completionVac) {
        this.completionVac = completionVac;
    }

    public Integer getCompletionVacBorrowed() {
        return completionVacBorrowed;
    }

    public void setCompletionVacBorrowed(Integer completionVacBorrowed) {
        this.completionVacBorrowed = completionVacBorrowed;
    }

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public String getFormattedLastModified() {
        return formattedLastModified;
    }
}
