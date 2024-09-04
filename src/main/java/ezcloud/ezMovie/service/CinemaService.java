package ezcloud.ezMovie.service;

import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.repository.CinemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CinemaService {
    @Autowired
    private CinemaRepository cinemaRepository;

    public List<Cinema> getAll(){
        return cinemaRepository.findAllByIsDeleted(false);
    }

    public Cinema createCinema(Cinema cinema){
        Cinema c1=cinema;
        c1.setCreatedAt(LocalDateTime.now());
        c1.setUpdatedAt(LocalDateTime.now());
        return cinemaRepository.save(c1);
    }
    public void deleteCinema(int id){
        Optional<Cinema> del = cinemaRepository.findById(id);
        if(del.isPresent()){
            Cinema cinema = del.get();
            cinema.setDeleted(true);
            cinemaRepository.save(cinema);
        }else {
            throw new RuntimeException("User not found");
        }
    }
}
