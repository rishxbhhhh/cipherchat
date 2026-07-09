package com.rishabh.cipherchat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@EnableScheduling
public class CipherchatApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(CipherchatApplication.class);
	@Autowired
	private JdbcClient jdbcClient;
	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(CipherchatApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Seed admin user if none exists
		// Admin does not chat — no encryption keys needed
		if (jdbcClient.sql("SELECT COUNT(*) FROM c_users WHERE role = 'ADMIN'")
				.query(Integer.class).stream().findFirst().orElse(0) == 0) {
			jdbcClient.sql(
				"INSERT INTO c_users (email, password, role, date_created, enabled) VALUES (?, ?, ?, NOW(), ?)")
				.params("admin@cipherchat.io", passwordEncoder.encode("admin"), "ADMIN", true)
				.update();
			logger.info("Seeded admin user: admin@cipherchat.io / admin");
		}
		logger.info("CipherChat Application Started Successfully.");
	}
}
