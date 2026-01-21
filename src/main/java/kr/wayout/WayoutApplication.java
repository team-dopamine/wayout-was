package kr.wayout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class WayoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(WayoutApplication.class, args);
	}

}
