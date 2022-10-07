package Controllers;

import DTOs.QuestionnaireDTO;
import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.*;
import Repositories.*;
import Services.IEmailService;
import Services.IQuestionnaireService;
import Services.IUserService;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static Services.PasswordEncrypter.encrypt;

@Controller
@CrossOrigin
@RequestMapping("/user")
public class UserController {

    @Autowired
    public IUserService userService;

    @Autowired
    public IEmailService emailService;

    @Autowired
    public AnswerRepository answerRepository;

    @Autowired
    public ExcelUploadStaticsRepository excelUploadStaticsRepository;

    @Autowired
    public RegistrationNumberQuestionnaireRepository RegNumberQuestionnaireRepository;

    @Autowired
    RegNumberRepository regNumberRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    UnionMembershipNumRepository unionMembershipNumRepository;

    @Autowired
    public UserRepository userRepository;

    @PostMapping("/saveUser")
    @ResponseBody
    public UserResponseDTO saveUser(@RequestBody User user) {
        UserResponseDTO response = new UserResponseDTO();

        try {
            response = userService.saveUser(user);
        }
        catch (Exception e) {
            if (userService.getApiResponse() != null) {
                response = userService.getApiResponse();
            }
            else {
                if (e.getMessage() != null)
                    response.setResponseText("A regisztráció sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A regisztráció sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false);
            }
        }
        return response;
    }

    @PostMapping("/login")
    @ResponseBody
    public UserResponseDTO login(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.login(user);
        return response;
    }

    @PostMapping("/logout")
    @ResponseBody
    public UserResponseDTO logout(@RequestBody User user) {
        UserResponseDTO response = userService.logout(user);
        return response;
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public UserResponseDTO changePassword(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.changePassword(user);
        return response;
    }

    @PostMapping("/forgotPassword")
    @ResponseBody
    public UserResponseDTO forgotPassword(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.forgotPassword(user);
        return response;
    }

    @PostMapping("/findUserByRegNumber")
    @ResponseBody
    public UserResponseDTO findUserByRegistrationNumber(@RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO response = userService.findUserByRegistrationNumber(userRequestDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/deleteUserById")
    @ResponseBody
    public UserResponseDTO deleteUserById(@RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO response = userService.deleteUserById(userRequestDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/sendUnionNumber")
    @ResponseBody
    public UserResponseDTO sendUnionNumber(@RequestBody User user) {
        UserResponseDTO response = userService.sendUnionNumber(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/isExpiredPage")
    @ResponseBody
    public UserResponseDTO isExpiredPage(@RequestBody User user) {
        UserResponseDTO response = userService.isExpiredPage(user);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/refreshToken")
    @ResponseBody
    public UserResponseDTO checkAndRefreshToken(@RequestBody User user) {
        UserResponseDTO response = userService.checkAndRefreshToken(user);
        return userService.removeUserPassword(response);
    }

    @GetMapping("/confirmRegistration")
    @ResponseBody
    public String confirmRegistration(@RequestParam String id) {
        return userService.confirmRegistration(id);
    }

    @PostMapping("/sendNewQuestionnaireEmail")
    @ResponseBody
    public QuestionnaireDTO sendNewQuestionnaireEmail(@RequestBody QuestionnaireDTO questionnaireDTO) {
        return emailService.sendNewQuestionnaireEmail(questionnaireDTO);
    }

    /* @Transactional
    @GetMapping("/testDeleteAllTable")
    @ResponseBody
    public UserResponseDTO testDeleteAllTable() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            answerRepository.deleteAll();
            excelUploadStaticsRepository.deleteAll();
            questionnaireRepository.deleteAll();
            RegNumberQuestionnaireRepository.deleteAll();
            regNumberRepository.deleteAll();
            tokenRepository.deleteAll();
            unionMembershipNumRepository.deleteAll();
            userRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteAnswers")
    @ResponseBody
    public UserResponseDTO testDeleteAnswers() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            answerRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteUploadStatics")
    @ResponseBody
    public UserResponseDTO testDeleteUploadStatics() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            excelUploadStaticsRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteQuestionnaires")
    @ResponseBody
    public UserResponseDTO testDeleteQuestionnaire() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            questionnaireRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteRegNumberQuestionnaires")
    @ResponseBody
    public UserResponseDTO testDeleteRegNumberQuestionnaires() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            RegNumberQuestionnaireRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteRegNumber")
    @ResponseBody
    public UserResponseDTO testDeleteRegNumber() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            regNumberRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteTokens")
    @ResponseBody
    public UserResponseDTO testDeleteTokens() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            tokenRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testExpireTokens")
    @ResponseBody
    public UserResponseDTO testExpireTokens() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            tokenRepository.testExpireTokens(LocalDateTime.now());
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteUnionMembershipNums")
    @ResponseBody
    public UserResponseDTO testDeleteUnionMembershipNums() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            unionMembershipNumRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @Transactional
    @GetMapping("/testDeleteAllUsers")
    @ResponseBody
    public UserResponseDTO testDeleteAllUsers() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            userRepository.deleteAll();
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @GetMapping("/testGetAllUsers")
    @ResponseBody
    public UserResponseDTO testGetAllUsers() {

        UserResponseDTO response = new UserResponseDTO();
        response.setResponseText("SUCCESS");

        try {
            response.setUsers(userRepository.findAll());
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @GetMapping("/testShowAnswers")
    @ResponseBody
    public QuestionnaireDTO testShowAnswers() {
        QuestionnaireDTO response = new QuestionnaireDTO();
        response.setResponseText("SUCCESS");

        try {
            response.setAnswers(answerRepository.findAll());
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @GetMapping("/testShowRegNumbQuestionnaires")
    @ResponseBody
    public List<RegistrationNumberQuestionnaire> testShowRegNumbQuestionnaires() {
        List<RegistrationNumberQuestionnaire> response = new ArrayList<>();

        try {
            response = RegNumberQuestionnaireRepository.findAll();
        }
        catch (Exception e) { }

        return response;
    }

    @GetMapping("/testSendingEmail")
    @ResponseBody
    public UserResponseDTO testSendEmail() {
        String text = "This is a text e-mail. please visit http://localhost:8080/user/testGetRedirection";

        UserResponseDTO response = new UserResponseDTO();

        try {
            emailService.sendSimpleMessage("veres.gabor.attila@gmail.com", "Testing", text);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }

        return response;

    }

    @PostMapping("/transTest")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public UserResponseDTO transactionTest(@RequestBody User user) throws Exception {
        User userWithoutRegNum = new User();
        userWithoutRegNum.setUserName("userWithoutRegNum");
        userWithoutRegNum.setPassword(encrypt("user123"));
        userWithoutRegNum.setPrivacyStatement(true);
        userWithoutRegNum.setRole(Enums.Role.USER);

        User user1 = new User();
        user1.setUserName("user88");
        user1.setPassword(encrypt("user123"));
        user1.setPrivacyStatement(true);
        user1.setRegistrationNum("1a");
        user1.setRole(Enums.Role.USER);

        RegistrationNumberQuestionnaire rnq1 = new RegistrationNumberQuestionnaire();
        rnq1.setQuestionnaireId(999L);
        rnq1.setRegistrationNum("333L");

        RegistrationNumberQuestionnaire rnq2 = new RegistrationNumberQuestionnaire();
        rnq2.setQuestionnaireId(555L);
        rnq2.setRegistrationNum("222L");

        List<RegistrationNumberQuestionnaire> rnqList = Arrays.asList(rnq1, rnq2);

        UserResponseDTO response = new UserResponseDTO();

        try {
            userService.testTransactionOperation(user1, userWithoutRegNum, rnqList);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            response.setResponseText("A hiba a következő: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/addAdmin")
    @ResponseBody
    public UserResponseDTO addAdmin(@RequestBody User user) throws Exception {
        User user1 = new User();
        user1.setUserName("Admin");
        user1.setPassword(encrypt("admin123"));
        user1.setEmail("adminEmail");
        user1.setEnabled(true);
        user1.setPrivacyStatement(true);
        user1.setRegistrationNum("1eqzsfaq342e24w542");
        user1.setRole(Enums.Role.ADMIN);

        UserResponseDTO response = new UserResponseDTO();

        try {
            userRepository.save(user1);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            response.setResponseText("A hiba a következő: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/addUnionMemberAdmin")
    @ResponseBody
    public UserResponseDTO addUnionMemberAdmin(@RequestBody User user) throws Exception {
        User user1 = new User();
        user1.setUserName("AdminSzakszerv");
        user1.setPassword(encrypt("admin456"));
        user1.setEmail("adminSzakszervEmail");
        user1.setEnabled(true);
        user1.setPrivacyStatement(true);
        user1.setRegistrationNum("1q35sgsafw4z4ays");
        user1.setRole(Enums.Role.UNION_MEMBER_ADMIN);

        UserResponseDTO response = new UserResponseDTO();

        try {
            userRepository.save(user1);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            response.setResponseText("A hiba a következő: " + e.getMessage());
        }
        return response;
    } */
}
