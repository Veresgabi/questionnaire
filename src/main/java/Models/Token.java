package Models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "UUID", nullable = false)
    private UUID uuid;

    @Column(name = "GeneratedAt")
    private LocalDateTime generatedAt;

    @Column(name = "ValidTo")
    private LocalDateTime validTo;

    @Transient
    private boolean isValid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="User_Id")
    private User user;

    public Token(Long id, UUID uuid, LocalDateTime generatedAt, LocalDateTime validTo, boolean isValid, User user) {
        this.id = id;
        this.uuid = uuid;
        this.generatedAt = generatedAt;
        this.validTo = validTo;
        this.isValid = isValid;
        this.user = user;
    }

    public Token(UUID uuid, LocalDateTime validTo) {
        this.uuid = uuid;
        this.validTo = validTo;
    }

    public Token() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
