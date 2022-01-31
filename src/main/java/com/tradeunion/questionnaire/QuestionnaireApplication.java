package com.tradeunion.questionnaire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan({"Services", "Controllers"})
@EnableJpaRepositories("Repositories")
@EnableTransactionManagement
@EntityScan(basePackages="Models")
public class QuestionnaireApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuestionnaireApplication.class, args);
	}

}
