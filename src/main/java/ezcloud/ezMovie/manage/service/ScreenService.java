package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.dto.ScreenDto1;
import ezcloud.ezMovie.manage.model.dto.CinemaDto;
import ezcloud.ezMovie.manage.model.dto.ScreenDto;
import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Screen;
import ezcloud.ezMovie.manage.model.payload.CreateScreenRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateScreenRequest;
import ezcloud.ezMovie.manage.repository.CinemaRepository;
import ezcloud.ezMovie.manage.repository.ScreenRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ScreenService {
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private CinemaRepository cinemaRepository;
    @Autowired
    private ModelMapper mapper;

    public Page<ScreenDto> getAll(Pageable pageable) {

        Page<Screen> screens = screenRepository.findAllByIsDeleted(false, pageable);

        return screens.map(screen -> {
            ScreenDto screenDTO = mapper.map(screen, ScreenDto.class);

            if (screen.getCinema() != null) {
                CinemaDto cinemaDTO = mapper.map(screen.getCinema(), CinemaDto.class);
                screenDTO.setCinemaDto(cinemaDTO);
            } else {
                screenDTO.setCinemaDto(null);
            }

            return screenDTO;
        });
    }


    public Screen findById(int id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not find Screen with ID:" + id));
    }

    public Screen createScreen(CreateScreenRequest request) {
        Cinema cinema = cinemaRepository.findById(request.getCinemaId())
                .orElseThrow(() -> new RuntimeException("Not found Cinema"));
        Screen screen = new Screen();
        screen.setCinema(cinema);
        screen.setScreenNumber(request.getScreenNumber());
        screen.setCapacity(request.getCapacity());
        screen.setCreatedAt(LocalDateTime.now());
        screen.setUpdatedAt(LocalDateTime.now());
        screen.setDeleted(false);
        return screenRepository.save(screen);
    }

    public ScreenDto1 updateScreen(UpdateScreenRequest request) {
        Screen existScreen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        if (request.getCinemaId() != null && !request.getCinemaId().equals(existScreen.getCinema().getId())) {
            Cinema newCinema = cinemaRepository.findById(request.getCinemaId())
                    .orElseThrow(() -> new RuntimeException("Cinema not found"));
            existScreen.setCinema(newCinema);
        }

        existScreen.setScreenNumber(request.getScreenNumber());
        existScreen.setCapacity(request.getCapacity());
        existScreen.setUpdatedAt(LocalDateTime.now());

        Screen saved = screenRepository.save(existScreen);

        // Mapping thủ công
        ScreenDto1 dto = new ScreenDto1();
        dto.setId(saved.getId());
        dto.setScreenNumber(saved.getScreenNumber());
        dto.setCapacity(saved.getCapacity());
        dto.setCinemaId(saved.getCinema().getId());

        return dto;
    }


    public void deleteScreen(int id) {
        Screen screen = screenRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found Screen to Delete"));
        screen.setDeleted(true);
        screenRepository.save(screen);
    }
}

