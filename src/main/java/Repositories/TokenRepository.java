package Repositories;

import Models.Token;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TokenRepository extends CrudRepository<Token, Long> {

    List<Token> findAll();
    Token findByUuid(UUID uuid);

    @Modifying
    @Query("DELETE FROM Token t where t.uuid = ?1")
    void deleteByUuid(UUID uuid);

    @Modifying
    @Query("DELETE FROM Token t where t.user.id = ?1 and t.validTo < ?2")
    void deleteByUserId(Long id, LocalDateTime now);
}
