package com.cinelog.api.Service; // Define o pacote da camada intermediária que abriga as regras de negócio

import com.cinelog.api.DTO.GenreRequest;
import com.cinelog.api.DTO.GenreResponse;
import com.cinelog.api.Entity.Genre;
import com.cinelog.api.Exception.ConflictException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import com.cinelog.api.Repository.GenreRepository;
import com.cinelog.api.Repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * GenreService: contém as regras de negócio dos gêneros.
 * O Controller recebe a requisição e chama este Service, que valida as regras e usa os Repositories.
 */
@Service
public class GenreService {

    private final GenreRepository genreRepository;

    // Precisamos do MovieRepository para saber se algum filme ainda usa o gênero antes de apagá-lo
    private final MovieRepository movieRepository;

    // Injeção de dependência via Construtor: o Spring entrega os dois repositories automaticamente
    public GenreService(GenreRepository genreRepository, MovieRepository movieRepository) {
        this.genreRepository = genreRepository;
        this.movieRepository = movieRepository;
    }

    // CREATE: Cadastra um novo gênero
    public GenreResponse createGenre(GenreRequest genreRequest) {

        // Regra de negócio: não pode haver dois gêneros com o mesmo nome (ignorando maiúsculas/minúsculas).
        // Erro 409 (Conflict): o pedido está correto, mas conflita com algo que já existe.
        if (genreRepository.existsByNomeIgnoreCase(genreRequest.nome())) {
            throw new ConflictException("Já existe um gênero cadastrado com o nome: " + genreRequest.nome());
        }

        Genre genre = new Genre(genreRequest.nome());

        // O JPA gera o INSERT e devolve a entidade com o ID gerado pelo banco
        Genre savedGenre = genreRepository.save(genre);

        return toResponse(savedGenre);
    }

    // READ: Busca um gênero pelo ID
    public GenreResponse findById(long id) {
        // Se não existir, lança o erro 404 (tratado pelo GlobalExceptionHandler)
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gênero não encontrado com o ID: " + id));

        return toResponse(genre);
    }

    // READ: Lista os gêneros de forma paginada
    public Page<GenreResponse> findAll(Pageable pageable) {
        // findAll(pageable) busca no banco só a página pedida (ex.: 10 registros da página 0)
        Page<Genre> genres = genreRepository.findAll(pageable);

        // map: transforma cada Genre da página em GenreResponse, mantendo as informações da paginação
        // (total de elementos, número da página, etc.)
        return genres.map(genre -> toResponse(genre));
    }

    // READ (consulta personalizada): busca gêneros cujo nome contém o texto informado, de forma paginada
    public Page<GenreResponse> findByName(String nome, Pageable pageable) {
        Page<Genre> genres = genreRepository.findByNomeContainingIgnoreCase(nome, pageable);

        return genres.map(genre -> toResponse(genre));
    }

    // UPDATE: Atualiza o nome de um gênero existente
    public GenreResponse updateGenre(long id, GenreRequest genreRequest) {
        // Primeiro verifica se o gênero existe. Se não existir, lança erro 404 e para aqui.
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gênero não encontrado com o ID: " + id));

        // Só confere duplicidade se o nome realmente mudou. Sem isso, mandar o mesmo nome
        // que o gênero já tem daria conflito com ele mesmo.
        boolean nameChanged = !genre.getNome().equalsIgnoreCase(genreRequest.nome());

        if (nameChanged && genreRepository.existsByNomeIgnoreCase(genreRequest.nome())) {
            throw new ConflictException("Já existe um gênero cadastrado com o nome: " + genreRequest.nome());
        }

        genre.setNome(genreRequest.nome());

        // save() com uma entidade que JÁ TEM ID executa um UPDATE em vez de um INSERT
        Genre savedGenre = genreRepository.save(genre);

        return toResponse(savedGenre);
    }

    // DELETE: Apaga um gênero
    public void deleteGenre(long id) {
        // Se o ID não existir, erro 404
        // ✅ VERIFICAÇÃO 1 — o gênero existe?
        if (!genreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Gênero não encontrado com o ID: " + id);
        }

        // Regra de negócio: não se apaga um gênero que ainda está ligado a algum filme.
        // Sem essa checagem, o banco recusaria o DELETE (chave estrangeira) e o cliente receberia erro 500.
        if (movieRepository.existsByGenresId(id)) {
            throw new ConflictException("Não é possível excluir o gênero com ID " + id + ", pois ele está associado a filmes");
        }

        // 3. DELETE
        genreRepository.deleteById(id);
    }

    // Converte a entidade Genre no DTO de saída. Fica em um método só para não repetir o código em cada operação.
    private GenreResponse toResponse(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getNome());
    }
}
