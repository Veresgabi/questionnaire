package Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import java.util.Properties;

@Component
public class EmailService implements IEmailService {

    @Autowired
    private JavaMailSender emailSender;

    private static final String host = "smtp.gmail.com";
    private static final int port = 587;

    public void sendSimpleMessage(String to, String subject, String text) throws Exception {

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
}
