#  Spring Boot 4 — API REST CRUD

Guia prático para construir uma API REST CRUD de usuários com **Spring Boot 4, JPA, H2, DTOs, Validation e Swagger**.

## 📋 Conteúdos

* API REST e CRUD
* Controller, Service e Repository
* DTOs e Records
* JPA e `JpaRepository`
* Dependency Injection
* `ResponseEntity` e HTTP Status
* Tratamento de exceções
* Validação
* H2 Database
* OpenAPI / Swagger

---

## 🏗️ Arquitetura

```text
Cliente
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

Cada camada possui uma responsabilidade:

| Camada     | Responsabilidade                 |
| ---------- | -------------------------------- |
| Controller | Recebe requisições HTTP          |
| Service    | Contém regras de negócio         |
| Repository | Acessa o banco                   |
| Entity     | Representa os dados persistidos  |
| DTO        | Define os dados de entrada/saída |

---

## 🔄 CRUD

| Operação  | HTTP     | Endpoint             |
| --------- | -------- | -------------------- |
| Criar     | `POST`   | `/api/v1/users`      |
| Listar    | `GET`    | `/api/v1/users`      |
| Buscar    | `GET`    | `/api/v1/users/{id}` |
| Atualizar | `PUT`    | `/api/v1/users/{id}` |
| Excluir   | `DELETE` | `/api/v1/users/{id}` |

---

## 📦 DTOs

DTO significa **Data Transfer Object**.

Não é recomendado retornar a Entity diretamente. Use DTOs para controlar o contrato da API.

```java
public record UserRequest(
        String name,
        String email
) {}
```

```java
public record UserResponse(
        Long id,
        String name,
        String email
) {}
```

### Fluxo

```text
JSON
 ↓
UserRequest
 ↓
Service
 ↓
User Entity
 ↓
Repository
 ↓
Database
```

---

## 🗄️ Entity + JPA

A Entity representa uma tabela do banco.

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
}
```

### JpaRepository

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

O `JpaRepository` fornece métodos como:

```text
save()
findById()
findAll()
deleteById()
existsById()
```

---

## 💉 Dependency Injection

Prefira **Constructor Injection**:

```java
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

Isso deixa as dependências explícitas e facilita os testes.

---

## 📡 ResponseEntity

Use `ResponseEntity` para controlar status e headers.

Ao criar um recurso:

```java
return ResponseEntity
        .created(URI.create("/api/v1/users/" + id))
        .body(response);
```

Principais status:

| Status | Significado           |
| ------ | --------------------- |
| `200`  | OK                    |
| `201`  | Created               |
| `204`  | No Content            |
| `400`  | Bad Request           |
| `404`  | Not Found             |
| `500`  | Internal Server Error |

O `201 Created` é usado quando um novo recurso foi criado.

O header `Location` informa onde o recurso pode ser encontrado:

```text
Location: /api/v1/users/15
```

---

## ⚠️ Tratamento de exceções

Crie exceções específicas:

```java
public class ResourceNotFoundException
        extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

E centralize o tratamento:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Assim, erros como usuário inexistente podem retornar:

```json
{
  "status": 404,
  "message": "User not found"
}
```

---

## ✅ Validation

Use Jakarta Bean Validation nos DTOs:

```java
public record UserRequest(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email

) {}
```

No Controller:

```java
@PostMapping
public ResponseEntity<UserResponse> create(
        @Valid @RequestBody UserRequest request) {
    // ...
}
```

Principais annotations:

```text
@NotBlank → campo obrigatório
@Email    → formato de e-mail
@Valid    → ativa a validação
```

---

## 🗃️ H2 Database

O H2 é um banco leve, muito útil para estudos e testes.

Exemplo:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create
spring.h2.console.enabled=true
```

### `data.sql`

Pode ser usado para inserir dados iniciais:

```sql
INSERT INTO users (name, email)
VALUES ('Tiago', 'tiago@email.com');
```

Se o `data.sql` for executado antes da criação das tabelas, pode ocorrer erro.

Uma configuração possível:

```properties
spring.jpa.defer-datasource-initialization=true
```

---

## 📖 OpenAPI / Swagger

OpenAPI documenta automaticamente a API e o Swagger UI permite testar os endpoints pelo navegador.

Exemplo de documentação:

```text
POST   /api/v1/users
GET    /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

---

## 🧠 Resumo

```text
Controller
   ↓
DTO
   ↓
Service
   ↓
Entity
   ↓
Repository
   ↓
Database
```

**Controller** → recebe HTTP
**DTO** → transporta dados
**Service** → regras de negócio
**Entity** → representa dados persistidos
**Repository** → acesso ao banco
**JPA** → mapeamento objeto-relacional
**Validation** → valida entradas
**RestControllerAdvice** → tratamento global de erros
**Swagger** → documentação da API

---

https://scryfall.com/docs/api

https://senacsp.blackboard.com/ultra/courses/_316557_1/cl/outline
## ✅ Checklist

* [ ] Entender REST e CRUD
* [ ] Criar Controllers
* [ ] Criar Services
* [ ] Criar Repositories
* [ ] Trabalhar com JPA
* [ ] Criar DTOs com Records
* [ ] Usar Constructor Injection
* [ ] Trabalhar com `ResponseEntity`
* [ ] Implementar Validation
* [ ] Criar tratamento global de exceções
* [ ] Configurar H2
* [ ] Utilizar `data.sql`
* [ ] Documentar com Swagger
