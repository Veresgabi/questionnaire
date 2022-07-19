package Services;

import DTOs.AnswerValidationDTO;
import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.*;
import Repositories.*;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.transaction.Transactional;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.hibernate.internal.util.collections.ArrayHelper.toList;

@Service
public class QuestionnaireService implements IQuestionnaireService {

    @Autowired
    public QuestionnaireRepository questionnaireRepository;

    @Autowired
    public AnswerRepository answerRepository;

    @Autowired
    public TokenService tokenService;

    @Autowired
    public RegistrationNumberQuestionnaireRepository registrationNumberQuestionnaireRepository;

    @Autowired
    public UnionMembershipNumRepository unionMembershipNumRepository;

    @Autowired
    public RegNumberRepository regNumberRepository;

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private static final Integer minimumTextualAnswerLength = 4;

    private static final String onlyForUnionMemebers = "Csak szakszervezeti tagok számára";

    private QuestionnaireDTO apiResponse;

    private QuestionnaireDTO globalQuestionnaireDTO;

    public QuestionnaireDTO getApiResponse() {
        return apiResponse;
    }

    public QuestionnaireDTO getGlobalQuestionnaireDTO() {
        return globalQuestionnaireDTO;
    }

    public void setGlobalQuestionnaireDTO(QuestionnaireDTO globalQuestionnaireDTO) {
        this.globalQuestionnaireDTO = globalQuestionnaireDTO;
    }

    @Transactional
    public QuestionnaireDTO saveQuestionnaire(QuestionnaireDTO questionnaireDTO) {
        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        // 3. Check the role is compatible for the requested action
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId()) || !currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }

        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        Questionnaire questionnaire = questionnaireDTO.getQuestionnaire();
        boolean isEditMode = false;
        boolean isDeleted = false;

        if (questionnaire.getState() == null) questionnaire.setState(Enums.State.OPEN);

        if (questionnaire.getId() == null) {
            questionnaire.setCreatedAt(LocalDateTime.now());

            boolean isAnySimpleQuestion = false;
            boolean isAnyUnionMembershipQuestion = false;

            for (ScaleQuestion sq : questionnaire.getScaleQuestions()) {
                if (sq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }
            for (ChoiceQuestion cq : questionnaire.getChoiceQuestions()) {
                if (cq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }
            for (TextualQuestion tq : questionnaire.getTextualQuestions()) {
                if (tq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }

            if (isAnySimpleQuestion && !isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.SIMPLE);
            else if (!isAnySimpleQuestion && isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.UNION_MEMBERS_ONLY);
            else if (isAnySimpleQuestion && isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.MIXED);
        }
        else {
            isEditMode = true;
            // Validation
            try {
                Questionnaire validQuestionnaire = questionnaireRepository.findQuestionnaireById(questionnaire.getId());
                if (validQuestionnaire == null) {
                    // If edited questionnaire doesn't exist, we save it with a new id
                    isDeleted = true;
                    questionnaire.setId(null);
                }
                // Validate state change
                else if (questionnaire.isStateChange()) {
                    String validateStateResult = validateQuestionnaireStateChange(questionnaire, validQuestionnaire);
                    if (validateStateResult != "OK") {
                        response.setResponseText(validateStateResult);
                        response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
                        // setSuccessful(true) -> így a frontend-en bezáródik az esetlegesen megnyitott kérdőív szerkesztő felület
                        response.setSuccessful(true);
                        return response;
                    }
                }
                // Ignore editing if state isn't OPEN
                else if (!questionnaire.isStateChange() && validQuestionnaire.getState() != Enums.State.OPEN) {
                    response.setResponseText("A kérdőív már publikálásra került korábban, így már nem szerkeszthető!");
                    response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
                    // setSuccessful(true) -> így a frontend-en bezáródik az esetlegesen megnyitott kérdőív szerkesztő felület
                    response.setSuccessful(true);
                    return response;
                }
            }
            catch (Exception e) {
                if (e.getMessage() != null)
                    response.setResponseText("A kérdőív mentése sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A kérdőív mentése sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false);
                return response;
            }
            boolean isAnySimpleQuestion = false;
            boolean isAnyUnionMembershipQuestion = false;

            for (ScaleQuestion sq : questionnaire.getScaleQuestions()) {
                sq.setQuestionnaire(questionnaire);
                if (isDeleted) sq.setId(null);

                if (sq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }
            for (ChoiceQuestion cq : questionnaire.getChoiceQuestions()) {
                cq.setQuestionnaire(questionnaire);
                if (isDeleted) cq.setId(null);
                for (Choice c : cq.getChoices()) {
                    c.setChoiceQuestion(cq);
                    if (isDeleted) c.setId(null);
                }
                if (cq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }
            for (TextualQuestion tq : questionnaire.getTextualQuestions()) {
                tq.setQuestionnaire(questionnaire);
                if (isDeleted) tq.setId(null);

                if (tq.isUnionMembersOnly()) isAnyUnionMembershipQuestion = true;
                else isAnySimpleQuestion = true;
            }

            if (isAnySimpleQuestion && !isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.SIMPLE);
            else if (!isAnySimpleQuestion && isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.UNION_MEMBERS_ONLY);
            else if (isAnySimpleQuestion && isAnyUnionMembershipQuestion)
                questionnaire.setQuestionnaireType(Enums.QuestionnaireType.MIXED);
        }

        questionnaire.setLastModified(LocalDateTime.now());

        try {
            boolean needToDeleteAnswers = false;
            if (questionnaire.isStateChange() && questionnaire.getState() == Enums.State.PUBLISHED) {

                String deadlineString = questionnaire.getFormattedDeadline().replace("-", ".") + " 23:59";
                DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                LocalDateTime deadline = LocalDateTime.parse(deadlineString, formatter);

                if (deadline.isBefore(LocalDateTime.now().plusDays(1))) {
                    response.setResponseText("A kérdőív mentése sikertelen, mivel a megadott kitöltési határidő nem megfelelő, " +
                            "az ugyanis nem lehet korábbi, mint az aktuális nap másnapja.");
                    response.setSuccessful(false);
                    return response;
                }
                questionnaire.setDeadline(deadline);
            }
            else if (questionnaire.isStateChange() && questionnaire.getState() == Enums.State.RESULT_PUBLISHED) {
                response = setResultsForQuestionnaire(response, questionnaire);
                if (!response.isSuccessful()) return response;

                questionnaire = response.getQuestionnaire();
                needToDeleteAnswers = true;
            }
            boolean invalidTitle = false;
            List<Questionnaire> queriedQuestionnaires = questionnaireRepository.findQuestionnaireByTitle(questionnaire.getTitle());
            if (queriedQuestionnaires != null && !queriedQuestionnaires.isEmpty()) {
                if (isEditMode) {
                    for (Questionnaire q : queriedQuestionnaires) {
                        if (!q.getId().equals(questionnaire.getId())) invalidTitle = true;
                    }
                }
                else invalidTitle = true;
            }
            if (invalidTitle) {
                response.setResponseText("A kérdőív mentése sikertelen, mivel a megadott cím már szerepel az adatbázisban.");
                response.setSuccessful(false);
                return response;
            }

            response.setQuestionnaire(questionnaireRepository.save(questionnaire));
            if (needToDeleteAnswers) {
                try {
                    deleteAnswers(questionnaire);
                }
                catch (Exception e) {
                    Exception excp = e;
                }
            }
            if (isEditMode) {
                response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
            }
            response.setResponseText("A kérdőív mentése sikeresen megtörtént!");
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A kérdőív mentése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A kérdőív mentése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
    }

    @Transactional
    private void deleteAnswers(Questionnaire questionnaire) {
        registrationNumberQuestionnaireRepository.deleteByQuestionnaireId(questionnaire.getId());
        for (ScaleQuestion sq : questionnaire.getScaleQuestions()) {
            answerRepository.deleteByScaleQuestionId(sq.getId());
        }
        for (ChoiceQuestion cq : questionnaire.getChoiceQuestions()) {
            answerRepository.deleteByChoiceQuestionId(cq.getId());
        }
    }

    public QuestionnaireDTO getAllQuestionnaires(QuestionnaireDTO questionnaireDTO) {

        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        // 3. Check the role is compatible for the requested action
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId()) || !currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        try {
            // sort 1:
            // questionnaires.sort(Comparator.comparing(Questionnaire::getLastModified));
            // sort 2 (reverse):
            // Collections.sort(questionnaires, Collections.reverseOrder());
            List<Questionnaire> questionnaires = questionnaireRepository.findAllOrderByLastModDesc();
            questionnaires = closeQuestionnaires(questionnaires);
            response.setQuestionnaireList(questionnaires);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A kérdőívek lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A kérdőívek lekérdezése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
    }

    public QuestionnaireDTO getQuestionnairesForUsers(QuestionnaireDTO questionnaireDTO) {

        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!userValidateResponse.isAuthSuccess() || !userValidateResponse.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId())) {
            response.setExpiredPage(true);
            return response;
        }

        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.ADMIN) || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        try {
            List<Questionnaire> questionnaires;

            List<Long> halfFilledQuestionnaireIds = registrationNumberQuestionnaireRepository.findQuestionnaireIdsByRegistrationNumber(currentUser.getRegistrationNum());

            questionnaires = questionnaireRepository.findQuestionnairesForUnionMemberUsers(currentUser.getRegistrationNum());
            questionnaires = closeQuestionnaires(questionnaires);
            questionnaires = questionnaires
                    .stream()
                    .filter(qu -> qu.getState() == Enums.State.PUBLISHED || qu.getState() == Enums.State.RESULT_PUBLISHED)
                    // To hide union members questions
                    .map(qu -> {
                        if (currentUser.getRole().equals(Enums.Role.USER)) {

                            return new Questionnaire(qu.getId(), qu.getTitle(), qu.isUnionMembersOnly(),
                                    qu.getChoiceQuestions().stream().map(cq -> {
                                        if (cq.isUnionMembersOnly())
                                            return new ChoiceQuestion(null, cq.getNumber(), null, false, true,
                                                    onlyForUnionMemebers, null, 0, false, null);
                                        else return cq;
                                    }).collect(Collectors.toList()),
                                    qu.getScaleQuestions().stream().map(sq -> {
                                        if (sq.isUnionMembersOnly())
                                            return new ScaleQuestion(null, sq.getNumber(), null, true, onlyForUnionMemebers,
                                                    null, null, null, false, 0, null);
                                        else return sq;
                                    }).collect(Collectors.toList()),
                                    qu.getTextualQuestions().stream().map(tq -> {
                                        if (tq.isUnionMembersOnly())
                                            return new TextualQuestion(null, tq.getNumber(), null, true, true,
                                                    onlyForUnionMemebers, false, null, null);
                                        else return tq;
                                    }).collect(Collectors.toList()),

                                    qu.isPublished(), qu.getState(), qu.getCreatedAt(), qu.getFormattedCreatedAt(), qu.getLastModified(),
                                    qu.getFormattedLastModified(), qu.getDeadline(), qu.getQuestionnaireType(), qu.getFormattedDeadline(),
                                    qu.isStateChange(), qu.getCompletion(), qu.getCompletionRate(), qu.getRelatedUsers(), qu.getCompletionMako(),
                                    qu.getCompletionMakoBorrowed(), qu.getCompletionVac(), qu.getCompletionVacBorrowed());
                        }
                        else return qu;
                    })
                    .map(qu -> {
                        if (halfFilledQuestionnaireIds.contains(qu.getId())) return new Questionnaire(qu.getId(), qu.getTitle(), qu.isUnionMembersOnly(),
                                qu.getChoiceQuestions().stream().map(cq -> {
                                    if (!cq.isUnionMembersOnly()) {
                                        cq.setCompletedByCurrentUser(true);
                                        return cq;
                                    }
                                    else return cq;
                                }).collect(Collectors.toList()),
                                qu.getScaleQuestions().stream().map(sq -> {
                                    if (!sq.isUnionMembersOnly()) {
                                        sq.setCompletedByCurrentUser(true);
                                        return sq;
                                    }
                                    else return sq;
                                }).collect(Collectors.toList()),
                                qu.getTextualQuestions().stream().map(tq -> {
                                    if (!tq.isUnionMembersOnly()) {
                                        tq.setCompletedByCurrentUser(true);
                                        return tq;
                                    }
                                    else return tq;
                                }).collect(Collectors.toList()),

                                qu.isPublished(), qu.getState(), qu.getCreatedAt(), qu.getFormattedCreatedAt(), qu.getLastModified(),
                                qu.getFormattedLastModified(), qu.getDeadline(), qu.getQuestionnaireType(), qu.getFormattedDeadline(),
                                qu.isStateChange(), qu.getCompletion(), qu.getCompletionRate(), qu.getRelatedUsers(), qu.getCompletionMako(),
                                qu.getCompletionMakoBorrowed(), qu.getCompletionVac(), qu.getCompletionVacBorrowed());
                        else return qu;
                    })
                    .collect(Collectors.toList());

            response.setQuestionnaireList(questionnaires);
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A kérdőívek lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A kérdőívek lekérdezése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
    }

    @Transactional
    public QuestionnaireDTO deleteQuestionnaire(QuestionnaireDTO questionnaireDTO) {
        apiResponse = null;

        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        // 3. Check the role is compatible for the requested action
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId()) || !currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }

        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        try {
            questionnaireRepository.deleteById(questionnaireDTO.getQuestionnaire().getId());
            registrationNumberQuestionnaireRepository.deleteByQuestionnaireId(questionnaireDTO.getQuestionnaire().getId());
            response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
            for (ChoiceQuestion cq : questionnaireDTO.getQuestionnaire().getChoiceQuestions()) {
                answerRepository.deleteByChoiceQuestionId(cq.getId());
            }
            for (ScaleQuestion sq : questionnaireDTO.getQuestionnaire().getScaleQuestions()) {
                answerRepository.deleteByScaleQuestionId(sq.getId());
            }
            for (TextualQuestion tq : questionnaireDTO.getQuestionnaire().getTextualQuestions()) {
                answerRepository.deleteByTextualQuestionId(tq.getId());
            }
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if ((e.getClass().equals(EmptyResultDataAccessException.class) ||
                e.getClass().equals(MissingResourceException.class)) &&
                e.getMessage().contains(questionnaireDTO.getQuestionnaire().getId().toString())) {

                String questionnaireNotExists = "Az adatbázisban már nem található a törölni kívánt kérdőív.";
                response.setResponseText(questionnaireNotExists);

                response.setTokenUUID(null);
                apiResponse = response;
            }
            else {
                throw e;
                /* if (e.getMessage() != null)
                    response.setResponseText("A kérdőív törlése sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A kérdőív törlése sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false); */
            }
        }
        return response;
    }

    public QuestionnaireDTO saveAnswers(QuestionnaireDTO questionnaireDTO) {

        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId())) {
            response.setExpiredPage(true);
            return response;
        }

        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.ADMIN) || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        // Validation
        RegistrationNumberQuestionnaire regnumbQuest = null;
        Questionnaire validQuestionnaire = null;

        if (questionnaireDTO.getAnswers() != null) {
            try {
                validQuestionnaire = questionnaireRepository.findQuestionnaireById(
                        questionnaireDTO.getQuestionnaire().getId());
                if (validQuestionnaire == null) {
                    response.setResponseText("Az Ön által kitöltött kérdőív már nem található meg az" +
                            " adatbázisban, így a válaszok mentése sikertelen.");
                    response.setSuccessful(true);
                    return response;
                }
                else if (!validQuestionnaire.getState().equals(Enums.State.PUBLISHED)) {
                    response.setResponseText("Az Ön által kitöltött kérdőív már lezárásra került, így a válaszok " +
                            "mentése sikertelen.");
                    response.setSuccessful(true);
                    return response;
                }
                else if (validQuestionnaire.isUnionMembersOnly() && currentUser.getRole().equals(Enums.Role.USER)) {
                    response.setResponseText("Mivel az Ön szakszervezeti tagsági státusza időközben megszűnt, és a jelen " +
                            "kérdőív kizárólag szakszervezeti tagok részére szól, válaszai nem kerültek mentésre.");
                    response.setSuccessful(true);
                    return response;
                }
                // Csekkoljuk, hogy már kitöltötte-e

                List<RegistrationNumberQuestionnaire> regnumbQuests = registrationNumberQuestionnaireRepository
                        .findByRegNumberAndQuestionnaireId(currentUser.getRegistrationNum(), validQuestionnaire.getId());
                if (regnumbQuests != null && !regnumbQuests.isEmpty()) regnumbQuest = regnumbQuests.get(0);
            }
            catch (Exception e) {
                if (e.getMessage() != null)
                    response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false);
                return response;
            }
        }

        try {
            // Validate Answers in request
            AnswerValidationDTO answerValidationDTO;
            List<Answer> savedAnswers = new ArrayList<>();

            List<ScaleQuestion> expectedScaleQuestions;
            List<ChoiceQuestion> expectedChoiceQuestions;
            List<TextualQuestion> expectedTextualQuestions;
            List<TextualQuestion> expectedOptionalTextualQuestionIds = new ArrayList<>();

            if (regnumbQuest != null) {
                // Ha Csak szakszervezeti tagok részére szóló válaszokat várunk
                if (regnumbQuest.getCompletedLevel().equals(Enums.CompletedLevel.PARTIAL)
                        && currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {

                    Integer answersCountBefore = questionnaireDTO.getAnswers().size();

                    expectedScaleQuestions = validQuestionnaire.getScaleQuestions()
                            .stream()
                            .filter(sq -> sq.isUnionMembersOnly())
                            .collect(Collectors.toList());
                    expectedChoiceQuestions = validQuestionnaire.getChoiceQuestions()
                            .stream()
                            .filter(cq -> cq.isUnionMembersOnly())
                            .collect(Collectors.toList());
                    expectedTextualQuestions = validQuestionnaire.getTextualQuestions()
                            .stream()
                            .filter(tq -> tq.isUnionMembersOnly())
                            .collect(Collectors.toList());

                    // Validate answers
                    answerValidationDTO = validateAnswers(questionnaireDTO.getAnswers(), expectedTextualQuestions,
                            expectedScaleQuestions, expectedChoiceQuestions);

                    if (answerValidationDTO == null) {
                        response.setResponseText("A beérkezett válaszai nem érvényesek!");
                        response.setSuccessful(false);
                        return response;
                    }

                    savedAnswers = answerValidationDTO.getValidAnswers();
                    String invalidAnswersTextPreFix = "";
                    String inValidAnswersTextSuffix = "azok vélhetően olyan kérdésekre vonatkoznak, amelyeket Ön korábban " +
                            "már megválaszolt, így ezen válaszok nem kerültek ismét mentésre.";

                    if (savedAnswers.size() == 0) invalidAnswersTextPreFix = "A beküldött válaszai érvénytelenek, ";
                    else invalidAnswersTextPreFix = "A beküldött válaszai között érvénytelenek is találhatóak, ";

                    if (answerValidationDTO.getMissedScaleQuestions().size() == 0
                            && answerValidationDTO.getMissedChoiceQuestions().size() == 0
                            && answerValidationDTO.getMissedTextualQuestions().size() == 0) {

                        // Csak opcionális kérdés volt kitölthető
                        if (savedAnswers.size() == 0) response.setResponseText("A kérdőív kitöltése sikeresen megtörtént.");
                        // Nem csak opcionális kérdés volt kitölthető
                        else {
                            response.setResponseText("A válaszok mentése sikeresen megtörtént!");
                            answerRepository.saveAll(savedAnswers);
                            globalQuestionnaireDTO = questionnaireDTO;
                        }

                            // Ha a beküldött válaszok között eredetileg több, vélhetően korábban már beküldött válaszokat is voltak
                        if (savedAnswers.size() < answersCountBefore) {
                            response.setResponseText(response.getResponseText() + " " + invalidAnswersTextPreFix
                                    + inValidAnswersTextSuffix);
                        }
                        regnumbQuest.setCompletedLevel(Enums.CompletedLevel.TOTAL);
                        registrationNumberQuestionnaireRepository.save(regnumbQuest);
                        response.setSuccessful(true);
                        return response;
                    }
                    // Ha van kihagyott kérdés, semmi nem mentődik
                    else if (answerValidationDTO.getMissedScaleQuestions().size() > 0
                            || answerValidationDTO.getMissedChoiceQuestions().size() > 0
                            || answerValidationDTO.getMissedTextualQuestions().size() > 0) {

                        // Ez csak JS hackelés útján lehetséges
                        if (savedAnswers.size() > 0) {
                            response.setResponseText("A beküldött válaszai hiányosak, azok nem fedik le az összes " +
                                    "megválaszolandó kérdést, így ezen válaszainak mentése nem történt meg. ");

                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(response.getResponseText() + invalidAnswersTextPreFix
                                        + inValidAnswersTextSuffix);
                            }
                        }
                        else {
                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(invalidAnswersTextPreFix + inValidAnswersTextSuffix);
                            }
                            else if (savedAnswers.size() == answersCountBefore) {
                                response.setExpiredPage(true);
                                return response;
                            }
                        }
                    }
                    response.setSuccessful(true);
                }
                // Ha már nem várunk válszokat
                else if ((regnumbQuest.getCompletedLevel().equals(Enums.CompletedLevel.PARTIAL) && currentUser.getRole().equals(Enums.Role.USER))
                        || regnumbQuest.getCompletedLevel().equals(Enums.CompletedLevel.TOTAL)) {
                    response.setResponseText("Ezt a kérdőívet Ön már kitöltötte, így a válaszai nem kerülnek ismét mentésre.");
                    response.setSuccessful(true);
                    return response;
                }
            }
            // Ha regnumbQuest == null.
            else {
                regnumbQuest = new RegistrationNumberQuestionnaire();
                regnumbQuest.setQuestionnaireId(validQuestionnaire.getId());
                regnumbQuest.setRegistrationNum(currentUser.getRegistrationNum());

                // Ha csak szimpla user-ek részére szóló válaszokat várunk
                if (currentUser.getRole().equals(Enums.Role.USER)) {

                    Integer answersCountBefore = questionnaireDTO.getAnswers().size();

                    expectedScaleQuestions = validQuestionnaire.getScaleQuestions()
                            .stream()
                            .filter(sq -> !sq.isUnionMembersOnly())
                            .collect(Collectors.toList());
                    expectedChoiceQuestions = validQuestionnaire.getChoiceQuestions()
                            .stream()
                            .filter(cq -> !cq.isUnionMembersOnly())
                            .collect(Collectors.toList());
                    expectedTextualQuestions = validQuestionnaire.getTextualQuestions()
                            .stream()
                            .filter(tq -> !tq.isUnionMembersOnly())
                            .collect(Collectors.toList());

                    // Validate answers
                    answerValidationDTO = validateAnswers(questionnaireDTO.getAnswers(), expectedTextualQuestions,
                            expectedScaleQuestions, expectedChoiceQuestions);

                    if (answerValidationDTO == null) {
                        response.setResponseText("A beérkezett válaszai nem érvényesek!");
                        response.setSuccessful(false);
                        return response;
                    }
                    savedAnswers = answerValidationDTO.getValidAnswers();

                    String invalidAnswersTextPreFix = "Mivel az Ön szakszervezeti tagsági státusza időközben megszűnt,";
                    String inValidAnswersTextSuffix = "";

                    if (savedAnswers.size() == 0) inValidAnswersTextSuffix = " válaszai nem kerültek mentésre.";
                    else inValidAnswersTextSuffix = " a szakszervezeti tagoknak szóló kérdésekre adott válaszai " +
                            " nem kerültek mentésre.";

                    if (answerValidationDTO.getMissedScaleQuestions().size() == 0
                            && answerValidationDTO.getMissedChoiceQuestions().size() == 0
                            && answerValidationDTO.getMissedTextualQuestions().size() == 0) {

                        // Csak opcionális kérdés volt kitölthető
                        if (savedAnswers.size() == 0)
                            response.setResponseText("A kérdőív kitöltése sikeresen megtörtént.");
                        // Nem csak opcionális kérdés volt kitölthető
                        else {
                            response.setResponseText("A válaszok mentése sikeresen megtörtént!");
                            answerRepository.saveAll(savedAnswers);
                            globalQuestionnaireDTO = questionnaireDTO;
                        }

                        // Ha a beküldött válaszok között eredetileg több válasz is volt
                        if (savedAnswers.size() < answersCountBefore) {
                            response.setResponseText(response.getResponseText() + " " + invalidAnswersTextPreFix
                                    + inValidAnswersTextSuffix);
                        }

                        if (validQuestionnaire.getQuestionnaireType().equals(Enums.QuestionnaireType.SIMPLE))
                            regnumbQuest.setCompletedLevel(Enums.CompletedLevel.TOTAL);
                        else if (validQuestionnaire.getQuestionnaireType().equals(Enums.QuestionnaireType.MIXED))
                            regnumbQuest.setCompletedLevel(Enums.CompletedLevel.PARTIAL);

                        registrationNumberQuestionnaireRepository.save(regnumbQuest);
                        response.setSuccessful(true);
                        return response;
                    }
                    // Ha van kihagyott kérdés, semmi nem mentődik
                    else if (answerValidationDTO.getMissedScaleQuestions().size() > 0
                            || answerValidationDTO.getMissedChoiceQuestions().size() > 0
                            || answerValidationDTO.getMissedTextualQuestions().size() > 0) {

                        // Ez csak JS hackelés útján lehetséges
                        if (savedAnswers.size() > 0) {
                            response.setResponseText("A beküldött válaszai hiányosak, azok nem fedik le az összes " +
                                    "megválaszolandó kérdést, így ezen válaszainak mentése nem történt meg. ");

                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(response.getResponseText() + invalidAnswersTextPreFix
                                        + inValidAnswersTextSuffix);
                            }
                        } else {
                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(response.getResponseText() + invalidAnswersTextPreFix
                                        + inValidAnswersTextSuffix);
                            } else if (savedAnswers.size() == answersCountBefore) {
                                response.setResponseText("A válaszok mentése sikertelen, ugyanis a beküldött objektum nem tartalmazza " +
                                        "válaszait.");
                                response.setSuccessful(false);
                                return response;
                            }
                        }
                    }
                    response.setSuccessful(true);
                }
                // Ha az összes választ várjuk
                else if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {

                    Integer answersCountBefore = questionnaireDTO.getAnswers().size();

                    expectedScaleQuestions = validQuestionnaire.getScaleQuestions();
                    expectedChoiceQuestions = validQuestionnaire.getChoiceQuestions();
                    expectedTextualQuestions = validQuestionnaire.getTextualQuestions();

                    // Validate answers
                    answerValidationDTO = validateAnswers(questionnaireDTO.getAnswers(), expectedTextualQuestions,
                            expectedScaleQuestions, expectedChoiceQuestions);

                    if (answerValidationDTO == null) {
                        response.setResponseText("A beérkezett válaszai nem érvényesek!");
                        response.setSuccessful(false);
                        return response;
                    }
                    savedAnswers = answerValidationDTO.getValidAnswers();

                    String invalidAnswersTextPreFix = "";
                    String inValidAnswersTextSuffix = "azok olyan kérdésekre vonatkoznak, amelyek nem azonosíthatóak, " +
                            "így ezen válaszok nem kerültek mentésre.";

                    if (savedAnswers.size() == 0) invalidAnswersTextPreFix = "A beküldött válaszai érvénytelenek, ";
                    else invalidAnswersTextPreFix = "A beküldött válaszai között érvénytelenek is találhatóak, ";

                    if (answerValidationDTO.getMissedScaleQuestions().size() == 0
                            && answerValidationDTO.getMissedChoiceQuestions().size() == 0
                            && answerValidationDTO.getMissedTextualQuestions().size() == 0) {

                        // Csak opcionális kérdés volt kitölthető
                        if (savedAnswers.size() == 0)
                            response.setResponseText("A kérdőív kitöltése sikeresen megtörtént.");
                            // Nem csak opcionális kérdés volt kitölthető
                        else {
                            response.setResponseText("A válaszok mentése sikeresen megtörtént!");
                            answerRepository.saveAll(savedAnswers);
                            globalQuestionnaireDTO = questionnaireDTO;
                        }

                        // Ha a beküldött válaszok között eredetileg több válasz is volt
                        if (savedAnswers.size() < answersCountBefore) {
                            response.setResponseText(response.getResponseText() + " " + invalidAnswersTextPreFix
                                    + inValidAnswersTextSuffix);
                        }
                        regnumbQuest.setCompletedLevel(Enums.CompletedLevel.TOTAL);
                        registrationNumberQuestionnaireRepository.save(regnumbQuest);
                        response.setSuccessful(true);
                        return response;
                    }
                    // Ha van kihagyott, szimpla user-eknek szóló kérdés, semmi nem mentődik
                    else if (answerValidationDTO.getMissedScaleQuestions().size() > 0
                            || answerValidationDTO.getMissedChoiceQuestions().size() > 0
                            || answerValidationDTO.getMissedTextualQuestions().size() > 0) {

                        boolean allMissedQuestionIsForUnionMembersOnly = true;
                        for (ScaleQuestion sq : answerValidationDTO.getMissedScaleQuestions()) {
                            if (!sq.isUnionMembersOnly()) allMissedQuestionIsForUnionMembersOnly = false;
                        }
                        for (ChoiceQuestion cq : answerValidationDTO.getMissedChoiceQuestions()) {
                            if (!cq.isUnionMembersOnly()) allMissedQuestionIsForUnionMembersOnly = false;
                        }
                        for (TextualQuestion tq : answerValidationDTO.getMissedTextualQuestions()) {
                            if (!tq.isUnionMembersOnly()) allMissedQuestionIsForUnionMembersOnly = false;
                        }

                        if (savedAnswers.size() > 0) {
                            if (allMissedQuestionIsForUnionMembersOnly) {
                                response.setResponseText("A válaszok mentése sikeresen megtörtént! Mivel az Ön státusza " +
                                        "időközben szakszervezeti tagsággá módosult, a szakszervezeti tagok részére " +
                                        "szóló kérdéseket lehetősége van megválaszolni.");
                                answerRepository.saveAll(savedAnswers);
                                globalQuestionnaireDTO = questionnaireDTO;
                                regnumbQuest.setCompletedLevel(Enums.CompletedLevel.PARTIAL);
                                registrationNumberQuestionnaireRepository.save(regnumbQuest);
                            }
                            // Ez csak JS hackelés útján lehetséges
                            else {
                                response.setResponseText("A beküldött válaszai hiányosak, azok nem fedik le az összes " +
                                        "megválaszolandó kérdést, így ezen válaszainak mentése nem történt meg. ");
                            }

                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(response.getResponseText() + invalidAnswersTextPreFix
                                        + inValidAnswersTextSuffix);
                            }
                        } else {
                            if (savedAnswers.size() < answersCountBefore) {
                                response.setResponseText(response.getResponseText() + invalidAnswersTextPreFix
                                        + inValidAnswersTextSuffix);
                            } else if (savedAnswers.size() == answersCountBefore) {
                                response.setExpiredPage(true);
                                return response;
                            }
                        }
                    }
                    response.setSuccessful(true);
                }
            }
        }
        catch (Exception e) {
            if (e.getMessage() != null) {
                response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e.getMessage());
            }
            else response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
    }

    private AnswerValidationDTO validateAnswers(List<Answer> answers, List<TextualQuestion> validTextualQuestions,
                                                List<ScaleQuestion> validScaleQuestions,
                                                List<ChoiceQuestion> validChoiceQuestions) {

        AnswerValidationDTO answerValidationDTO = new AnswerValidationDTO();

        List<Answer> validAnswers = new ArrayList<>();
        List<TextualQuestion> missedTextualQuestions = new ArrayList<>();
        List<ScaleQuestion> missedScaleQuestions = new ArrayList<>();
        List<ChoiceQuestion> missedChoiceQuestions = new ArrayList<>();

        // --------------------------------------------------
        // Validate Textual Answers
        // --------------------------------------------------
        for (TextualQuestion textualQuestion : validTextualQuestions) {
            Answer answer = answers
                    .stream()
                    .filter(a -> a.getTextualQuestionId() != null && a.getTextualQuestionId()
                            .equals(textualQuestion.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer != null) {

                if (answer.getContent() != null) {
                    if (answer.getContent().length() < minimumTextualAnswerLength && !textualQuestion.getOptional()) return null;

                    answer.setUnionMembersOnly(textualQuestion.isUnionMembersOnly());
                    validAnswers.add(answer);
                }
                else if (answer.getContent() == null && !textualQuestion.getOptional()) {
                    missedTextualQuestions.add(textualQuestion);
                }
            }
            else if (answer == null && !textualQuestion.getOptional()) {
                missedTextualQuestions.add(textualQuestion);
            }
        }

        // --------------------------------------------------
        // Validate Scale Answers
        // --------------------------------------------------
        for (ScaleQuestion scaleQuestion : validScaleQuestions) {
            Answer answer = answers
                    .stream()
                    .filter(a -> a.getScaleQuestionId() != null && a.getScaleQuestionId().equals(scaleQuestion.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer != null) {
                // Validate rateNumber
                try {
                    Integer ratedNumber = Integer.parseInt(answer.getContent());
                    if (ratedNumber < scaleQuestion.getScaleMinNumber()
                            || ratedNumber > scaleQuestion.getScaleMaxNumber()) {
                        return null;
                    }
                }
                catch (Exception e) {
                    return null;
                }
                answer.setUnionMembersOnly(scaleQuestion.isUnionMembersOnly());
                validAnswers.add(answer);
            }
            else missedScaleQuestions.add(scaleQuestion);
        }

        // --------------------------------------------------
        // Validate Choice Answers
        // --------------------------------------------------
        for (ChoiceQuestion choiceQuestion : validChoiceQuestions) {
            Answer answer = answers
                    .stream()
                    .filter(a -> a.getChoiceQuestionId() != null && a.getChoiceQuestionId().equals(choiceQuestion.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer != null) {
                // Validate Selected Choices
                List<String> validChoiceMarks = choiceQuestion.getChoices()
                        .stream()
                        .map(c -> c.getMark())
                        .collect(Collectors.toList());

                List<ChoiceAnswer> validChoiceAnswers = new ArrayList<>();

                for (String choiceMark : validChoiceMarks) {
                    ChoiceAnswer choiceAnswer = answer.getChoiceAnswers().stream().filter(ca -> ca.getContent() != null
                            && ca.getContent().equals(choiceMark)).findFirst().orElse(null);

                    if (choiceAnswer != null) validChoiceAnswers.add(choiceAnswer);
                }
                if (validChoiceAnswers.size() == 0 || (!choiceQuestion.isMultipleChoiceEnabled() && validChoiceAnswers.size() > 1)) {
                    return null;
                }
                answer.setChoiceAnswers(validChoiceAnswers);
                answer.setUnionMembersOnly(choiceQuestion.isUnionMembersOnly());
                validAnswers.add(answer);
            }
            else missedChoiceQuestions.add(choiceQuestion);
        }
        answerValidationDTO.setValidAnswers(validAnswers);
        answerValidationDTO.setMissedTextualQuestions(missedTextualQuestions);
        answerValidationDTO.setMissedScaleQuestions(missedScaleQuestions);
        answerValidationDTO.setMissedChoiceQuestions(missedChoiceQuestions);

        return answerValidationDTO;
    }

    public QuestionnaireDTO getResult(QuestionnaireDTO questionnaireDTO) {

        QuestionnaireDTO response = new QuestionnaireDTO();

        // 1. Check token is valid
        UserResponseDTO userValidateResponse = tokenService.checkToken(questionnaireDTO.getTokenUUID());
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        // If not valid, return the response!
        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        User currentUser = userValidateResponse.getUser();

        // 2. Check the current user of the page is equals to user of token
        if (!currentUser.getId().equals(questionnaireDTO.getUser().getId())) {
            response.setExpiredPage(true);
            return response;
        }

        // 3. Check the role is compatible for the requested action
        if (currentUser.getRole().equals(Enums.Role.ADMIN)) {
            response.setExpiredPage(true);
            return response;
        }
        // 4. Refresh token
        userValidateResponse = tokenService.refreshToken(userValidateResponse.getTokenUUID(), currentUser);
        response.setAuthSuccess(userValidateResponse.isAuthSuccess());
        response.setSuccessful(userValidateResponse.isSuccessful());
        response.setResponseText(userValidateResponse.getResponseText());

        if (!response.isAuthSuccess() || !response.isSuccessful()) return response;

        response.setTokenUUID(userValidateResponse.getTokenUUID());
        response.setUser(userValidateResponse.getUser());

        try {
            // Validation
            Questionnaire questionnaire = questionnaireRepository.findQuestionnaireById(questionnaireDTO.getQuestionnaire().getId());
            if (questionnaire != null) {
                if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN) && questionnaire.getState().equals(Enums.State.OPEN)) {
                    response.setResponseText("A kérdőív még nem került publikálásra.");
                    response.setSuccessful(false);
                    return response;
                }
                else if (currentUser.getRole().equals(Enums.Role.USER) && questionnaire.isUnionMembersOnly()) {
                    response.setResponseText("A kérdőív eredményeinek megtekintésére csak szakszervezeti tagoknak van lehetőségük.");
                    response.setSuccessful(false);
                    return response;
                }
                else if ((currentUser.getRole().equals(Enums.Role.USER) || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER))
                        && !questionnaire.getState().equals(Enums.State.RESULT_PUBLISHED)) {
                    response.setResponseText("A kérdőív eredményei még nem kerültek publikálásra.");
                    response.setSuccessful(false);
                    return response;
                }
                response.setQuestionnaire(questionnaire);
            }
            else {
                response.setResponseText("Az adatbázisban nem található a lekérdezett kérdőív.");
                response.setSuccessful(false);
                return response;
            }

            for (TextualQuestion tq : response.getQuestionnaire().getTextualQuestions()) {

                if (!tq.isUnionMembersOnly() || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)
                        || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                    List<Answer> answers = answerRepository.findByTextualQuestionId(tq.getId());
                    tq.setAnswers(answers);
                }
                else {
                    tq.setType(null);
                    tq.setQuestion(onlyForUnionMemebers);
                }
            }

            // Ha ResultPublished, akkor a lekért kérdőív már tartalmazza a statisztikákat, kivéve a textualQuestion-öknél!
            if (response.getQuestionnaire().getState().equals(Enums.State.RESULT_PUBLISHED)) {
                for (ScaleQuestion sq : response.getQuestionnaire().getScaleQuestions()) {
                    if (sq.isUnionMembersOnly() && currentUser.getRole().equals(Enums.Role.USER)) {
                        sq.setType(null);
                        sq.setQuestion(onlyForUnionMemebers);
                    }
                }
                for (ChoiceQuestion cq : response.getQuestionnaire().getChoiceQuestions()) {

                    if (cq.isUnionMembersOnly() && currentUser.getRole().equals(Enums.Role.USER)) {
                        cq.setType(null);
                        cq.setQuestion(onlyForUnionMemebers);
                    }
                }
                response.setSuccessful(true);
                globalQuestionnaireDTO = response;
            }
            else {
                for (ScaleQuestion sq : response.getQuestionnaire().getScaleQuestions()) {
                    List<Answer> answers = answerRepository.findByScaleQuestionId(sq.getId());

                    String formattedAverageRate = "0";
                    double averageRate = answers.stream().map(
                            a -> Double.parseDouble(a.getContent())).reduce(0.0, Double::sum) / answers.size();
                    if (!Double.isNaN(averageRate)) formattedAverageRate = decimalFormat.format(averageRate);

                    sq.setAverageRate(formattedAverageRate);
                    sq.setCompletion(answers.size());
                }
                for (ChoiceQuestion cq : response.getQuestionnaire().getChoiceQuestions()) {

                    List<Answer> answers = answerRepository.findByChoiceQuestionId(cq.getId());
                    List<ChoiceAnswer> choiceAnswers = answers.stream().flatMap(a -> a.getChoiceAnswers().stream()).collect(Collectors.toList());

                    for (Choice c : cq.getChoices()) {
                        Integer numberOfSelection = (int) choiceAnswers.stream().filter(ca -> ca.getContent().equals(c.getMark())).count();

                        String formattedPercentOfSelection = "0.00 %";
                        double percentOfSelection = Double.valueOf((double) numberOfSelection / answers.size() * 100);
                        if (!Double.isNaN(percentOfSelection)) formattedPercentOfSelection = decimalFormat.format(percentOfSelection) + " %";

                        c.setNumberOfSelection(numberOfSelection);
                        c.setPercentOfSelection(formattedPercentOfSelection);
                    }
                    cq.setCompletion(answers.size());
                }
                Integer filledQuestionnaires = registrationNumberQuestionnaireRepository.getNumberOfFills(questionnaire.getId());
                Integer relatedUsers;

                if (questionnaire.isUnionMembersOnly()) relatedUsers = unionMembershipNumRepository.getNumberOfUnionMemberUsers();
                else relatedUsers = regNumberRepository.getNumberOfUsers();

                response.getQuestionnaire().setCompletion(filledQuestionnaires);
                response.getQuestionnaire().setRelatedUsers(relatedUsers);
                String formattedCompletionRate = "0.00 %";

                double completionRate = Double.valueOf((double) filledQuestionnaires / relatedUsers * 100);
                if (!Double.isNaN(completionRate)) formattedCompletionRate = decimalFormat.format(completionRate) + " %";

                response.getQuestionnaire().setCompletionRate(formattedCompletionRate);
                globalQuestionnaireDTO = response;
                response.setSuccessful(true);
            }
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
    }

    private QuestionnaireDTO setResultsForQuestionnaire(QuestionnaireDTO questionnaireDTO, Questionnaire questionnaire) {
        try {
            for (ScaleQuestion sq : questionnaire.getScaleQuestions()) {

                List<Answer> answers = answerRepository.findByScaleQuestionId(sq.getId());

                String formattedAverageRate = "0";
                double averageRate = answers.stream().map(
                        a -> Double.parseDouble(a.getContent())).reduce(0.0, Double::sum) / answers.size();
                if (!Double.isNaN(averageRate)) formattedAverageRate = decimalFormat.format(averageRate);

                sq.setAverageRate(formattedAverageRate);
                sq.setCompletion(answers.size());
            }
            for (ChoiceQuestion cq : questionnaire.getChoiceQuestions()) {

                List<Answer> answers = answerRepository.findByChoiceQuestionId(cq.getId());
                List<ChoiceAnswer> choiceAnswers = answers.stream().flatMap(a -> a.getChoiceAnswers().stream()).collect(Collectors.toList());

                for (Choice c : cq.getChoices()) {
                    Integer numberOfSelection = (int) choiceAnswers.stream().filter(ca -> ca.getContent().equals(c.getMark())).count();

                    String formattedPercentOfSelection = "0.00 %";
                    double percentOfSelection = Double.valueOf((double) numberOfSelection / answers.size() * 100);
                    if (!Double.isNaN(percentOfSelection)) formattedPercentOfSelection = decimalFormat.format(percentOfSelection) + " %";

                    c.setNumberOfSelection(numberOfSelection);
                    c.setPercentOfSelection(formattedPercentOfSelection);
                }
                cq.setCompletion(answers.size());
            }
            Integer filledQuestionnaires = registrationNumberQuestionnaireRepository.getNumberOfFills(questionnaire.getId());
            Integer relatedUsers;

            if (questionnaire.isUnionMembersOnly()) relatedUsers = unionMembershipNumRepository.getNumberOfUnionMemberUsers();
            else relatedUsers = regNumberRepository.getNumberOfUsers();

            questionnaire.setCompletion(filledQuestionnaires);
            questionnaire.setRelatedUsers(relatedUsers);
            String formattedCompletionRate = "0.00 %";

            double completionRate = Double.valueOf((double) filledQuestionnaires / relatedUsers * 100);
            if (!Double.isNaN(completionRate)) formattedCompletionRate = decimalFormat.format(completionRate) + " %";

            questionnaire.setCompletionRate(formattedCompletionRate);

            questionnaireDTO.setQuestionnaire(questionnaire);
            questionnaireDTO.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                questionnaireDTO.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else questionnaireDTO.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e);

            questionnaireDTO.setSuccessful(false);
        }
        return questionnaireDTO;
    }

    private String validateQuestionnaireStateChange(Questionnaire questionnaire, Questionnaire queriedQuestionnaire) {

        String result = "OK";

        final String prefix = "A kérdőív ";
        String action = "";
        final String midfix = " nem lehetséges, mert ";
        String reason = "";
        final String suffix = " került.";

        if (questionnaire.getState().getValue() - queriedQuestionnaire.getState().getValue() != 1) {

            if (questionnaire.getState() == Enums.State.OPEN)  action = "nyitott státuszba helyezése";
            else if (questionnaire.getState() == Enums.State.PUBLISHED) action = "publikálása";
            else if (questionnaire.getState() == Enums.State.CLOSED) action = "lezárása";
            else if (questionnaire.getState() == Enums.State.RESULT_PUBLISHED) action = "eredményeinek közzététele";

            if (queriedQuestionnaire.getState() == Enums.State.OPEN) reason = "az nyitott státuszba";
            else if (queriedQuestionnaire.getState() == Enums.State.PUBLISHED) reason = "az már publikálásra";
            else if (queriedQuestionnaire.getState() == Enums.State.CLOSED) reason = "az már lezárásra";
            else if (queriedQuestionnaire.getState() == Enums.State.RESULT_PUBLISHED) reason = "annak eredménye már közzétételre";

            result = prefix + action + midfix + reason + suffix;
        }
        return result;
    }

    private List<Questionnaire> closeQuestionnaires(List<Questionnaire> questionnaires) {
        boolean needToSaveQuestList = false;
        for (Questionnaire q : questionnaires) {
            if (q.getState() == Enums.State.PUBLISHED
                    && q.getDeadline() != null && q.getDeadline().isBefore(LocalDateTime.now())) {
                q.setState(Enums.State.CLOSED);
                needToSaveQuestList = true;
            }
        }
        if (needToSaveQuestList) questionnaireRepository.saveAll(questionnaires);

        return questionnaires;
    }
}
