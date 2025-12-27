package com.rishabh.cipherchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CipherchatApplication {

	private static final Logger logger = LoggerFactory.getLogger(CipherchatApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(CipherchatApplication.class, args);
		logger.info("Booting up CipherChat Completed.");
	}

}
