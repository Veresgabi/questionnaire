package Controllers;

import DTOs.QuestionnaireDTO;
import Models.Answer;
import Models.Questionnaire;
import Models.RegistrationNumber;
import Models.User;
import Repositories.QuestionnaireRepository;
import Services.IQuestionnaireService;
import Services.IUserService;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@CrossOrigin
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    public IQuestionnaireService questionnaireService;
    @Autowired
    public IUserService userService;
    @Autowired
    public QuestionnaireRepository questionnaireRepository;

    @PostMapping("/saveQuestionnaire")
    @ResponseBody
    public QuestionnaireDTO saveQuestionnaire(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = new QuestionnaireDTO();
        try {
            response = questionnaireService.saveQuestionnaire(questionnaireDTO);
        }
        catch (Exception e) {
            response.setResponseText(e.toString());
        }
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
        QuestionnaireDTO response = new QuestionnaireDTO();

        try {
            response = questionnaireService.deleteQuestionnaire(questionnaireDTO);
        }
        catch (Exception e) {
            // Ha a törölni kívánt kérdőív már nem található
            if (questionnaireService.getApiResponse() != null) {
                response = questionnaireService.getApiResponse();

                try { response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());}
                catch (Exception exception) { }
            }
            else {
                if (e.getMessage() != null)
                    response.setResponseText("A kérdőív törlése sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A kérdőív törlése sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false);
            }
        }
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

    @PostMapping("/turnPage")
    @ResponseBody
    public QuestionnaireDTO turnPage(@RequestBody QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = questionnaireService.turnPage(questionnaireDTO);
        return response;
    }

    @GetMapping("/testCloseQuestionnaire")
    @ResponseBody
    public QuestionnaireDTO testExpireQuestionnaireDeadline(@RequestParam String id) {
        LocalDateTime today = LocalDateTime.now().minusHours(1);
        QuestionnaireDTO response = new QuestionnaireDTO();
        try {
            Long longId = Long.parseLong(id);
            Questionnaire questionnaire = questionnaireRepository.getQuestionnaireById(longId);
            questionnaire.setState(Enums.State.CLOSED);
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
}
