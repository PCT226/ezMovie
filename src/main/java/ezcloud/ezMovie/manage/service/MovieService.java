package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.exception.MovieNotFound;
import ezcloud.ezMovie.manage.model.dto.MovieInfo;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.payload.CreateMovieRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateMovieRequest;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.specification.MovieSpecification;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<MovieInfo> findAll(){
        List<Movie> movies=movieRepository.findAllByIsDeleted(false);
        return movies.stream().map(movie -> mapper.map(movie, MovieInfo.class))
                .collect(Collectors.toList());
    }
    public MovieInfo findById(int id){
        Movie movie= movieRepository.findById(id)
                .orElseThrow(()-> new MovieNotFound("Not found Movie with ID: "+id));
        return mapper.map(movie, MovieInfo.class);
    }

    public List<MovieInfo> searchMovies(String title, String genre, String actor) {
        Specification<Movie> spec = MovieSpecification.searchMovies(title, genre, actor)
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("isDeleted"), false)); // Thêm điều kiện isDeleted = false
        List<Movie> movies= movieRepository.findAll(spec);

        return movies.stream().map(movie -> mapper.map(movie,MovieInfo.class))
                .collect(Collectors.toList());
    }
    public MovieInfo createMovie(CreateMovieRequest movieInfo){
        Movie movie=new Movie();
        mapper.map(movieInfo,movie);
        movie.setCreatedAt(LocalDateTime.now());
        movie.setUpdatedAt(LocalDateTime.now());
        Movie savedMovie= movieRepository.save(movie);
        return mapper.map(savedMovie, MovieInfo.class);
    }
    public MovieInfo updateMovie(UpdateMovieRequest movieInfo){
        Movie movie=movieRepository.findById(movieInfo.getId()).orElseThrow(()-> new MovieNotFound("Not found Movie with ID: "+movieInfo.getId()));
        mapper.map(movieInfo,movie);
        movie.setUpdatedAt(LocalDateTime.now());
        Movie updatedMovie= movieRepository.save(movie);
        return mapper.map(updatedMovie,MovieInfo.class);

    }
    public void deleteMovie(int id){
        Optional<Movie> del = movieRepository.findById(id);
        if (del.isPresent()){
            Movie movie=del.get();
            movie.setDeleted(true);
            movieRepository.save(movie);
        }
        throw new MovieNotFound("Movie not found");
    }
}
