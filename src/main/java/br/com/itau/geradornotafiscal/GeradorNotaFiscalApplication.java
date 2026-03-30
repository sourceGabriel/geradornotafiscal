package br.com.itau.geradornotafiscal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/**
 * Ponto de entrada da aplicacao de processamento e emissao de nota fiscal.
 */
public class GeradorNotaFiscalApplication {

	/**
	 * Inicializa o contexto Spring Boot.
	 *
	 * @param args argumentos de linha de comando
	 */
	public static void main(String[] args) {
		SpringApplication.run(GeradorNotaFiscalApplication.class, args);
	}

}
