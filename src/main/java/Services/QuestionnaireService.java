package Services;

import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.*;
import Repositories.*;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

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

    private static final String onlyForUnionMemebers = "Csak szakszervezeti tagok számára";

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

        if (questionnaire.getId() == null) questionnaire.setCreatedAt(LocalDateTime.now());
        else {
            isEditMode = true;
            // Validation
            try {
                Questionnaire queriedQuestionnaire = questionnaireRepository.findQuestionnaireById(questionnaire.getId());
                if (queriedQuestionnaire == null) {
                    // If edited questionnaire doesn't exist, we save it with a new id
                    isDeleted = true;
                    questionnaire.setId(null);
                }
                // Validate state change
                else if (questionnaire.isStateChange()) {
                    var validateStateResult = validateQuestionnaireStateChange(questionnaire, queriedQuestionnaire);
                    if (validateStateResult != "OK") {
                        response.setResponseText(validateStateResult);
                        response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
                        // setSuccessful(true) -> így a frontend-en bezáródik az esetlegesen megnyitott kérdőív szerkesztő felület
                        response.setSuccessful(true);
                        return response;
                    }
                }
                // Ignore editing if state isn't OPEN
                else if (!questionnaire.isStateChange() && queriedQuestionnaire.getState() != Enums.State.OPEN) {
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

            for (ScaleQuestion sq : questionnaire.getScaleQuestions()) {
                sq.setQuestionnaire(questionnaire);
                if (isDeleted) sq.setId(null);
            }
            for (ChoiceQuestion cq : questionnaire.getChoiceQuestions()) {
                cq.setQuestionnaire(questionnaire);
                if (isDeleted) cq.setId(null);
                for (Choice c : cq.getChoices()) {
                    c.setChoiceQuestion(cq);
                    if (isDeleted) c.setId(null);
                }
            }
            for (TextualQuestion tq : questionnaire.getTextualQuestions()) {
                tq.setQuestionnaire(questionnaire);
                if (isDeleted) tq.setId(null);
            }
        }

        questionnaire.setLastModified(LocalDateTime.now());

        try {
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
            boolean invalidTile = false;
            List<Questionnaire> queriedQuestionnaires = questionnaireRepository.findQuestionnaireByTitle(questionnaire.getTitle());
            if (queriedQuestionnaires != null && !queriedQuestionnaires.isEmpty()) {
                if (isEditMode) {
                    for (Questionnaire q : queriedQuestionnaires) {
                        if (q.getId() != questionnaire.getId()) invalidTile = true;
                    }
                }
                else invalidTile = true;
            }
            if (invalidTile) {
                response.setResponseText("A kérdőív mentése sikertelen, mivel a megadott cím már szerepel az adatbázisban.");
                response.setSuccessful(false);
                return response;
            }

            response.setQuestionnaire(questionnaireRepository.save(questionnaire));
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
            List<Questionnaire> questionnaires = questionnaireRepository.findPublishedQuestionnaires(currentUser.getRegistrationNum());
            questionnaires = closeQuestionnaires(questionnaires);

            if (currentUser.getRole().equals(Enums.Role.USER)) {
                questionnaires = questionnaires
                    .stream()
                    .filter(qu -> qu.getState() == Enums.State.PUBLISHED || qu.getState() == Enums.State.RESULT_PUBLISHED)
                    .map(qu -> new Questionnaire(qu.getId(), qu.getTitle(), qu.isUnionMembersOnly(),
                        qu.getChoiceQuestions().stream().map(cq -> {
                            if (cq.isUnionMembersOnly())
                                return new ChoiceQuestion(null, cq.getNumber(), null, false, true,
                                    onlyForUnionMemebers, null, 0, null);
                            else return cq;
                        }).collect(Collectors.toList()),
                        qu.getScaleQuestions().stream().map(sq -> {
                            if (sq.isUnionMembersOnly())
                                return new ScaleQuestion(null, sq.getNumber(), null, true, onlyForUnionMemebers,
                                    null, null, null, 0, null);
                            else return sq;
                        }).collect(Collectors.toList()),
                        qu.getTextualQuestions().stream().map(tq -> {
                            if (tq.isUnionMembersOnly())
                                  return new TextualQuestion(null, tq.getNumber(), null, true, true,
                                      onlyForUnionMemebers, null, null);
                            else return tq;
                        }).collect(Collectors.toList()),

                        qu.isPublished(), qu.getState(), qu.getCreatedAt(), qu.getFormattedCreatedAt(), qu.getLastModified(),
                        qu.getFormattedLastModified(), qu.getDeadline(), qu.getFormattedDeadline(), qu.isStateChange(),
                        qu.getCompletion(), qu.getCompletionRate(), qu.getRelatedUsers(), qu.getCompletionMako(), qu.getCompletionMakoBorrowed(), qu.getCompletionVac(),
                        qu.getCompletionVacBorrowed()))
                    .collect(Collectors.toList());
            }
            // If we closed a questionnaire, ignore the 'Closed' state!
            else if (currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {
                questionnaires = questionnaires
                        .stream()
                        .filter(qu -> qu.getState() == Enums.State.PUBLISHED || qu.getState() == Enums.State.RESULT_PUBLISHED)
                        .collect(Collectors.toList());
            }

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

    public QuestionnaireDTO deleteQuestionnaire(QuestionnaireDTO questionnaireDTO) {
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
            response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if ((e.getClass().equals(EmptyResultDataAccessException.class) ||
                e.getClass().equals(MissingResourceException.class)) &&
                e.getMessage().contains(questionnaireDTO.getQuestionnaire().getId().toString())) {

                String questionnaireNotExists = "Az adatbázisban már nem található a törölni kívánt kérdőív.";
                response.setResponseText(questionnaireNotExists);

                try {
                    response.setQuestionnaireList(questionnaireRepository.findAllOrderByLastModDesc());
                    response.setSuccessful(true);
                }
                catch (Exception exception) {
                    if (exception.getMessage() != null)
                        response.setResponseText("A kérdőív lista frissítése sikertelen a következő hiba miatt: " + exception.getMessage());
                    else response.setResponseText("A kérdőív lista frissítése sikertelen a következő hiba miatt: " + exception);

                    response.setSuccessful(false);
                }
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
        if (questionnaireDTO.getAnswers() != null && !questionnaireDTO.getAnswers().isEmpty()) {
            try {
                Questionnaire questionnaire = questionnaireRepository.findQuestionnaireById(
                    questionnaireDTO.getQuestionnaire().getId());
                if (questionnaire == null) {
                    response.setResponseText("Az Ön által kitöltött kérdőív már nem található meg az" +
                        " adatbázisban, így a válaszok mentése sikertelen.");
                    response.setSuccessful(true);
                    return response;
                }
                else if (!questionnaire.getState().equals(Enums.State.PUBLISHED)) {
                    response.setResponseText("Az Ön által kitöltött kérdőív már lezárásra került, így a válaszok " +
                            "mentése sikertelen.");
                    response.setSuccessful(true);
                    return response;
                }
                // Csekkoljuk, hogy már kitöltötte-e
                List<RegistrationNumberQuestionnaire> regnumbQuests = registrationNumberQuestionnaireRepository
                        .findByRegNumberAndQuestionnaireId(currentUser.getRegistrationNum(), questionnaireDTO.getQuestionnaire().getId());
                if (regnumbQuests != null && !regnumbQuests.isEmpty()) {
                    response.setResponseText("Ezt a kérdőívet Ön már kitöltötte, így a válaszai nem kerülnek ismét mentésre.");
                    response.setSuccessful(true);
                    return response;
                }
            }
            catch (Exception e) {
                if (e.getMessage() != null)
                    response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e.getMessage());
                else response.setResponseText("A válaszok mentése sikertelen a következő hiba miatt: " + e);

                response.setSuccessful(false);
                return response;
            }
        }
        else {
            response.setResponseText("A válaszok mentése sikertelen, ugyanis a beküldött objektum nem tartalmazza " +
                    "válaszait.");
            response.setSuccessful(false);
            return response;
        }

        try {
            List<Answer> savedAnswers = new ArrayList<>();
            boolean roleChanged = false;
            int answerCounter = 0;
            for (Answer answer : questionnaireDTO.getAnswers()) {
                if (!answer.isUnionMembersOnly() || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)) {
                    savedAnswers.add(answer);
                    answerCounter++;
                }
                else roleChanged = true;
            }
            answerRepository.saveAll(savedAnswers);

            RegistrationNumberQuestionnaire regnumbQuest = new RegistrationNumberQuestionnaire();
            regnumbQuest.setQuestionnaireId(questionnaireDTO.getQuestionnaire().getId());
            regnumbQuest.setRegistrationNum(currentUser.getRegistrationNum());

            if (roleChanged && answerCounter > 0) {
                registrationNumberQuestionnaireRepository.save(regnumbQuest);
                response.setResponseText("A válaszok mentése sikeresen megtörtént! Mivel az Ön szakszervezeti " +
                   "tagsági státusza időközben megszűnt, a szakszervezeti tagok részére szóló kérdések kapcsán adott válaszai " +
                   "nem kerültek mentésre.");
            }
            else if (roleChanged && answerCounter == 0) {
                response.setResponseText("Mivel az Ön szakszervezeti tagsági státusza időközben megszűnt, és a jelen " +
                    "kérdőív kizárólag szakszervezeti tagok részére szól, válaszai nem kerültek mentésre.");
            }
            else {
                registrationNumberQuestionnaireRepository.save(regnumbQuest);
                response.setResponseText("A válaszok mentése sikeresen megtörtént!");
            }
            response.setSuccessful(true);
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
            for (ScaleQuestion sq : response.getQuestionnaire().getScaleQuestions()) {

                if (!sq.isUnionMembersOnly() || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)
                        || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                    List<Answer> answers = answerRepository.findByScaleQuestionId(sq.getId());

                    String averageRate = decimalFormat.format(answers.stream().map(
                            a -> Double.parseDouble(a.getContent())).reduce(0.0, Double::sum) / answers.size());
                    if (averageRate.equals("NaN")) averageRate = "0";
                    sq.setAverageRate(averageRate);
                    sq.setCompletion(answers.size());
                }
                else {
                    sq.setType(null);
                    sq.setQuestion(onlyForUnionMemebers);
                }
            }
            for (ChoiceQuestion cq : response.getQuestionnaire().getChoiceQuestions()) {

                if (!cq.isUnionMembersOnly() || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_USER)
                        || currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
                    List<Answer> answers = answerRepository.findByChoiceQuestionId(cq.getId());
                    List<ChoiceAnswer> choiceAnswers = answers.stream().flatMap(a -> a.getChoiceAnswers().stream()).collect(Collectors.toList());

                    for (Choice c : cq.getChoices()) {
                        Integer numberOfSelection = (int) choiceAnswers.stream().filter(ca -> ca.getContent().equals(c.getMark())).count();
                        String percentOfSelection = decimalFormat.format((double) numberOfSelection / answers.size() * 100) + " %";
                        if (percentOfSelection.equals("NaN %")) percentOfSelection = "0 %";
                        c.setNumberOfSelection(numberOfSelection);
                        c.setPercentOfSelection(percentOfSelection);
                    }
                    cq.setCompletion(answers.size());
                }
                else {
                    cq.setType(null);
                    cq.setQuestion(onlyForUnionMemebers);
                }
            }
            Integer filledQuestionnaires = registrationNumberQuestionnaireRepository.getNumberOfFills(questionnaire.getId());
            Integer relatedUsers;

            if (questionnaire.isUnionMembersOnly()) relatedUsers = unionMembershipNumRepository.getNumberOfUnionMemberUsers();
            else relatedUsers = regNumberRepository.getNumberOfUsers();

            response.getQuestionnaire().setCompletion(filledQuestionnaires);
            response.getQuestionnaire().setRelatedUsers(relatedUsers);
            response.getQuestionnaire().setCompletionRate(decimalFormat.format((double) filledQuestionnaires / relatedUsers * 100) + "%");
            response.setSuccessful(true);
        }
        catch (Exception e) {
            if (e.getMessage() != null)
                response.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e.getMessage());
            else response.setResponseText("A válaszok lekérdezése sikertelen a következő hiba miatt: " + e);

            response.setSuccessful(false);
        }
        return response;
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
