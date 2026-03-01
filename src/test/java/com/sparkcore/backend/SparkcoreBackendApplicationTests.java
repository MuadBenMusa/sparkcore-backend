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
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.GenericContainer;

// Beweist: Die Anwendung startet korrekt mit echten Postgres-, Redis- & Kafka-Datenbanken/Brokern.
// Hibernate übernimmt das Schema-Management (create-drop) im Test-Kontext.
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class SparkcoreBackendApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@Container
	static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// Datasource → Testcontainer
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		// Flyway explizit → gleicher Testcontainer
		registry.add("spring.flyway.url", postgres::getJdbcUrl);
		registry.add("spring.flyway.user", postgres::getUsername);
		registry.add("spring.flyway.password", postgres::getPassword);

		// Redis → Testcontainer
		registry.add("spring.redis.host", redis::getHost);
		registry.add("spring.redis.port", redis::getFirstMappedPort);

		// Kafka → Testcontainer
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@Test
	void contextLoads() {
		System.out.println("✅ Spring-Kontext läuft mit Postgres, Redis & Kafka!");
		System.out.println("Postgres Port: " + postgres.getFirstMappedPort());
		System.out.println("Redis Port: " + redis.getFirstMappedPort());
		System.out.println("Kafka Bootstrap: " + kafka.getBootstrapServers());
	}
}
