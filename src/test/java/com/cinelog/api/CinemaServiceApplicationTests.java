package com.cinelog.api; // Fica na pasta src/test/java, com a mesma estrutura de pacotes da classe principal

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @SpringBootTest: Diz ao Spring Boot para carregar TODO o contexto da aplicação
 * (banco de dados, beans, controllers, services) antes de rodar os testes que estão aqui dentro.
 * É usado para criar Testes de Integração.
 */
@SpringBootTest
class CinemaServiceApplicationTests {

    /**
     * @Test: Uma anotação do framework JUnit. Diz que este método é um caso de teste que deve ser executado.
     *
     * void contextLoads():
     * PERGUNTA DO PROFESSOR: "Por que esse método está vazio? Ele não faz nada?"
     * SUA RESPOSTA: "Ele faz sim! O simples fato de ele estar vazio e não ter código significa que o teste só vai passar
     * se o contexto do Spring (ApplicationContext) conseguir subir inteiro sem dar nenhum erro (Crash).
     * É um teste de sanidade. Se eu errar uma senha de banco de dados ou criar uma dependência circular no meu código,
     * o contexto não vai carregar e esse teste vai falhar automaticamente, me avisando que o projeto está quebrado."
     */
    @Test
    void contextLoads() {
        // O corpo fica vazio intencionalmente.
        // Se chegar até aqui sem lançar exceções durante o carregamento, o teste passou.
    }

}
