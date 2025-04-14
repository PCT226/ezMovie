package ezcloud.ezMovie.manage.service;


import ezcloud.ezMovie.manage.model.dto.CinemaDto;
import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.repository.CinemaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public List<CinemaDto> getAll(Pageable pageable) {
        Page<Cinema> cinemas = cinemaRepository.findAllByIsDeleted(false, pageable);
        return cinemas.stream().map(cinema -> mapper.map(cinema, CinemaDto.class))
                .collect(Collectors.toList());
    }

    public Response<CinemaDto> getById(int id) {
        Cinema cinema = cinemaRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found Cinema with ID:" + id));
        return new Response<>(0, mapper.map(cinema, CinemaDto.class));
    }

    public Response<CinemaDto> createCinema(CinemaDto cinemaDto) {
        Cinema cinema = mapper.map(cinemaDto, Cinema.class);
        cinema.setCreatedAt(LocalDateTime.now());
        cinema.setUpdatedAt(LocalDateTime.now());
        cinema.setDeleted(false);
        cinemaRepository.save(cinema);
        return new Response<>(0, mapper.map(cinema, CinemaDto.class));

    }

    public Response<CinemaDto> updateCinema(CinemaDto cinemaDto) {
        Cinema existingCinema = cinemaRepository.findById(cinemaDto.getId())
                .orElseThrow(() -> new RuntimeException("Cinema not found with id: " + cinemaDto.getId()));
        mapper.map(cinemaDto, Cinema.class);
        existingCinema.setName(cinemaDto.getName());
        existingCinema.setCity(cinemaDto.getCity());
        existingCinema.setLocation(cinemaDto.getLocation());
        existingCinema.setUpdatedAt(LocalDateTime.now());
        Cinema updatedCinema = cinemaRepository.save(existingCinema);
        return new Response<>(0, mapper.map(updatedCinema, CinemaDto.class));

    }

    public void deleteCinema(int id) {
        Optional<Cinema> del = cinemaRepository.findById(id);
        if (del.isPresent()) {
            Cinema cinema = del.get();
            cinema.setDeleted(true);
            cinemaRepository.save(cinema);
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
