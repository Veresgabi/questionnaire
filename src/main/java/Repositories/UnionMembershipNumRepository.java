package Repositories;

import Models.RegistrationNumber;
import Models.UnionMembershipNumber;
import Models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UnionMembershipNumRepository extends CrudRepository<UnionMembershipNumber, Long> {

  List<UnionMembershipNumber> deleteByUnionMembershipNum(String unionMembershipNum);

  UnionMembershipNumber findByUnionMembershipNum(String unionMembershipNum);

  List<UnionMembershipNumber> findAll();

  @Query("SELECT COUNT(id) FROM UnionMembershipNumber u where u.isActive = TRUE")
  Integer getNumberOfUnionMemberUsers();

  @Query("SELECT COUNT(id) FROM UnionMembershipNumber")
  Integer getNumberOfRecords();
}
