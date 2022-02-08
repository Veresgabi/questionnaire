package Controllers;

import DTOs.QuestionnaireDTO;
import DTOs.UserRequestDTO;
import DTOs.UserResponseDTO;
import Models.Questionnaire;
import Models.UnionMembershipNumber;
import Models.User;
import Repositories.QuestionnaireRepository;
import Repositories.UnionMembershipNumRepository;
import Repositories.UserRepository;
import Services.IEmailService;
import Services.IQuestionnaireService;
import Services.IUserService;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @RequestMapping("/testGetRedirection")
    @ResponseBody
    public String testGetRedirection(@RequestParam(name = "id") String uuid) {
        String test = uuid;
        String newUuid = "sdfsda234234sdfe242";
        return
                "    <body>\n" +
                        "        <div>\n" +
                        "            <button onclick=\"setToken()\">Set Token</button>\n" +
                        "            <button onclick=\"getToken()\">Get Token</button>\n" +
                        "        </div>\n" +
                        "    </body>\n" +
                        "    <script>\n" +
                        "\n" +
                        "        function setToken() {\n" +
                        "            localStorage.setItem(\"questionnaireTokenUUID\", \"" + newUuid + "\");\n" +
                        "            console.log(localStorage.getItem(\"questionnaireTokenUUID\"));\n" +
                        "        }\n" +
                        "\n" +
                        "        function getToken() {\n" +
                        "            console.log(localStorage.getItem(\"questionnaireTokenUUID\"));\n" +
                        "        }\n" +
                        "    </script>\n" +
                        "</html>";

    }

    @GetMapping("/testExpireQuestionnaireDeadline")
    @ResponseBody
    public QuestionnaireDTO testExpireQuestionnaireDeadline() {
        LocalDateTime today = LocalDateTime.now().minusHours(1);
        QuestionnaireDTO response = new QuestionnaireDTO();
        try {
            Questionnaire questionnaire = questionnaireRepository.getQuestionnaireByTitle("próba");
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
