package Services;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@org.springframework.context.annotation.Configuration
public class DbSessionConfig {

    public static Session dbSession;

    @Bean
    public void getDbSession() {
        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.username", System.getenv("DB_USERNAME"));
        config.setProperty("hibernate.connection.password", System.getenv("DB_PASSWORD"));
        config.configure();
        // local SessionFactory bean created
        SessionFactory sessionFactory = config.buildSessionFactory();

        dbSession = sessionFactory.openSession();
    }
}
