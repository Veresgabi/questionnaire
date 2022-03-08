package Repositories;

import Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    User findUserById(Long id);
    User findByFirstName(String firstName);
    User findByLastName(String lastName);
    User findByRegistrationNum(String registrationNum);
    User findByUnionMembershipNum(String unMembNum);
    User findByUserName(String userName);
    User findByPassword(String password);
    List<User> findAll();

    @Modifying
    @Query("delete FROM User u where u.role = 0 or role = 1")
    void testDeleteAllUsers();

    @Query("SELECT password FROM User")
    List<String> findPasswords();

    User save(User user);
}
