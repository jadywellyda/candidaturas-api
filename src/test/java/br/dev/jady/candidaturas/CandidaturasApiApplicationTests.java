package br.dev.jady.candidaturas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:candidaturas-context-test;DB_CLOSE_DELAY=-1")
class CandidaturasApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
