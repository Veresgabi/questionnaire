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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.stream.Collectors;

import static Services.PasswordEncrypter.decrypt;
import static Services.PasswordEncrypter.encrypt;

@Service
public class UserService implements IUserService {

    private final String errorRegistration = "A regisztráció sikertelen a következő hiba miatt: ";
    private final String errorConfirmation = "A regisztráció megerősítése sikertelen a következő hiba miatt: ";
    private final String errorLogin = "A bejelentkezés sikertelen a következő hiba miatt: ";
    private final String errorChangePassword = "A jelszó megváltoztatása sikertelen a következő hiba miatt: ";
    private final String errorFoundUser = "A felhasználó keresése sikertelen a következő hiba miatt: ";
    private final String errorDeleteUser = "A felhasználó törlése sikertelen a következő hiba miatt: ";
    private final String errorSendUnionNumber = "A szakszervezeti regisztrációs szám megadása sikertelen a következő hiba miatt: ";
    private final String errorForgotPassword = "A jelszó küldése sikertelen a következő hiba miatt: ";
    private final String userNameOrPasswordExists = "A megadott felhasználónév vagy jelszó már " +
            "létezik. ";
    private final String emailExists = "A megadott email cím már létezik. ";
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
    private final String invalidUserNameOrEmail = "Az Ön által megadott felhasználó név vagy e-mail cím nem található.";
    private final String userNoHaveEmail = "Az Ön által megadott felhasználó névhez nem tartozik regisztrált e-mail cím.";
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
    @Autowired
    public EmailService emailService;

    private UserResponseDTO apiResponse;

    public UserResponseDTO getApiResponse() {
        return apiResponse;
    }

    @Transactional
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
        try {
            List<User> disabledUsers = userRepository.getDisabledUsers();
            List<User> deletedUsers = disabledUsers
                    .stream()
                    .filter(u -> u.getTokens()
                            .stream()
                            .filter(token -> !LocalDateTime.now().isAfter(token.getValidTo()))
                            .collect(Collectors.toList()).isEmpty())
                    .collect(Collectors.toList());
            userRepository.deleteAll(deletedUsers);
        }
        catch (Exception e) {
            response = new UserResponseDTO();
            if (e.getMessage() != null)
                response.setResponseText(errorRegistration + e.getMessage());
            else response.setResponseText(errorRegistration + e);
            apiResponse = response;

            throw e;
        }
        response = validateNewUser(user);

        if (response.isValidUser() && response.getResponseText().isEmpty()) {

            try {
                User savedUser;

                if (!user.getUnionMembershipNum().isEmpty()) user.setRole(Enums.Role.UNION_MEMBER_USER);
                else user.setRole(Enums.Role.USER);

                user.setPassword(encrypt(user.getPassword()));

                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    Token token = tokenService.createToken(Enums.Role.USER, 1);
                    user.setTokens(Arrays.asList(token));

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                    String tokenExpireTime = token.getValidTo().format(formatter);

                    savedUser = userRepository.save(user);

                    String text = "Kedves " + user.getUserName() + "!" +
                            "\n\nA gszsz.hu kérdőív kitöltő alkalmazásán történt regisztrációjának megerősítése érdekében " +
                            "kérjük, kattintson a következő linkre: " +
                            "https://fierce-meadow-29425.herokuapp.com/user/confirmRegistration?id=" + token.getUuid() + "_" + user.getId() +
                            "\nTájékoztatjuk, hogy a regisztráció megerősítésére a következő időpontig van lehetősége: " +
                            tokenExpireTime + "." +
                            "\n\nÜdvözlettel: Makói Gumiipari Szakszervezet";

                    emailService.sendSimpleMessage(user.getEmail(), "Regisztráció megerősítése", text);
                }
                else {
                    user.setEnabled(true);
                    savedUser = userRepository.save(user);
                }

                if (savedUser != null) {
                    response.setResponseText(success);
                    // response.setTokenUUID(user.getTokens().get(0).getUuid());
                }
            }
            catch (Exception e) {
                response.setSuccessful(false);
                String errorMessage = "";
                if (e.getMessage() != null) errorMessage = e.getMessage();
                else errorMessage = e.toString();

                if (errorMessage.contains("Failed messages") && errorMessage.contains("SendFailedException: Invalid Addresses"))
                    response.setResponseText(errorRegistration + "Az Ön által megadott e-mail cím nem megfelelő.");
                else if (errorMessage.contains("Mail server connection failed") && errorMessage.contains("MailConnectException: Couldn't connect to host"))
                    response.setResponseText(errorRegistration + "A server nem elérhető, kérjük, ellenőrizze internet kapcsolatát.");
                else if (errorMessage.contains("Authentication failed") && errorMessage.contains("AuthenticationFailedException")
                        && errorMessage.contains("Username and Password not accepted"))
                    response.setResponseText(errorRegistration + "Az e-mail server hitelesítése nem sikerült.");
                else response.setResponseText(errorRegistration + errorMessage);

                apiResponse = response;

                throw e;
            }
        }
        return response;
    }

    public String confirmRegistration(String id) {

        String responseText = "";
        String redirectPage = "";
        String redirectText = "";

        String uuidStg = null;
        Long userId = null;
        String[] splitedId = id.split("_");

        try {
            if (splitedId != null && splitedId.length > 0) {
                uuidStg = splitedId[0];

                if (splitedId.length == 2) userId = Long.parseLong(splitedId[1]);
            }

            UUID tokenUUID = UUID.fromString(uuidStg);
            Token token = tokenRepository.findByUuid(tokenUUID);

            if (token != null) {
                User currentUser = token.getUser();
                if (LocalDateTime.now().isBefore(token.getValidTo()) && currentUser != null) {

                    if (!currentUser.isEnabled()) {
                        currentUser.setEnabled(true);
                        userRepository.save(currentUser);
                        responseText = "Profiljának aktiválása sikeresen megtörtént, így már be tud jelentkezni " +
                                "felhasználó nevének és jelszavának megadásával.";
                    }
                    else responseText = "Profiljának aktiválása már sikeresen megtörtént korábban, így már be tud jelentkezni " +
                            "felhasználó nevének és jelszavának megadásával.";

                    redirectText = "másodpercen belül átirányítjuk a bejelentkező felületre.";
                    redirectPage = "http://gszsz.hu/kerdoiv/login.html";
                }
                else if (LocalDateTime.now().isAfter(token.getValidTo()) && currentUser != null) {
                    if (!currentUser.isEnabled()) {
                        responseText = "Profiljának aktiválása sikertelen, mivel az aktiválásra " +
                                "rendelkezésre álló idő lejárt.";
                        redirectText = "másodpercen belül átirányítjuk a regisztrációs felületre.";
                        redirectPage = "http://gszsz.hu/kerdoiv/register.html";
                    }
                    else {
                        responseText = "Profiljának aktiválása már sikeresen megtörtént korábban, " +
                                "így már be tud jelentkezni felhasználó nevének és jelszavának megadásával.";
                        redirectText = "másodpercen belül átirányítjuk a bejelentkező felületre.";
                        redirectPage = "http://gszsz.hu/kerdoiv/login.html";
                    }
                }
                // no need this case below because if the token is exists, the currentUser cannot be deleted
                // else if (currentUser != null)
            }
            else {
                User currentUser = userRepository.findUserById(userId);
                if (currentUser == null || !currentUser.isEnabled()) {
                    responseText = "Profiljának aktiválása nem történt meg, mivel az azonosítás sikertelen volt.";
                    redirectText = "másodpercen belül átirányítjuk a regisztrációs felületre.";
                    redirectPage = "http://gszsz.hu/kerdoiv/register.html";
                }
                else if (currentUser != null && currentUser.isEnabled()) {
                    responseText = "Profiljának aktiválása már sikeresen megtörtént korábban, " +
                            "így már be tud jelentkezni felhasználó nevének és jelszavának megadásával.";
                    redirectText = "másodpercen belül átirányítjuk a bejelentkező felületre.";
                    redirectPage = "http://gszsz.hu/kerdoiv/login.html";
                }
            }
        }
        catch (Exception e) {
            if (e.getMessage() != null) responseText = errorConfirmation + e.getMessage();
            else responseText = errorConfirmation + e;
        }

        return getConfirmRegistrationResponseHtmlString(responseText, redirectText, redirectPage);
    }

    private String getConfirmRegistrationResponseHtmlString(String responseText, String redirectText, String redirectPage) {

        String htmlRedirectNode = "";

        if (redirectText != "") {
            htmlRedirectNode = String.format("    <div style=\"margin-top: 20px;\">\n" +
                    "        <span id=\"counter\"></span> %s\n" +
                    "    </div>", redirectText);
        }

        String mainHTML = String.format("<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "</head>\n" +
                "<body style=\"text-align: center; font-family: sans-serif;\">\n" +
                "    <div style=\"margin-top: 100px;\">\n" +
                "        %s\n" +
                "    </div>\n" +
                "    %s\n" +
                "</body>", responseText, htmlRedirectNode);

        String jsCode = String.format("<script>\n" +
                "    var delayTime = 7;\n" +
                "    var counterElement = document.getElementById(\"counter\");\n" +
                "    counterElement.innerHTML = delayTime;\n" +
                "    \n" +
                "    var countDownDate = new Date().getTime() + (delayTime + 1) * 1000;\n" +
                "\n" +
                "    var x = setInterval(function() {\n" +
                "\n" +
                "        var now = new Date().getTime();\n" +
                "\n" +
                "        var distance = countDownDate - now;\n" +
                "        var seconds = Math.floor((distance %s (1000 * 60)) / 1000);\n" +
                "\n" +
                "        if (seconds > -1) counterElement.innerHTML = seconds;\n" +
                "\n" +
                "        if (distance < 0) {\n" +
                "            clearInterval(x);\n" +
                "            window.location.href = \"%s\";\n" +
                "        }\n" +
                "    }, 1000);\n" +
                "</script>", "%", redirectPage);

        if (!redirectText.isEmpty() && !redirectPage.isEmpty()) {
            return mainHTML + jsCode;
        }
        return mainHTML;
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
            if (foundUser != null && foundUser.isEnabled()
                    && foundUser.getPassword().equals(encrypt(user.getPassword()))) {

                Token token = tokenService.createToken(foundUser.getRole());
                foundUser.tokensAdd(token);
                userRepository.save(foundUser);

                tokenRepository.deleteByUserId(foundUser.getId(), LocalDateTime.now());
                response.setTokenUUID(token.getUuid());
                response.setResponseText(successfulLogin);
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

    public UserResponseDTO forgotPassword(User user) {

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

        String emailPopupText = "";
        try {
            boolean givenUserName = true;
            User foundUser = null;

            if (user.getUserName() != null && !user.getUserName().isEmpty()) {
                foundUser = userRepository.findByUserName(user.getUserName());

                if (foundUser == null) {
                    givenUserName = false;
                    foundUser = userRepository.findByEmail(user.getUserName());
                }
            }
            if (foundUser != null && foundUser.isEnabled()) {

                if (!foundUser.getEmail().isEmpty()) {
                    String text = "";
                    String userNameText = givenUserName ? "" : "\nAz Ön felhasználó neve a következő: " + foundUser.getUserName();
                    text = "Kedves " + foundUser.getUserName() + "!" +
                            "\n\nÖn a gszsz.hu kérdőív kitöltő alkalmazásán regisztrált profilja jelszavának megküldését kérte, " +
                            "melyre tekintettel az alábbiakról tájékoztatjuk:" + userNameText +
                            "\nAz Ön jelszava a következő: " + decrypt(foundUser.getPassword()) +
                            "\n\nÜdvözlettel: Makói Gumiipari Szakszervezet";
                    emailPopupText = givenUserName ? "nyilvántartásunkban szereplő" : "megadott";
                    emailService.sendSimpleMessage(foundUser.getEmail(), "Elfelejtett jelszó", text);

                    String userNamePopupText = givenUserName ? "" : "felhasználó nevét és ";
                    response.setResponseText("Az Ön " + userNamePopupText +  "jelszavát elküldtük a " + emailPopupText + " e-mail címére");
                }
                else response.setResponseText(userNoHaveEmail);
            }
            else response.setResponseText(invalidUserNameOrEmail);
        }
        catch (Exception e) {
            response.setSuccessful(false);
            String errorMessage = "";
            if (e.getMessage() != null) errorMessage = e.getMessage();
            else errorMessage = e.toString();

            if (errorMessage.contains("Failed messages") && errorMessage.contains("SendFailedException: Invalid Addresses"))
                response.setResponseText(errorForgotPassword + "A " + emailPopupText + " e-mail címre nem sikerült elküldeni üzenetünket.");
            else if (errorMessage.contains("Mail server connection failed") && errorMessage.contains("MailConnectException: Couldn't connect to host"))
                response.setResponseText(errorForgotPassword + "A server nem elérhető, kérjük, ellenőrizze internet kapcsolatát.");
            else if (errorMessage.contains("Authentication failed") && errorMessage.contains("AuthenticationFailedException")
                    && errorMessage.contains("Username and Password not accepted"))
                response.setResponseText(errorForgotPassword + "Az e-mail server hitelesítése nem sikerült.");
            else response.setResponseText(errorForgotPassword + errorMessage);
        }
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
            else if (!user.getEmail().isEmpty() && userRepository.findByEmail(user.getEmail()) != null) {
                response.setValidUser(false);
                response.setResponseText(emailExists);
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
