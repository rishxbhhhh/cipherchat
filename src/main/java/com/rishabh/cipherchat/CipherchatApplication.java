package com.rishabh.cipherchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class CipherchatApplication {

	private static final Logger logger = LoggerFactory.getLogger(CipherchatApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(CipherchatApplication.class, args);
		logger.info("Booting up CipherChat Completed.");
	}

}
