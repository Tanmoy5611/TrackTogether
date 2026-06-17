package TrackTogether;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
		"server.port=0",
		"spring.datasource.url=jdbc:h2:mem:tracktogether-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.sql.init.mode=never",
		"spring.jpa.defer-datasource-initialization=false",
		"spring.security.oauth2.client.registration.google.client-id=test-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
		"spring.security.oauth2.client.registration.google.scope=openid,profile,email",
		"delijn.api.api-key="
})
class TrackTogetherApplicationTests {

	@Test
	void contextLoads() {
	}

}