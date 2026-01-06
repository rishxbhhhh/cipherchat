package com.rishabh.cipherchat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
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
		// seed one admin user and one normal user if not present
		if (jdbcClient.sql("SELECT COUNT(*) FROM c_users").query(Integer.class).stream().findFirst().orElse(0) == 0) {
			jdbcClient.sql("INSERT INTO c_users (email, password, role, date_created) VALUES (?, ?, ?, NOW())")
					.params("admin@example.com", passwordEncoder.encode("admin"), "ADMIN")
					.update();
			jdbcClient.sql("INSERT INTO c_users (email, password, role, date_created) VALUES (?, ?, ?, NOW())")
					.params("user@example.com", passwordEncoder.encode("user"), "USER")
					.update();
		}
		logger.info("CipherChat Application Started Successfully.");
	}
}
