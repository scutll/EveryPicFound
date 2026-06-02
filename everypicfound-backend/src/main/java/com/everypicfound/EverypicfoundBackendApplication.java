package com.everypicfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.everypicfound.**.infrastructure.mapper")
@ConfigurationPropertiesScan
@EnableScheduling
public class EverypicfoundBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EverypicfoundBackendApplication.class, args);
	}

}
