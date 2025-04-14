package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.exception.MovieNotFound;
import ezcloud.ezMovie.manage.model.dto.MovieInfo;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.specification.MovieSpecification;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieService {
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = "movies", key = "'allMovie'")
    public List<MovieInfo> findAll(Pageable pageable) {
        Page<Movie> movies = movieRepository.findAllByIsDeletedFalse(pageable);
        return movies.stream().map(movie -> mapper.map(movie, MovieInfo.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "movies")
    public Response<MovieInfo> findById(int id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFound("Not found Movie with ID: " + id));
        return new Response<>(0, mapper.map(movie, MovieInfo.class));
    }

    public List<MovieInfo> searchMovies(String title, String genre, String actor, Pageable pageable) {
        Specification<Movie> spec = MovieSpecification.searchMovies(title, genre, actor)
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("isDeleted"), false));

        Page<Movie> movies = movieRepository.findAll(spec, pageable);

        return movies.stream()
                .map(movie -> mapper.map(movie, MovieInfo.class))
                .collect(Collectors.toList());
    }

    public Response<MovieInfo> createMovie(MovieInfo movieInfo) {
        Movie movie = new Movie();
        mapper.map(movieInfo, movie);
        movie.setCreatedAt(LocalDateTime.now());
        movie.setUpdatedAt(LocalDateTime.now());
        Movie savedMovie = movieRepository.save(movie);
        cacheManager.getCache("movies").clear();

        return new Response<>(0, mapper.map(savedMovie, MovieInfo.class));
    }

    public Response<MovieInfo> updateMovie(MovieInfo movieInfo) {
        Movie movie = movieRepository.findById(movieInfo.getId()).orElseThrow(() -> new MovieNotFound("Not found Movie with ID: " + movieInfo.getId()));
        mapper.map(movieInfo, movie);
        movie.setUpdatedAt(LocalDateTime.now());
        Movie updatedMovie = movieRepository.save(movie);
        return new Response<>(0, mapper.map(updatedMovie, MovieInfo.class));

    }

    public void deleteMovie(int id) {
        Optional<Movie> del = movieRepository.findById(id);
        if (del.isPresent()) {
            Movie movie = del.get();
            movie.setDeleted(true);
            movieRepository.save(movie);
        }
        throw new MovieNotFound("Movie not found");
    }
}
