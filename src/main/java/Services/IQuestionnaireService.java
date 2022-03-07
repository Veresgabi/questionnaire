package Services;

import DTOs.QuestionnaireDTO;

public interface IQuestionnaireService {
    QuestionnaireDTO saveQuestionnaire(QuestionnaireDTO questionnaireDTO);
    QuestionnaireDTO getAllQuestionnaires(QuestionnaireDTO questionnaireDTO);
    QuestionnaireDTO getQuestionnairesForUsers(QuestionnaireDTO questionnaireDTO);
    QuestionnaireDTO deleteQuestionnaire(QuestionnaireDTO questionnaireDTO);
    QuestionnaireDTO saveAnswers(QuestionnaireDTO questionnaireDTO);
    QuestionnaireDTO getResult(QuestionnaireDTO questionnaireDTO);

    QuestionnaireDTO getApiResponse();
}
