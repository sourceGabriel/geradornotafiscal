package br.com.itau.geradornotafiscal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeradorNotaFiscalApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeradorNotaFiscalApplication.class, args);
	}

}
