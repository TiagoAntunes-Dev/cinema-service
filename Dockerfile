# Estágio 1: Compilação do projeto com Maven
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Limita o consumo de memória do Maven para caber nos 512MB do Render
ENV MAVEN_OPTS="-Xmx256m"

# Copia os ficheiros de configuração do Maven e o código fonte
COPY . .

# Dá permissão de execução ao Maven Wrapper
RUN chmod +x mvnw

# Compila o projeto
RUN ./mvnw clean package -DskipTests

# Estágio 2: Execução da aplicação em ambiente leve
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia o jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]