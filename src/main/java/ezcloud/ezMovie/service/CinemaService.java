package ezcloud.ezMovie.service;


import ezcloud.ezMovie.model.dto.CinemaDto;
import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.repository.CinemaRepository;
import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class CinemaService {
    @Autowired
    private CinemaRepository cinemaRepository;
    @Autowired
    private ModelMapper mapper;

    public List<CinemaDto> getAll(){
        List<Cinema> cinemas=cinemaRepository.findAllByIsDeleted(false);
        return cinemas.stream().map(cinema -> mapper.map(cinema,CinemaDto.class))
                .collect(Collectors.toList());
    }

    public Cinema createCinema(CinemaDto cinemaDto){
        Cinema cinema=mapper.map(cinemaDto,Cinema.class);
        cinema.setCreatedAt(LocalDateTime.now());
        cinema.setUpdatedAt(LocalDateTime.now());
        cinema.setDeleted(false);
        return cinemaRepository.save(cinema);

    }
    public Cinema updateCinema(CinemaDto cinemaDto){
        Cinema existingCinema = cinemaRepository.findById(cinemaDto.getId())
                .orElseThrow(() -> new RuntimeException("Cinema not found with id: " + cinemaDto.getId()));
        existingCinema.setName(cinemaDto.getName());
        existingCinema.setCity(cinemaDto.getCity());
        existingCinema.setLocation(cinemaDto.getLocation());
        existingCinema.setUpdatedAt(LocalDateTime.now());
        return cinemaRepository.save(existingCinema);

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
