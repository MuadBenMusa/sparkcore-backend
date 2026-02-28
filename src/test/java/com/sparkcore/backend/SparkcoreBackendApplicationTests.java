package com.sparkcore.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Beweist: Die Anwendung startet korrekt mit einer echten Postgres-Datenbank.
// Hibernate übernimmt das Schema-Management (create-drop) im Test-Kontext.
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class SparkcoreBackendApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// Datasource → Testcontainer
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		// Flyway explizit → gleicher Testcontainer
		// (nötig, da application-local.yaml in CI nicht existiert und
		// spring.flyway.* sonst auf localhost:5432 zeigt)
		registry.add("spring.flyway.url", postgres::getJdbcUrl);
		registry.add("spring.flyway.user", postgres::getUsername);
		registry.add("spring.flyway.password", postgres::getPassword);
	}

	@Test
	void contextLoads() {
		System.out.println("✅ Spring-Kontext läuft mit echter Datenbank!");
		System.out.println("Postgres Port: " + postgres.getFirstMappedPort());
	}
}
