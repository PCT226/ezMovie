package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.manage.model.dto.SeatAdminDto;
import ezcloud.ezMovie.manage.model.dto.SeatDto;
import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Screen;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.model.payload.CreateSeatRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateSeatRequest;
import ezcloud.ezMovie.manage.repository.ScreenRepository;
import ezcloud.ezMovie.manage.repository.SeatRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeatService {
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    public Page<SeatDto> getSeatsByShowtimeId(Integer showtimeId, Pageable pageable) {
        List<SeatDto> listSeat = new ArrayList<>();

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Not found Showtime"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(), showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        List<Seat> availableSeats = seatRepository.findAllByScreenIdAndIsDeletedFalse(showtime.getScreen().getId());
        List<BookedSeat> bookedSeats = bookedSeatRepository.findBookedSeatsByShowtimeId(showtimeId);

        Set<Integer> bookedSeatIds = bookedSeats.stream()
                .map(bookedSeat -> bookedSeat.getSeat().getId())
                .collect(Collectors.toSet());

        for (Seat seat : availableSeats) {
            SeatDto seatDto = new SeatDto();
            seatDto.setSeatId(seat.getId());
            seatDto.setSeatNumber(seat.getSeatNumber());
            seatDto.setPrice(seat.getPrice());

            if (bookedSeatIds.contains(seat.getId())) {
                seatDto.setStatus("BOOKED");
            } else {
                seatDto.setStatus("AVAILABLE");
            }

            listSeat.add(seatDto);
        }

        redisTemplate.opsForValue().set("listSeat::" + showtimeId, listSeat);
        redisTemplate.expire("listSeat::" + showtimeId, ttlSeconds, TimeUnit.SECONDS);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), listSeat.size());
        return new PageImpl<>(listSeat.subList(start, end), pageable, listSeat.size());

    }

    public List<SeatAdminDto> findAllByScreenId(int screenId, Pageable pageable) {
        List<Seat> seats = seatRepository.findAllByScreenIdAndIsDeletedFalse(screenId, pageable);

        return seats.stream().map(seat -> {
            Screen screen = screenRepository.findById(screenId)
                    .orElseThrow(() -> new RuntimeException("Not found Screen"));
            Cinema cinema = screen.getCinema();
            SeatAdminDto seatDTO = new SeatAdminDto();
            seatDTO.setSeatId(seat.getId());
            seatDTO.setSeatNumber(seat.getSeatNumber());
            seatDTO.setPrice(seat.getPrice());
            seatDTO.setScreenName(String.valueOf(screen.getScreenNumber()));
            seatDTO.setScreenId(screen.getId());
            seatDTO.setCinemaName(cinema.getName());
            return seatDTO;
        }).collect(Collectors.toList());
    }

    public void createSeat(CreateSeatRequest request) {
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Not found Screen"));
        Seat seat = new Seat();
        seat.setCreatedAt(LocalDateTime.now());
        seat.setUpdatedAt(LocalDateTime.now());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setPrice(request.getPrice());
        seat.setScreen(screen);

        seatRepository.save(seat);
    }

    public Seat updateSeat(UpdateSeatRequest request) {
        Seat s1 = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new RuntimeException("Not found Seat"));
        s1.setUpdatedAt(LocalDateTime.now());
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new RuntimeException("Not found Screen"));
        s1.setSeatNumber(request.getSeatNumber());
        s1.setPrice(request.getPrice());
        s1.setScreen(screen);

        return seatRepository.save(s1);
    }

    public void deleteSeat(int seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Not found Seat"));
        seat.setDeleted(true);
        seatRepository.save(seat);
    }

    public void updateRedisCache(List<SeatDto> seats, Integer showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Not found Showtime"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(), showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        // Cập nhật Redis với danh sách ghế mới
        redisTemplate.opsForValue().set("listSeat::" + showtime.getScreen().getId(), seats);
        redisTemplate.expire("listSeat::" + showtimeId, ttlSeconds, TimeUnit.SECONDS);
    }
}
