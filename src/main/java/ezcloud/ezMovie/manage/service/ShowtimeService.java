package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.manage.model.dto.CinemaDto;
import ezcloud.ezMovie.manage.model.dto.MovieInfo;
import ezcloud.ezMovie.manage.model.dto.ScreenDto;
import ezcloud.ezMovie.manage.model.dto.ShowtimeDto;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Screen;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.model.payload.CreateShowtimeRequest;
import ezcloud.ezMovie.manage.model.payload.CreateBulkShowtimeRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateShowtimeRq;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.manage.repository.ScreenRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowtimeService {
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private ModelMapper mapper;

    private LocalDateTime getCurrentTimeInHCM() {
        ZoneId hcmZone = ZoneId.of("Asia/Ho_Chi_Minh");
        return LocalDateTime.now(hcmZone);
    }

    public List<ShowtimeDto> getUpcomingShowtimes(Pageable pageable) {
        LocalDateTime now = getCurrentTimeInHCM();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        System.out.println("Getting upcoming showtimes for date: " + nowDate + ", time: " + nowTime);

        List<Showtime> todayUpcoming = showtimeRepository.findByDateAndStartTimeAfterAndIsDeletedFalse(nowDate, nowTime);
        List<Showtime> futureShowtimes = showtimeRepository.findByDateAfterAndIsDeletedFalse(nowDate);

        System.out.println("Today upcoming showtimes: " + todayUpcoming.size());
        System.out.println("Future showtimes: " + futureShowtimes.size());

        List<Showtime> combinedList = new ArrayList<>();
        combinedList.addAll(todayUpcoming);
        combinedList.addAll(futureShowtimes);

        System.out.println("Total combined showtimes: " + combinedList.size());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedList.size());

        List<Showtime> pagedContent = combinedList.subList(start, end);
        Page<Showtime> allShowtime = new PageImpl<>(pagedContent, pageable, combinedList.size());

        List<ShowtimeDto> result = allShowtime.stream().map(showtime -> {
            ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);
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

        System.out.println("Returning " + result.size() + " showtimes to user");
        return result;
    }

    public List<ShowtimeDto> getUpcomingShowtimesByCinemaId(Pageable pageable) {
        LocalDateTime now = getCurrentTimeInHCM();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        List<Showtime> todayUpcoming = showtimeRepository.findByDateAndStartTimeAfterAndIsDeletedFalse(nowDate, nowTime);
        List<Showtime> futureShowtimes = showtimeRepository.findByDateAfterAndIsDeletedFalse(nowDate);

        List<Showtime> combinedList = new ArrayList<>();
        combinedList.addAll(todayUpcoming);
        combinedList.addAll(futureShowtimes);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedList.size());

        List<Showtime> pagedContent = combinedList.subList(start, end);
        Page<Showtime> allShowtime = new PageImpl<>(pagedContent, pageable, combinedList.size());

        return allShowtime.stream().map(showtime -> {
            ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);
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

    public List<ShowtimeDto> getUpcomingShowtimesForMovie(Integer movieId, Integer cinemaId, LocalDate date, Pageable pageable) {
        LocalDateTime now = getCurrentTimeInHCM();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // Lấy danh sách showtime hôm nay và tương lai
        List<Showtime> todayUpcoming = showtimeRepository.findByMovieIdAndDateAndStartTimeAfterAndIsDeletedFalse(movieId, nowDate, nowTime);
        List<Showtime> futureShowtimes = showtimeRepository.findByMovieIdAndDateAfterAndIsDeletedFalse(movieId, nowDate);

        // Kết hợp danh sách
        List<Showtime> combinedList = new ArrayList<>();
        combinedList.addAll(todayUpcoming);
        combinedList.addAll(futureShowtimes);

        // Lọc theo cinemaId
        if (cinemaId != null) {
            combinedList = combinedList.stream()
                    .filter(showtime -> showtime.getScreen().getCinema().getId().equals(cinemaId))
                    .collect(Collectors.toList());
        }

        // Lọc theo date
        if (date != null) {
            combinedList = combinedList.stream()
                    .filter(showtime -> showtime.getDate().equals(date))
                    .collect(Collectors.toList());
        }

        // Phân trang
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedList.size());
        List<Showtime> pagedContent = combinedList.subList(start, end);

        // Tạo Page từ danh sách đã phân trang
        Page<Showtime> allShowtime = new PageImpl<>(pagedContent, pageable, combinedList.size());

        // Chuyển đổi sang ShowtimeDto
        return allShowtime.getContent().stream().map(showtime -> {
            ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);
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

    public List<ShowtimeDto> getUpcomingAndOngoingShowtimes(Integer cinemaId) {
        LocalDateTime now = getCurrentTimeInHCM();
        LocalDate nowDate = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // Lấy danh sách showtime hôm nay (đang diễn ra hoặc chưa bắt đầu)
        List<Showtime> todayShowtimes = showtimeRepository.findByScreen_Cinema_IdAndDateAndStartTimeAfterAndIsDeletedFalse(cinemaId, nowDate, nowTime);

        // Lấy danh sách showtime trong tương lai
        List<Showtime> futureShowtimes = showtimeRepository.findByScreen_Cinema_IdAndDateAfterAndIsDeletedFalse(cinemaId, nowDate);

        // Kết hợp danh sách
        List<Showtime> combinedList = new ArrayList<>();
        combinedList.addAll(todayShowtimes);
        combinedList.addAll(futureShowtimes);

        // Chuyển đổi sang ShowtimeDto
        return combinedList.stream().map(showtime -> {
            ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);
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
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        List<Showtime> existingShowtimes = showtimeRepository.findAllByScreenIdAndDateAndIsDeletedFalse(request.getScreenId(), request.getDate());
        for (Showtime existingShowtime : existingShowtimes) {
            if (isTimeConflict(existingShowtime, request)) {
                throw new RuntimeException("Showtime conflict detected");
            }
        }

        Showtime showtime = new Showtime();
        showtime.setDate(request.getDate());
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setCreatedAt(LocalDateTime.now());
        showtime.setUpdatedAt(LocalDateTime.now());
        showtime.setDeleted(false); // Explicitly set to false

        showtime = showtimeRepository.save(showtime);
        return mapper.map(showtime, ShowtimeDto.class);
    }

    private boolean isTimeConflict(Showtime existingShowtime, CreateShowtimeRequest request) {
        LocalTime newStartTime = request.getStartTime();
        LocalTime newEndTime = request.getEndTime();
        LocalTime existingStartTime = existingShowtime.getStartTime();
        LocalTime existingEndTime = existingShowtime.getEndTime();

        return (newStartTime.isBefore(existingEndTime) && newEndTime.isAfter(existingStartTime));
    }

    public ShowtimeDto updateShowtime(UpdateShowtimeRq rq) {
        Showtime showtime = showtimeRepository.findById(rq.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Not found Showtime"));
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
        Showtime updatedShowtime = showtimeRepository.save(showtime);
        return mapper.map(updatedShowtime, ShowtimeDto.class);
    }

    public void deleteShowtime(int id) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found Showtime to Delete"));
        showtime.setDeleted(true);
        showtimeRepository.save(showtime);
    }

    public ShowtimeDto getShowtimeById(int id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu"));

        ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);

        if (showtime.getMovie() != null) {
            MovieInfo movieInfo = mapper.map(showtime.getMovie(), MovieInfo.class);
            showtimeDto.setMovieInfo(movieInfo);
        }

        if (showtime.getScreen() != null) {
            ScreenDto screenDto = mapper.map(showtime.getScreen(), ScreenDto.class);
            if (showtime.getScreen().getCinema() != null) {
                CinemaDto cinemaDTO = mapper.map(showtime.getScreen().getCinema(), CinemaDto.class);
                screenDto.setCinemaDto(cinemaDTO);
            }
            showtimeDto.setScreen(screenDto);
        }

        return showtimeDto;
    }

    public List<Showtime> getShowtimeOutTime() {
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<Showtime> todayUpcoming = showtimeRepository.findShowtimeByDateEqualsAndEndTimeBefore(nowDate, nowTime);

        return todayUpcoming;
    }

    public List<ShowtimeDto> createBulkShowtimes(CreateBulkShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        System.out.println("Creating bulk showtimes for movie: " + movie.getTitle() + ", screen: " + screen.getScreenNumber());
        System.out.println("Date range: " + request.getStartDate() + " to " + request.getEndDate());
        System.out.println("Time: " + request.getStartTime() + " to " + request.getEndTime());
        System.out.println("Days of week: " + request.getDaysOfWeek());

        List<Showtime> createdShowtimes = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();

        while (!currentDate.isAfter(request.getEndDate())) {
            // Kiểm tra xem ngày hiện tại có trong danh sách ngày được chọn không
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String dayName = dayOfWeek.name();
            
            if (request.getDaysOfWeek().contains(dayName)) {
                System.out.println("Processing date: " + currentDate + " (Day: " + dayName + ")");
                
                // Kiểm tra xung đột thời gian với các showtime chưa bị xóa
                List<Showtime> existingShowtimes = showtimeRepository.findAllByScreenIdAndDateAndIsDeletedFalse(request.getScreenId(), currentDate);
                System.out.println("Found " + existingShowtimes.size() + " existing showtimes for this date");
                
                boolean hasConflict = false;
                
                for (Showtime existingShowtime : existingShowtimes) {
                    if (isTimeConflict(existingShowtime, request.getStartTime(), request.getEndTime())) {
                        hasConflict = true;
                        System.out.println("Time conflict detected with existing showtime: " + existingShowtime.getStartTime() + " - " + existingShowtime.getEndTime());
                        break;
                    }
                }
                
                if (!hasConflict) {
                    Showtime showtime = new Showtime();
                    showtime.setDate(currentDate);
                    showtime.setMovie(movie);
                    showtime.setScreen(screen);
                    showtime.setStartTime(request.getStartTime());
                    showtime.setEndTime(request.getEndTime());
                    showtime.setCreatedAt(LocalDateTime.now());
                    showtime.setUpdatedAt(LocalDateTime.now());
                    showtime.setDeleted(false); // Explicitly set to false
                    
                    showtime = showtimeRepository.save(showtime);
                    createdShowtimes.add(showtime);
                    System.out.println("Created showtime for " + currentDate + " with ID: " + showtime.getId());
                } else {
                    System.out.println("Skipped " + currentDate + " due to time conflict");
                }
            } else {
                System.out.println("Skipped " + currentDate + " (Day: " + dayName + ") - not in selected days");
            }
            
            currentDate = currentDate.plusDays(1);
        }

        System.out.println("Total showtimes created: " + createdShowtimes.size());

        return createdShowtimes.stream()
                .map(showtime -> mapper.map(showtime, ShowtimeDto.class))
                .collect(Collectors.toList());
    }

    private boolean isTimeConflict(Showtime existingShowtime, LocalTime newStartTime, LocalTime newEndTime) {
        LocalTime existingStartTime = existingShowtime.getStartTime();
        LocalTime existingEndTime = existingShowtime.getEndTime();

        return (newStartTime.isBefore(existingEndTime) && newEndTime.isAfter(existingStartTime));
    }

    public List<ShowtimeDto> getAllShowtimesForAdmin(Pageable pageable) {
        List<Showtime> allShowtimes = showtimeRepository.findByIsDeletedFalse();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allShowtimes.size());
        
        List<Showtime> pagedContent = allShowtimes.subList(start, end);
        Page<Showtime> allShowtime = new PageImpl<>(pagedContent, pageable, allShowtimes.size());

        return allShowtime.stream().map(showtime -> {
            ShowtimeDto showtimeDto = mapper.map(showtime, ShowtimeDto.class);
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

}
