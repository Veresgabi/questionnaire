package Services;

import DTOs.AbstractDTO;
import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.*;
import Repositories.*;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;

import static Services.PasswordEncrypter.encrypt;

@Service
public class UserService implements IUserService {

    private final String errorRegistration = "A regisztráció sikertelen a következő hiba miatt: ";
    private final String errorLogin = "A bejelentkezés sikertelen a következő hiba miatt: ";
    private final String errorChangePassword = "A jelszó megváltoztatása sikertelen a következő hiba miatt: ";
    private final String errorFoundUser = "A felhasználó keresése sikertelen a következő hiba miatt: ";
    private final String errorDeleteUser = "A felhasználó törlése sikertelen a következő hiba miatt: ";
    private final String errorSendUnionNumber = "A szakszervezeti regisztrációs szám megadása sikertelen a következő hiba miatt: ";
    private final String userNameOrPasswordExists = "A megadott felhasználónév vagy jelszó már " +
            "létezik. ";
    private final String invalidUserNameOrPasswordLogin = "Helytelen felhasználónév vagy jelszó!";
    private final String invalidPassword = "Az Ön által megadott jelenlegi jelszó nem megfelelő.";
    private final String invalidNewPassword = "Az Ön által megadott új jelszó nem megfelelő.";
    private final String sameNewPassword = "Az Ön által megadott új jelszó megegyezik a jelenlegi jelszavával.";
    private final String usedRegistrationNum = "Az Ön által megadott munkavállalói törzsszámmal már " +
        "létezik regisztrált felhasználó.";
    private final String invalidRegistrationNum = "Az Ön által megadott munkavállalói törzsszám nem" +
        " szerepel a nyilvántartásunkban.";
    private final String usedUnionMemberNum = "A megadott szakszervezeti tagsági számmal már " +
            "létezik regisztrált felhasználó.";
    private final String invalidUnionMemberNum = "A megadott szakszervezeti tagsági szám adatbázisunkban nem" +
        " fellelhető.";
    private final String invalidName = "A megadott munkavállalói törzsszámhoz tartozó, " +
        "nyilvántartásunkban szereplő név nem egyezik az Ön által megadott névvel.";

    private final String success = "Sikeres regisztráció!";
    private final String successfulLogin = "Sikeres bejelentkezés!";
    private final String passwordChanged = "A jelszó megváltozott!";

    @Autowired
    public UserRepository userRepository;
    @Autowired
    public TokenRepository tokenRepository;
    @Autowired
    public RegNumberRepository regNumberRepository;
    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepo;
    @Autowired
    public ITokenService tokenService;
    @Autowired
    public RegistrationNumberQuestionnaireRepository regNumQuestRepository;

    public UserResponseDTO saveUser(User user) throws Exception {

        UserResponseDTO response;

        if (user.getTokenUUID() != null) {
            response = tokenService.checkToken(user.getTokenUUID());

            if (response.isSuccessful() && response.isAuthSuccess()) {
                response.setExpiredPage(true);
                return response;
            }
            else if (!response.isSuccessful()) return response;
        }

        response = validateNewUser(user);

        if (response.isValidUser() && response.getResponseText().isEmpty()) {
            Token token = tokenService.createToken(Enums.Role.USER);
            user.setTokens(Arrays.asList(token));
            if (!user.getUnionMembershipNum().isEmpty()) user.setRole(Enums.Role.UNION_MEMBER_USER);
            else user.setRole(Enums.Role.USER);

            try {
                user.setPassword(encrypt(user.getPassword()));
                User savedUser = userRepository.save(user);
                if (savedUser != null) {
                    response.setResponseText(success);
                    response.setTokenUUID(user.getTokens().get(0).getUuid());
                }
            }
            catch (Exception e) {
                response.setSuccessful(false);
                if (e.getMessage() != null)
                    response.setResponseText(errorRegistration + e.getMessage());
                else response.setResponseText(errorRegistration + e);
            }
        }
        return response;
    }

    @Transactional
    public UserResponseDTO login(User user) {

        UserResponseDTO response = new UserResponseDTO();

        if (user.getTokenUUID() != null) {
            response = tokenService.checkToken(user.getTokenUUID());

            if (response.isSuccessful() && response.isAuthSuccess()) {
                response.setExpiredPage(true);
                return response;
            }
            else if (!response.isSuccessful()) return response;
        }
        else response.setSuccessful(true);

        response.setResponseText(invalidUserNameOrPasswordLogin);
        response.setValidUser(true);

        try {
            User foundUser = userRepository.findByUserName(user.getUserName());
            if (foundUser != null) {
                if (foundUser.getPassword().equals(encrypt(user.getPassword()))) {

                    Token token = tokenService.createToken(foundUser.getRole());
                    foundUser.tokensAdd(token);
                    userRepository.save(foundUser);

                    tokenRepository.deleteByUserId(foundUser.getId(), LocalDateTime.now());
                    response.setTokenUUID(token.getUuid());
                    response.setResponseText(successfulLogin);
                }
                else response.setValidUser(false);
            }
            else response.setValidUser(false);
        }
        catch (Exception e) {
            response.setTokenUUID(null);
            response.setSuccessful(false);
            if (e.getMessage() != null)
                response.setResponseText(errorLogin + e.getMessage());
            else response.setResponseText(errorLogin + e);
        }
        return response;
    }

    public UserResponseDTO logout(User user) {
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());

        if (!response.isSuccessful() || !response.isAuthSuccess()) return response;

        if (user.getId().equals(response.getUser().getId())) {
            response = tokenService.deleteToken(user.getTokenUUID());
        }
        else response.setExpiredPage(true);

        return response;
    }

    public UserResponseDTO changePassword(User user) {

        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(user.getId())) {
            response.setExpiredPage(true);
            return response;
        }

        try {
            // A régi password-öt a userName tartalmazza
            if (currentUser.getPassword().equals(encrypt(user.getUserName()))) {

                if (currentUser.getPassword().equals(encrypt(user.getPassword()))) {
                    response.setResponseText(sameNewPassword);
                    response.setValidUser(false);
                }
                else {
                    User foundUser = userRepository.findByPassword(encrypt(user.getPassword()));
                    if (foundUser != null) {
                        response.setResponseText(invalidNewPassword);
                        response.setValidUser(false);
                    }
                    else {
                        response = tokenService.refreshToken(response.getTokenUUID(), currentUser);
                        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

                        response.setValidUser(true);
                        currentUser.setPassword(encrypt(user.getPassword()));
                        userRepository.save(currentUser);

                        response.setResponseText(passwordChanged);
                    }
                }
            }
            else {
                response.setResponseText(invalidPassword);
                response.setValidUser(false);
            }

        }
        catch (Exception e) {
            response.setTokenUUID(null);
            response.setSuccessful(false);

            if (e.getMessage() != null)
                response.setResponseText(errorChangePassword + e.getMessage());
            else response.setResponseText(errorChangePassword + e);

        }
        return response;
    }

    private UserResponseDTO validateNewUser(User user) throws Exception {

        UserResponseDTO response = new UserResponseDTO();
        response.setValidUser(true);
        response.setSuccessful(true);
        response.setResponseText("");

        try {
            if (userRepository.findByPassword(encrypt(user.getPassword())) != null) {
                response.setValidUser(false);
                response.setResponseText(userNameOrPasswordExists);
                return response;
            }
            else if (userRepository.findByUserName(user.getUserName()) != null) {
                response.setValidUser(false);
                response.setResponseText(userNameOrPasswordExists);
                return response;
            }

            RegistrationNumber regNum = regNumberRepository.findByRegistrationNum(user.getRegistrationNum());
            if (regNum == null) {
                response.setValidUser(false);
                response.setResponseText(invalidRegistrationNum);
            }
            else if (!regNum.isActive()) {
                response.setValidUser(false);
                response.setResponseText(invalidRegistrationNum);
            }
            else if (userRepository.findByRegistrationNum(user.getRegistrationNum()) != null) {
                response.setValidUser(false);
                response.setResponseText(usedRegistrationNum);
            }

            if (!user.getUnionMembershipNum().isEmpty()) {
                response = validateRegistrationNumber(user.getUnionMembershipNum(), response);
            }

            // If we need to store firstName and lastName
            /* else if (!regNum.getFirstName().equals(user.getFirstName()) || !regNum.getLastName().equals(user.getLastName())) {
                    response.setValid(false);
                    response.setMessage(response.getMessage() + invalidName);
                }
            else if (!regNum.isActive()) {
                    response.setValid(false);
                    response.setMessage(response.getMessage() + inactiveRegistrationNum);
            } */
        }
        catch (Exception e) {
            response.setSuccessful(false);

            if (e.getMessage() != null)
                response.setResponseText(errorRegistration + e.getMessage());
            else response.setResponseText(errorRegistration + e);
        }

        return response;
    }

    private UserResponseDTO validateRegistrationNumber(String unionMembershipNum, UserResponseDTO response) {
        if (response.getResponseText() == null) response.setResponseText("");
        String whiteSpace = response.getResponseText().isEmpty() ? "" : " ";

        UnionMembershipNumber unionNum = unionMembershipNumRepo.findByUnionMembershipNum(unionMembershipNum);

        if (unionNum == null) {
            response.setValidUser(false);
            response.setResponseText(response.getResponseText() + whiteSpace + invalidUnionMemberNum);
            return response;
        }
        else if (!unionNum.isActive()) {
            response.setValidUser(false);
            response.setResponseText(response.getResponseText() + whiteSpace + invalidUnionMemberNum);
            return response;
        }
        else if (userRepository.findByUnionMembershipNum(unionMembershipNum) != null) {
            response.setValidUser(false);
            response.setResponseText(response.getResponseText() + whiteSpace + usedUnionMemberNum);
            return response;
        }

        return response;
    }

    public UserResponseDTO findUserByRegistrationNumber(UserRequestDTO userRequestDTO) {
        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(userRequestDTO.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(userRequestDTO.getCurrentUserId())) {
            response.setExpiredPage(true);
            return response;
        }
        // 3. Check the role is compatible for the requested action
        if (!currentUser.getRole().equals(Enums.Role.ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }

        try {
            User foundUser = userRepository.findByRegistrationNum(userRequestDTO.getRegistrationNum());
            response.setSuccessful(true);

            if (foundUser != null && !foundUser.getRole().equals(Enums.Role.ADMIN) && !foundUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                response.setUser(foundUser);
            }
            else response.setResponseText("A megadott dolgozói törzsszámmal számmal nem található regisztrált felhasználó.");
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText(errorFoundUser + e.getMessage());
            else response.setResponseText(errorFoundUser + e);
            response.setSuccessful(false);
        }
        return response;
    }

    public UserResponseDTO deleteUserById(UserRequestDTO userRequestDTO) {
        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(userRequestDTO.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(userRequestDTO.getCurrentUserId())) {
            response.setExpiredPage(true);
            return response;
        }

        Long deletedUserId = 0L;

        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.ADMIN) && !userRequestDTO.getDeletedUserId().equals(userRequestDTO.getCurrentUserId())) {
            deletedUserId = userRequestDTO.getDeletedUserId();
            response = tokenService.refreshToken(response.getTokenUUID(), currentUser);
            if (!response.isAuthSuccess() || !response.isSuccessful()) return response;
        }
        else if ((currentUser.getRole().equals(Enums.Role.USER) || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER))
                && userRequestDTO.getDeletedUserId().equals(userRequestDTO.getCurrentUserId()))
            deletedUserId = userRequestDTO.getCurrentUserId();

        if (deletedUserId.equals(0L)) {
            response.setExpiredPage(true);
            return response;
        }

        try {
            User foundUser = userRepository.findUserById(deletedUserId);

            if (foundUser != null && !foundUser.getRole().equals(Enums.Role.ADMIN) && !foundUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN))
                userRepository.deleteById(deletedUserId);
            else
                response.setResponseText("Az adatbázisban már nem található a törölni kívánt felhasználó.");

            // The statement below is not important, because checkToken() method already set it to true
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if ((e.getClass().equals(EmptyResultDataAccessException.class) ||
                    e.getClass().equals(MissingResourceException.class)) &&
                    e.getMessage().contains(deletedUserId.toString())) {

                response.setResponseText("Az adatbázisban már nem található a törölni kívánt felhasználó.");
                response.setSuccessful(true);
            }
            else {
                if (e.getMessage() != null)
                    response.setResponseText(errorDeleteUser + e.getMessage());
                else response.setResponseText(errorDeleteUser + e);
                response.setSuccessful(false);
            }
        }
        return response;
    }

    public UserResponseDTO sendUnionNumber(User user) {

        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(user.getId())) {
            response.setExpiredPage(true);
            return response;
        }
        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.USER)) {
            response = tokenService.refreshToken(response.getTokenUUID(), currentUser);
            if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

            try {
                // Ez a setValidUser mire van? Front end-en be van if-elve, de felesleges, elég lenne akkor is a
                // currentUser = response.user (frontend-en), ha az authSuccess true!
                response.setValidUser(true);
                response = validateRegistrationNumber(user.getUnionMembershipNum(), response);

                if (response.isValidUser() && response.getResponseText().isEmpty()) {
                    currentUser.setUnionMembershipNum(user.getUnionMembershipNum());
                    currentUser.setRole(Enums.Role.UNION_MEMBER_USER);
                    userRepository.save(currentUser);
                    response.setUser(currentUser);
                }
            }
            catch (Exception e) {
                if (e.getMessage() != null)
                    response.setResponseText(errorSendUnionNumber + e.getMessage());
                else response.setResponseText(errorSendUnionNumber + e);
                response.setSuccessful(false);
            }
        }
        else {
            response.setExpiredPage(true);
            return response;
        }
        return response;
    }

    public UserResponseDTO isExpiredPage(User user) {

        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(user.getId())) {
            response.setExpiredPage(true);
            return response;
        }
        return response;
    }

    public UserResponseDTO checkAndRefreshToken(User user) {
        // 1. Check token is valid
        UserResponseDTO response = tokenService.checkToken(user.getTokenUUID());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = response.getUser();

        // 2. Check the current user of the page is equals to user of token
        // 3. Check the role is compatible for the requested action
        if (!currentUser.getId().equals(user.getId()) || !currentUser.getRole().equals(user.getRole())) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        response = tokenService.refreshToken(response.getTokenUUID(), currentUser);

        // if (!response.isAuthSuccess() || !response.isSuccessful()) return response;
        return response;
    }

    public <T extends AbstractDTO> T removeUserPassword(T response) {
        if (response.getUser() != null) response.getUser().setPassword(null);
        return response;
    }

    public UserResponseDTO testTransactionOperation(User user, User userWithoutRegNum, List<RegistrationNumberQuestionnaire> rnqList) {
        UserResponseDTO response = new UserResponseDTO();
        try {
            regNumQuestRepository.saveAll(rnqList);
            regNumQuestRepository.deleteById(88L);
            regNumQuestRepository.deleteByQuestionnaireId(101010L);
            userRepository.save(user);
            userRepository.save(userWithoutRegNum);
        }
        catch (Exception e) {
            response.setResponseText(e.getMessage());
        }

        return response;
    }
}
