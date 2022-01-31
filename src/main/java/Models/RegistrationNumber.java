package Models;

import Utils.Enums;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;

@Entity
@Table(name = "RegistrationNumbers")
public class RegistrationNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "RegistrationNumber", nullable = false)
    private String registrationNum;

    @Column(name = "FirstName", length = 100)
    private String firstName;

    @Column(name = "LastName", length = 100)
    private String lastName;

    @Column(name = "IsActive")
    private boolean isActive;

    @Column(name = "Location")
    private Enums.Location location;

    @Transient
    private boolean needToInactivate = true;

    public RegistrationNumber(Long id, String registrationNum, String firstName, String lastName,
                              boolean isActive, Enums.Location location, boolean needToInactivate) {
        this.id = id;
        this.registrationNum = registrationNum;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isActive = isActive;
        this.location = location;
        this.needToInactivate = needToInactivate;
    }

    public RegistrationNumber() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNum() {
        return registrationNum;
    }

    public void setRegistrationNum(String registrationNum) {
        this.registrationNum = registrationNum;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Enums.Location getLocation() {
        return location;
    }

    public void setLocation(Enums.Location location) {
        this.location = location;
    }

    public boolean isNeedToInactivate() {
        return needToInactivate;
    }

    public void setNeedToInactivate(boolean needToInactivate) {
        this.needToInactivate = needToInactivate;
    }
}
