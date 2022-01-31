package Controllers;

import DTOs.QuestionnaireDTO;
import Models.Questionnaire;
import Models.RegistrationNumber;
import Models.User;
import Repositories.QuestionnaireRepository;
import Services.IQuestionnaireService;
import Services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@CrossOrigin
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    public IQuestionnaireService questionnaireService;
    @Autowired
    public IUserService userService;

    @PostMapping("/saveQuestionnaire")
    @ResponseBody
    public QuestionnaireDTO saveQuestionnaire(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.saveQuestionnaire(questionnaireDTO);
        return response;
    }

    @PostMapping("/getAllQuestionnaires")
    @ResponseBody
    public QuestionnaireDTO getAllQuestionnaires(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.getAllQuestionnaires(questionnaireDTO);
        return response;
    }

    @PostMapping("/deleteQuestionnaire")
    @ResponseBody
    public QuestionnaireDTO deleteQuestionnaire(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.deleteQuestionnaire(questionnaireDTO);
        return response;
    }

    @PostMapping("/getValidQuestionnaires")
    @ResponseBody
    public QuestionnaireDTO getValidQuestionnaires(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.getQuestionnairesForUsers(questionnaireDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/saveAnswers")
    @ResponseBody
    public QuestionnaireDTO saveAnswers(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.saveAnswers(questionnaireDTO);
        return userService.removeUserPassword(response);
    }

    @PostMapping("/getResult")
    @ResponseBody
    public QuestionnaireDTO getResult(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.getResult(questionnaireDTO);
        return userService.removeUserPassword(response);
    }
}
