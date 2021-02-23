package it.minetti.dbautoreconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class DbAutoReconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbAutoReconnectApplication.class, args);
	}

}
