package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "cinema")
public class CinemaController {
    @Autowired
    private CinemaService cinemaService;
    @GetMapping("/")
    public List<Cinema> getAll(){
       return cinemaService.getAll();
    }
    @PostMapping("/")
    public Cinema create(@RequestBody Cinema cinema){
        return cinemaService.createCinema(cinema);
    }
    @DeleteMapping("/{id}")
    public void deleteCinema(@PathVariable int id){
        cinemaService.deleteCinema(id);
    }

}
