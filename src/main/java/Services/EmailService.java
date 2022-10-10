package Services;

import DTOs.QuestionnaireDTO;
import DTOs.UserResponseDTO;
import Models.Questionnaire;
import Models.User;
import Repositories.QuestionnaireRepository;
import Repositories.RegNumberRepository;
import Repositories.UnionMembershipNumRepository;
import Repositories.UserRepository;
import Utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class EmailService implements IEmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    public ITokenService tokenService;
    @Autowired
    public IQuestionnaireService questionnaireService;
    @Autowired
    public QuestionnaireRepository questionnaireRepository;
    @Autowired
    public UserRepository userRepository;

    private static final String host = "smtp.gmail.com";
    private static final int port = 587;

    private final String errorMailSending = "Az értesítő e-mailek kiküldése során a következő hiba lépett fel: ";

    public void sendSimpleMessage(String to, String subject, String text) {

        /* MimeMessage message = emailSender.createMimeMessage();
        message.addRecipients(Message.RecipientType.TO, to);
        message.setSubject(subject);
        message.setContent(text, "text/html"); */

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@questionnaire.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    public void sendSimpleMessages(SimpleMailMessage[] messages) {
        emailSender.send(messages);
    }

    private SimpleMailMessage createSimpleMessage(String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@questionnaire.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(System.getenv("GMAIL_USERNAME"));
        mailSender.setPassword(System.getenv("GMAIL_PASSWORD"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", host);

        return mailSender;
    }

    public QuestionnaireDTO sendNewQuestionnaireEmail(QuestionnaireDTO questionnaireDTO) {

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
        if (!currentUser.getRole().equals(Enums.Role.UNION_MEMBER_ADMIN)) {
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
        String questionnaireTitle = questionnaire.getTitle();

        List<String[]> affectedUsers = new ArrayList<>();
        int failCounter = 0;

        long startTime = System.currentTimeMillis();
        try {
            questionnaire = questionnaireRepository.findQuestionnaireById(questionnaire.getId());

            response.setQuestionnaire(questionnaire);
            if (questionnaire == null) {
                response.setResponseText("A " + questionnaireTitle + " című kérdőív már nem található meg az" +
                        " adatbázisban, így hozzá kapcsolódó értesítő e-mail nem küldhető ki.");
                return response;
            }
            questionnaire = questionnaireService.closeQuestionnaire(questionnaire);

            if (!questionnaire.getState().equals(Enums.State.PUBLISHED)) {
                response.setResponseText("A " + questionnaireTitle + " című kérdőív már lezárásra került, " +
                        "így hozzá kapcsolódó értesítő e-mail nem küldhető ki.");
                return response;
            }

            if (!questionnaire.isUnionMembersOnly()) {
                affectedUsers = userRepository.findAllUsers();
            }
            else affectedUsers = userRepository.findUnionMembers();

            String subject = "Értesítés új kérdőív közzétételéről";
            String questionnaireDeadline = questionnaire.getFormattedDeadline();

            SimpleMailMessage[] messages = new SimpleMailMessage[affectedUsers.size()];

            int index = 0;
            for (String[] userFields : affectedUsers) {

                String messageText = "Kedves " + userFields[0] + "! " +
                        "\n\nTájékoztatjuk, hogy a gszsz.hu kérdőív kitöltő alkalmazásán új kérdőív került közzétételre " +
                        questionnaireTitle + " címen, melynek kitöltési határideje " + questionnaireDeadline + ". " +
                        "\nKérjük, a kérdőívet szíveskedjen kitölteni a megjelölt határidőig. " +
                        "\nhttp://gszsz.hu/kerdoiv/login.html " +
                        "\nAmennyiben a kérdőív kitöltése már megtörtént, jelen levelünket kérjük, tekintse tárgytalannak. " +
                        "\n\nÜdvözlettel: Makói Gumiipari Szakszervezet";

                SimpleMailMessage message = createSimpleMessage(userFields[1], subject, messageText);
                messages[index] = message;

                index++;
            }
            try {
                sendSimpleMessages(messages);
                response.setResponseText(getEmailSendResponseText(affectedUsers.size(), failCounter));
            }
            catch (MailSendException e) {
                String errorMessage = "";
                if (e.getMessage() != null) errorMessage = e.getMessage();
                else errorMessage = e.toString();

                if (errorMessage.contains("Failed messages") && errorMessage.contains("SendFailedException: Invalid Addresses")
                        && e.getFailedMessages() != null) {
                    failCounter = e.getFailedMessages().size();
                    response.setResponseText(getEmailSendResponseText(affectedUsers.size(), failCounter));
                }
                else throw e;
            }
        }
        catch (Exception e) {
            response.setResponseText(getEmailSendResponseText(affectedUsers.size(), failCounter));

            response.setSuccessful(false);
            String errorMessage = "";
            if (e.getMessage() != null) errorMessage = e.getMessage();
            else errorMessage = e.toString();

            if (errorMessage.contains("Mail server connection failed") && errorMessage.contains("MailConnectException: Couldn't connect to host"))
                response.setResponseText(response.getResponseText() +
                        "\n" + errorMailSending + "A server nem elérhető, kérjük, ellenőrizze internet kapcsolatát.");
            else if (errorMessage.contains("Authentication failed") && errorMessage.contains("AuthenticationFailedException")
                    && errorMessage.contains("Username and Password not accepted"))
                response.setResponseText(response.getResponseText() +
                        "\n" + errorMailSending + "Az e-mail server hitelesítése nem sikerült.");
            else response.setResponseText(response.getResponseText() +
                        "\n" + errorMailSending + errorMessage);

        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        double estimatedTimeSeconds = (double) estimatedTime / 1000;
        response.setResponseText(response.getResponseText() +
                "\n" + "A kiküldés időtartama: " + estimatedTimeSeconds + " mp. ");

        return response;
    }

    private String getEmailSendResponseText(int affectedUsers, int failCounter) {
        return "Érintett felhasználók száma: " + affectedUsers + "; " +
                "\nE-mail cím formátum miatti sikertelen kiküldések száma: " + failCounter + "; ";
    }
}
