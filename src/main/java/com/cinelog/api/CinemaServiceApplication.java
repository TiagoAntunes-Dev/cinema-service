package com.cinelog.api; // Define o pacote raiz da sua aplicação

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// http://localhost:8080/swagger-ui/index.html#/

// http://localhost:8080/h2-console

/**
 * @SpringBootApplication: Esta é a anotação mais importante do seu projeto.
 * Na verdade, ela é um "combo" que embute três outras anotações poderosas por debaixo dos panos:
 * 1. @Configuration: Permite registrar beans (objetos) extras no contexto do Spring.
 * 2. @EnableAutoConfiguration: A "mágica" do Spring Boot. Ele olha para as dependências que você instalou
 *    (como Web, JPA, H2) e configura tudo automaticamente (como o servidor web e a conexão com o banco).
 * 3. @ComponentScan: Diz ao Spring para varrer todos os pacotes dentro de "com.cinelog.api"
 *    (Entity, Controllers, Service, etc.) procurando anotações como @RestController e @Service para instanciá-las.
 */
@SpringBootApplication
public class CinemaServiceApplication {

    /**
     * public static void main(String[] args):
     * Este é o método padrão de entrada de qualquer programa Java.
     * É aqui que a execução do código realmente começa quando você aperta o "Play" na sua IDE.
     */
    public static void main(String[] args) {
        /**
         * SpringApplication.run():
         * Esta linha é responsável por inicializar todo o ecossistema do Spring Boot.
         * O que ela faz nos bastidores:
         * - Sobe um servidor web embutido (normalmente o Apache Tomcat) na porta 8080.
         * - Cria o ApplicationContext (o contêiner de injeção de dependências do Spring).
         * - Lê as configurações do seu application.properties.
         * - Executa o seu arquivo data.sql no banco de dados H2 em memória.
         */
        SpringApplication.run(CinemaServiceApplication.class, args);
    }

}
