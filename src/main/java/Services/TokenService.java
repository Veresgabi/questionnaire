package Services;

import DTOs.UserResponseDTO;
import Models.Token;
import Models.User;
import Repositories.TokenRepository;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.MissingResourceException;
import java.util.UUID;

@Service
public class TokenService implements ITokenService {

    private final String validateTokenError = "A token validációja sikertelen a következő hiba miatt: ";
    private final String refreshTokenError = "A token frissítése sikertelen a következő hiba miatt: ";
    private final String deleteTokenError = "A token törlése sikertelen a következő hiba miatt: ";

    @Autowired
    public TokenRepository tokenRepository;



    public UserResponseDTO checkToken(UUID tokenUUID) {
        UserResponseDTO response = new UserResponseDTO();

        if (tokenUUID == null) {
            response.setSuccessful(true);
            return response;
        }

        Token token = null;

        try {
            token = tokenRepository.findByUuid(tokenUUID);

            if (token != null && LocalDateTime.now().isBefore(token.getValidTo())) {

                response.setUser(token.getUser());
                response.setAuthSuccess(true);
                response.setTokenUUID(token.getUuid());
            }
            else if (token != null && LocalDateTime.now().isAfter(token.getValidTo())) {
                tokenRepository.deleteById(token.getId());
            }
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if ((e.getClass().equals(EmptyResultDataAccessException.class) ||
                    e.getClass().equals(MissingResourceException.class)) &&
                    e.getMessage().contains(token.getId().toString())) {
                response.setSuccessful(true);
            }
            else {
                if (e.getMessage() != null)
                    response.setResponseText(validateTokenError + e.getMessage());
                else response.setResponseText(validateTokenError + e);
            }
        }
        return response;
    }

    @Transactional
    public UserResponseDTO refreshToken(UUID tokenUUID, User user) {
        UserResponseDTO response = new UserResponseDTO();
        // response.setAuthSuccess(true);
        try {
            tokenRepository.deleteByUuid(tokenUUID);

            Token freshToken = createToken(user);
            tokenRepository.save(freshToken);
            response.setTokenUUID(freshToken.getUuid());

            response.setUser(user);
            response.setAuthSuccess(true);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText(refreshTokenError + e.getMessage());
            else response.setResponseText(refreshTokenError + e);
        }
        return response;
    }

    @Transactional
    public UserResponseDTO deleteToken(UUID tokenUUID) {
        UserResponseDTO response = new UserResponseDTO();
        try {
            tokenRepository.deleteByUuid(tokenUUID);
            response.setAuthSuccess(true);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if ((e.getClass().equals(EmptyResultDataAccessException.class) ||
                    e.getClass().equals(MissingResourceException.class)) &&
                    e.getMessage().contains(tokenUUID.toString())) {
                response.setSuccessful(true);
                response.setAuthSuccess(true);
            }
            if (e.getMessage() != null)
                response.setResponseText(deleteTokenError + e.getMessage());
            else response.setResponseText(deleteTokenError + e);
        }
        return response;
    }

    public Token createToken(Enums.Role role) {

        Token token = new Token();
        token.setUuid(UUID.randomUUID());
        token.setGeneratedAt(LocalDateTime.now());
        token.setValidTo(LocalDateTime.now().plusHours(2));

        return token;
    }

    public Token createToken(User user) {

        Token token = new Token();
        token.setUuid(UUID.randomUUID());
        token.setGeneratedAt(LocalDateTime.now());
        token.setValidTo(LocalDateTime.now().plusHours(2));
        token.setUser(user);

        return token;
    }
}
