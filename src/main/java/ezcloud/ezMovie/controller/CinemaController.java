package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.model.dto.CinemaDto;
import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "cinema")
public class CinemaController {
    @Autowired
    private CinemaService cinemaService;
    @GetMapping("/")
    public ResponseEntity<List<CinemaDto>> getAll(){
       return ResponseEntity.ok(cinemaService.getAll());
    }
    @PostMapping("/")
    public ResponseEntity<Cinema> create(@RequestBody CinemaDto cinemaDto){
        Cinema c1= cinemaService.createCinema(cinemaDto);
        return ResponseEntity.ok( c1);
    }
    @PutMapping("/")
    public ResponseEntity<Cinema> update(@RequestBody CinemaDto cinema){
        return ResponseEntity.ok(cinemaService.updateCinema(cinema));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCinema(@PathVariable int id){
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok("Xóa thành công");

    }

}
