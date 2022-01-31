package Repositories;

import Models.RegistrationNumber;
import Models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegNumberRepository extends CrudRepository<RegistrationNumber, Long> {

    RegistrationNumber findByRegistrationNum(String registrationNum);

    List<RegistrationNumber> findAll();

    @Query("SELECT COUNT(id) FROM RegistrationNumber r where r.isActive = TRUE")
    Integer getNumberOfUsers();
}
