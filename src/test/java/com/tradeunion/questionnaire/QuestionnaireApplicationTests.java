package com.tradeunion.questionnaire;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QuestionnaireApplicationTests {

	@Test
	void contextLoads() {

		String test1 = "asd fg as ert";
		String test2 = test1.replace("as", "EF");

		String test3 = test2;
	}

}
