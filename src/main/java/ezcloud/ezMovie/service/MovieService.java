package ezcloud.ezMovie.service;

import ezcloud.ezMovie.exception.MovieNotFound;
import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.model.enities.Movie;
import ezcloud.ezMovie.repository.MovieRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
        Movie movie= movieRepository.findById(id).orElseThrow(()-> new MovieNotFound("Not founf Movie with ID: "+id));
        return mapper.map(movie, MovieInfo.class);
    }
    public MovieInfo createMovie(MovieInfo movieInfo){
        Movie movie=new Movie();
        mapper.map(movieInfo,movie);
        movie.setCreatedAt(LocalDateTime.now());
        movie.setUpdatedAt(LocalDateTime.now());
        Movie savedMovie= movieRepository.save(movie);
        return mapper.map(savedMovie, MovieInfo.class);
    }
    public MovieInfo updateMovie(MovieInfo movieInfo){
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
