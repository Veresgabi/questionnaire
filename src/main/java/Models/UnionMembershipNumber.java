package Models;

import javax.persistence.*;

@Entity
@Table(name = "UnionMembershipNumbers")
public class UnionMembershipNumber {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Id", nullable = false)
  private Long id;

  @Column(name = "UnionMembershipNumber", nullable = false)
  private String unionMembershipNum;

  @Column(name = "RegistrationNumber")
  private String registrationNumber;

  @Column(name = "IsActive")
  private boolean isActive;

  @Transient
  private boolean needToInactivate = true;

  public UnionMembershipNumber(Long id, String unionMembershipNum, String registrationNumber, boolean isActive,
                               boolean needToInactivate) {
    this.id = id;
    this.unionMembershipNum = unionMembershipNum;
    this.registrationNumber = registrationNumber;
    this.isActive = isActive;
    this.needToInactivate = needToInactivate;
  }

  public UnionMembershipNumber() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUnionMembershipNum() {
    return unionMembershipNum;
  }

  public void setUnionMembershipNum(String unionMembershipNum) {
    this.unionMembershipNum = unionMembershipNum;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public void setRegistrationNumber(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public boolean isNeedToInactivate() {
    return needToInactivate;
  }

  public void setNeedToInactivate(boolean needToInactivate) {
    this.needToInactivate = needToInactivate;
  }
}
