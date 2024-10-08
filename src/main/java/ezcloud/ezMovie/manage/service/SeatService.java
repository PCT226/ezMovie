package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.manage.model.dto.SeatDto;
import ezcloud.ezMovie.manage.model.enities.Screen;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.model.payload.CreateSeatRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateSeatRequest;
import ezcloud.ezMovie.manage.repository.ScreenRepository;
import ezcloud.ezMovie.manage.repository.SeatRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private TicketRepository ticketRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String SEAT_STATUS_KEY_PREFIX = "seat:status:";

    @Cacheable(value = "listSeat",key = "#showtimeId")
    public List<SeatDto> getSeatsByShowtimeId(Integer showtimeId) {
        List<SeatDto> listSeat= new ArrayList<>();

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Not found Showtime"));

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

        return listSeat;
    }

    // Lấy danh sách ghế từ Screen
    public List<SeatDto> findAllByScreenId(int screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new RuntimeException("Not found Screen"));

        List<Seat> seats = seatRepository.findAllByScreenIdAndIsDeletedFalse(screenId);

        // Kiểm tra trạng thái của từng ghế từ Redis và trả về DTO
        return seats.stream().map(seat -> {
//            String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seat.getId();
//            String seatStatus = redisTemplate.opsForValue().get(seatStatusKey);
//            String status= seat.getSeatStatus();
//
//            if ( status == null) {
//                status = "AVAILABLE"; // Mặc định là AVAILABLE nếu không có trong Redis
//            }

            // Tạo SeatDTO để trả về cho frontend
            SeatDto seatDTO = new SeatDto();
            seatDTO.setSeatId(seat.getId());
            seatDTO.setSeatNumber(seat.getSeatNumber());
            seatDTO.setPrice(seat.getPrice());
//            seatDTO.setStatus(status);

            return seatDTO;
        }).collect(Collectors.toList());
    }
    public Seat createSeat(CreateSeatRequest request){
        Screen screen= screenRepository.findById(request.getScreenId())
                .orElseThrow(()->new RuntimeException("Not found Screen"));
        Seat seat=new Seat();
        seat.setCreatedAt(LocalDateTime.now());
        seat.setUpdatedAt(LocalDateTime.now());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setPrice(request.getPrice());
        seat.setScreen(screen);
//        seat.setSeatStatus("AVAILABLE");
        Seat savedSeat = seatRepository.save(seat);

        // Cập nhật trạng thái ghế trong Redis khi tạo mới ghế
        String seatStatusKey = SEAT_STATUS_KEY_PREFIX + savedSeat.getId();
        redisTemplate.opsForValue().set(seatStatusKey, "AVAILABLE");

        return savedSeat;
    }
    public Seat updateSeat(UpdateSeatRequest request){
        Seat s1= seatRepository.findById(request.getSeatId())
                .orElseThrow(()->new RuntimeException("Not found Seat"));
        s1.setUpdatedAt(LocalDateTime.now());
        s1.setSeatNumber(request.getSeatNumber());
        s1.setPrice(request.getPrice());
        s1.setScreen(request.getScreen());
        Seat updatedSeat = seatRepository.save(s1);

        // Cập nhật lại Redis nếu ghế được thay đổi
        String seatStatusKey = SEAT_STATUS_KEY_PREFIX + updatedSeat.getId();
        redisTemplate.opsForValue().set(seatStatusKey, "AVAILABLE"); // Hoặc trạng thái khác nếu có

        return updatedSeat;
    }
    public void deleteSeat(int seatId){
        Seat seat=seatRepository.findById(seatId)
                .orElseThrow(()->new RuntimeException("Not found Seat"));
        seat.setDeleted(true);
        seatRepository.save(seat);
        // Xóa trạng thái ghế khỏi Redis khi ghế bị xóa
        String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
        redisTemplate.delete(seatStatusKey);
    }


    // Cập nhật trạng thái ghế sau khi giờ chiếu kết thúc
    public void updateSeatStatusAfterShowtime(String showtimeId) {
        // 1. Lấy danh sách ghế từ showtimeId
        Showtime showtime = showtimeRepository.findById(Integer.parseInt(showtimeId)).orElse(null);

        if (showtime != null) {
            List<Seat> seats = seatRepository.findAllByScreenIdAndIsDeletedFalse(showtime.getScreen().getId());

            // 2. Cập nhật trạng thái ghế sang AVAILABLE
//            for (Seat seat : seats) {
//                if(seat.getSeatStatus().equals("BOOKED")) {
//                    seat.setSeatStatus("AVAILABLE");
//                    seat.setUpdatedAt(LocalDateTime.now());
//                    seatRepository.save(seat);
//
//                    // Cập nhật Redis (nếu bạn sử dụng Redis để lưu trữ trạng thái ghế)
//                    String redisKey = SEAT_STATUS_KEY_PREFIX + seat.getId();
//                    redisTemplate.opsForValue().set(redisKey, "AVAILABLE");
//                }
//            }
        }
    }
}
