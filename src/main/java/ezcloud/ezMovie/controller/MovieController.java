package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "movie")
public class MovieController {
    @Autowired
    private MovieService movieService;
    @GetMapping(value = "/")
    public ResponseEntity<List<MovieInfo>> getListMovie(){
        return ResponseEntity.ok(movieService.findAll());
    }

}
