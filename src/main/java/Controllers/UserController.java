package Controllers;

import DTOs.QuestionnaireDTO;
import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.Questionnaire;
import Models.RegistrationNumberQuestionnaire;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.QuestionnaireRepository;
import Repositories.RegistrationNumberQuestionnaireRepository;
import Repositories.UnionMembershipNumRepository;
import Repositories.UserRepository;
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
    public UserRepository userRepository;

    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepository;

    @Autowired
    public IEmailService emailService;

    @Autowired
    public QuestionnaireRepository questionnaireRepository;

    @Autowired
    public RegistrationNumberQuestionnaireRepository regNumQuestRepository;

    @PostMapping("/saveUser")
    @ResponseBody
    public UserResponseDTO saveUser(@RequestBody User user) throws Exception {
        UserResponseDTO response = userService.saveUser(user);
        return userService.removeUserPassword(response);
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

    @GetMapping("/testGetUnionMembers")
    @ResponseBody
    public List<UnionMembershipNumber> testGetUnionMembershipNumbers() {
        return unionMembershipNumRepository.findAll();
    }

    @GetMapping("/testSendEmail")
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

    @GetMapping("/testExpireQuestionnaireDeadline")
    @ResponseBody
    public QuestionnaireDTO testExpireQuestionnaireDeadline() {
        LocalDateTime today = LocalDateTime.now().minusHours(1);
        QuestionnaireDTO response = new QuestionnaireDTO();
        try {
            Long id = Long.parseLong("3");
            Questionnaire questionnaire = questionnaireRepository.getQuestionnaireById(id);
            questionnaire.setDeadline(today);
            questionnaireRepository.save(questionnaire);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null) response.setResponseText(e.getMessage());
            else response.setResponseText(e.toString());
        }
        return response;
    }

    @GetMapping("/testGetAllQuestionnaires")
    @ResponseBody
    public QuestionnaireDTO testGetAllQuestionnaires() {
        LocalDateTime today = LocalDateTime.now().minusHours(1);
        QuestionnaireDTO response = new QuestionnaireDTO();
        try {
            List<Questionnaire> questionnaires = questionnaireRepository.findAllOrderByLastModDesc();
            response.setQuestionnaireList(questionnaires);
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
    @Transactional
    public UserResponseDTO transactionTest(@RequestBody User user) throws Exception {
        User userWithoutRegNum = new User();
        userWithoutRegNum.setUserName("userWithoutRegNum");
        userWithoutRegNum.setPassword(encrypt("user123"));
        userWithoutRegNum.setPrivacyStatement(true);
        userWithoutRegNum.setRole(Enums.Role.USER);

        User user1 = new User();
        user1.setUserName("user1");
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
            transactionOperation(user1, userWithoutRegNum, rnqList);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            response.setResponseText("A hiba a következő: " + e.getMessage());
        }
        return response;
    }

    private void transactionOperation(User user, User userWithoutRegNum, List<RegistrationNumberQuestionnaire> rnqList) {
        regNumQuestRepository.saveAll(rnqList);
        userRepository.save(user);
        userRepository.save(userWithoutRegNum);
    }

    /* @PostMapping("/addAdmin")
    @ResponseBody
    public UserResponseDTO addAdmin(@RequestBody User user) throws Exception {
        User user1 = new User();
        user1.setUserName("Admin");
        user1.setPassword(encrypt("admin123"));
        user1.setPrivacyStatement(true);
        user1.setRegistrationNum("1a");
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
        user1.setPrivacyStatement(true);
        user1.setRegistrationNum("1b");
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
