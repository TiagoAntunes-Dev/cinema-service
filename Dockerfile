# Estágio 1: Compilação do projeto com Maven
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia os ficheiros de configuração do Maven e o código fonte
COPY . .
RUN ./mvnw clean package -DskipTests

# Estágio 2: Execução da aplicação em ambiente leve
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia o jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]