package ezcloud.ezMovie.service;

import ezcloud.ezMovie.model.dto.CinemaDto;
import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.model.dto.ScreenDto;
import ezcloud.ezMovie.model.dto.ShowtimeDto;
import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.model.enities.Movie;
import ezcloud.ezMovie.model.enities.Screen;
import ezcloud.ezMovie.model.enities.Showtime;
import ezcloud.ezMovie.model.payload.CreateShowtimeRequest;
import ezcloud.ezMovie.model.payload.UpdateShowtimeRq;
import ezcloud.ezMovie.repository.MovieRepository;
import ezcloud.ezMovie.repository.ScreenRepository;
import ezcloud.ezMovie.repository.ShowtimeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowtimeService {
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private MovieService movieService;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private ModelMapper mapper;

    public List<ShowtimeDto> getUpcomingShowtimes() {
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        // Tìm lịch chiếu trong ngày hiện tại nhưng giờ chiếu chưa bắt đầu
        List<Showtime> todayUpcoming = showtimeRepository.findByDateAndStartTimeAfterAndIsDeletedFalse(nowDate, nowTime);
        // Tìm lịch chiếu của ngày sau ngày hiện tại
        List<Showtime> futureShowtimes = showtimeRepository.findByDateAfterAndIsDeletedFalse(nowDate);
        // Kết hợp cả hai danh sách
        todayUpcoming.addAll(futureShowtimes);

        return todayUpcoming.stream().map(showtime -> {
            ShowtimeDto showtimeDto=mapper.map(showtime,ShowtimeDto.class);
            if (showtime.getMovie() != null) {
                MovieInfo movieInfo = mapper.map(showtime.getMovie(), MovieInfo.class);
                showtimeDto.setMovieInfo(movieInfo);
            } else {
                showtimeDto.setMovieInfo(null);
            }
            if (showtime.getScreen() != null) {
                ScreenDto screenDto = mapper.map(showtime.getScreen(), ScreenDto.class);
                if (showtime.getScreen().getCinema() != null) {
                    CinemaDto cinemaDTO = mapper.map(showtime.getScreen().getCinema(), CinemaDto.class);
                    screenDto.setCinemaDto(cinemaDTO);
                } else {
                    screenDto.setCinemaDto(null);
                }
                showtimeDto.setScreen(screenDto);
            } else {
                showtimeDto.setScreen(null);
            }
            return showtimeDto;

        }).collect(Collectors.toList());
    }

    public List<ShowtimeDto> getUpcomingShowtimesForMovie(Integer movieId) {
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        // Tìm lịch chiếu của bộ phim theo ngày hiện tại nhưng giờ chiếu chưa bắt đầu
        List<Showtime> todayUpcoming = showtimeRepository.findByMovieIdAndDateAndStartTimeAfterAndIsDeletedFalse(movieId, nowDate, nowTime);

        // Tìm lịch chiếu của bộ phim của các ngày sau ngày hiện tại
        List<Showtime> futureShowtimes = showtimeRepository.findByMovieIdAndDateAfterAndIsDeletedFalse(movieId, nowDate);

        // Kết hợp cả hai danh sách
        todayUpcoming.addAll(futureShowtimes);

        return todayUpcoming.stream().map(showtime -> {
            ShowtimeDto showtimeDto=mapper.map(showtime,ShowtimeDto.class);
            if (showtime.getMovie() != null) {
                MovieInfo movieInfo = mapper.map(showtime.getMovie(), MovieInfo.class);
                showtimeDto.setMovieInfo(movieInfo);
            } else {
                showtimeDto.setMovieInfo(null);
            }
            if (showtime.getScreen() != null) {
                ScreenDto screenDto = mapper.map(showtime.getScreen(), ScreenDto.class);
                if (showtime.getScreen().getCinema() != null) {
                    CinemaDto cinemaDTO = mapper.map(showtime.getScreen().getCinema(), CinemaDto.class);
                    screenDto.setCinemaDto(cinemaDTO);
                } else {
                    screenDto.setCinemaDto(null);
                }
                showtimeDto.setScreen(screenDto);
            } else {
                showtimeDto.setScreen(null);
            }
            return showtimeDto;

        }).collect(Collectors.toList());
    }


    public ShowtimeDto createShowtime(CreateShowtimeRequest request) {
        Movie movie=movieRepository.findById(request.getMovieId())
                .orElseThrow(()-> new RuntimeException("Movie not found"));
        mapper.map(movie,MovieInfo.class);
        Screen screen= screenRepository.findById(request.getScreenId())
                .orElseThrow(()-> new RuntimeException("Screen not found"));
//        Showtime showtime= new Showtime(0,movie,screen,request.getDate(),request.getStartTime(),request.getEndTime(), LocalDateTime.now(),LocalDateTime.now(),false);
        Showtime showtime =new Showtime();
        showtime.setDate(request.getDate());
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(request.getStartTime().toLocalTime());
        showtime.setEndTime(request.getEndTime().toLocalTime());
        showtime.setCreatedAt(LocalDateTime.now());
        showtime.setUpdatedAt(LocalDateTime.now());

        showtimeRepository.save(showtime);
        return mapper.map(showtime,ShowtimeDto.class);
    }
    public ShowtimeDto updateShowtime(UpdateShowtimeRq rq){
        Showtime showtime=showtimeRepository.findById(rq.getShowtimeId())
                .orElseThrow(()->new RuntimeException("Not found Showtime"));
        if (rq.getMovieId() != null && !rq.getMovieId().equals(showtime.getMovie().getId())) {
            Movie movie = movieRepository.findById(rq.getMovieId())
                    .orElseThrow(() -> new RuntimeException("Movie not found"));
            showtime.setMovie(movie);
        }
        if (rq.getScreenId() != null && !rq.getScreenId().equals(showtime.getScreen().getId())) {
            Screen screen = screenRepository.findById(rq.getScreenId())
                    .orElseThrow(() -> new RuntimeException("Screen not found"));
            showtime.setScreen(screen);
        }
        showtime.setDate(rq.getDate());
        showtime.setStartTime(rq.getStartTime());
        showtime.setEndTime(rq.getEndTime());
        showtime.setUpdatedAt(LocalDateTime.now());
        Showtime updatedShowtime=showtimeRepository.save(showtime);
        return mapper.map(updatedShowtime,ShowtimeDto.class);
    }

    public void deleteShowtime(int id){
        Showtime showtime=showtimeRepository.findById(id).orElseThrow(()->new RuntimeException("Not found Showtime to Delete"));
        showtime.setDeleted(true);
        showtimeRepository.save(showtime);
    }
}
