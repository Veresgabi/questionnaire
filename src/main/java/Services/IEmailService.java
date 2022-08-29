package Services;

import DTOs.QuestionnaireDTO;

public interface IEmailService {
    void sendSimpleMessage(String to, String subject, String text) throws Exception;
    QuestionnaireDTO sendNewQuestionnaireEmail(QuestionnaireDTO questionnaireDTO);
}
