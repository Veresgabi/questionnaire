package Models;

import Utils.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "FirstName", length = 100)
    private String firstName;

    @Column(name = "LastName", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "UserName", nullable = false, length = 100)
    private String userName;

    @Column(name = "Password", nullable = false, length = 100)
    private String password;

    @Column(name = "Role", nullable = false)
    private Enums.Role role;

    @Column(name = "RegistrationNumber", nullable = false, length = 100)
    private String registrationNum;

    @Column(name = "unionMembershipNum", length = 100)
    private String unionMembershipNum;

    @Column(name = "privacyStatement")
    private boolean privacyStatement;

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name="User_Id")
    @JsonIgnore
    private List<Token> tokens = new ArrayList<>();

    @Transient
    private UUID tokenUUID;

    public User() {  }

    public User(Long id, String firstName, String lastName, String userName, String password, String email,
                boolean enabled, Enums.Role role, String registrationNum, String unionMembershipNum,
                boolean privacyStatement, List<Token> tokens, UUID tokenUUID) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
        this.role = role;
        this.registrationNum = registrationNum;
        this.unionMembershipNum = unionMembershipNum;
        this.privacyStatement = privacyStatement;
        this.tokens = tokens;
        this.tokenUUID = tokenUUID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Enums.Role getRole() {
        return role;
    }

    public void setRole(Enums.Role role) {
        this.role = role;
    }

    public String getRegistrationNum() {
        return registrationNum;
    }

    public void setRegistrationNum(String registrationNum) {
        this.registrationNum = registrationNum;
    }

    public String getUnionMembershipNum() {
        return unionMembershipNum;
    }

    public void setUnionMembershipNum(String unionMembershipNum) {
        this.unionMembershipNum = unionMembershipNum;
    }

    public boolean isPrivacyStatement() {
        return privacyStatement;
    }

    public void setPrivacyStatement(boolean privacyStatement) {
        this.privacyStatement = privacyStatement;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void tokensAdd(Token token) { this.tokens.add(token); }

    public UUID getTokenUUID() {
        return tokenUUID;
    }

    public void setTokenUUID(UUID token) {
        this.tokenUUID = token;
    }
}
