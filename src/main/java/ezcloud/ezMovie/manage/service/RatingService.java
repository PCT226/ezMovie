package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.service.UserService;
import ezcloud.ezMovie.exception.MovieNotFound;
import ezcloud.ezMovie.manage.model.dto.MovieRatingDto;
import ezcloud.ezMovie.manage.model.dto.RatingDto;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Rating;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.manage.repository.RatingRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RatingService {
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ModelMapper mapper;

    public Response<RatingDto> createOrUpdateRating(Integer movieId, Integer rating, UUID userId) {
        // Kiểm tra rating hợp lệ
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        // Kiểm tra movie có tồn tại không
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFound("Movie not found with ID: " + movieId));
        
        // Kiểm tra user có tồn tại không
        User user = userService.findUserById(userId);
        
        // Kiểm tra xem user đã rating phim này chưa
        Rating existingRating = ratingRepository.findByMovieIdAndUserId(movieId, userId).orElse(null);
        
        if (existingRating != null) {
            // Cập nhật rating hiện tại
            existingRating.setRating(rating);
            existingRating.setUpdatedAt(LocalDateTime.now());
            Rating updatedRating = ratingRepository.save(existingRating);
            
            RatingDto dto = mapper.map(updatedRating, RatingDto.class);
            dto.setUsername(updatedRating.getUser().getUsername());
            
            return new Response<>(0, dto);
        } else {
            // Tạo rating mới
            Rating newRating = new Rating();
            newRating.setMovie(movie);
            newRating.setUser(user);
            newRating.setRating(rating);
            newRating.setCreatedAt(LocalDateTime.now());
            newRating.setUpdatedAt(LocalDateTime.now());
            
            Rating savedRating = ratingRepository.save(newRating);
            
            RatingDto dto = mapper.map(savedRating, RatingDto.class);
            dto.setUsername(savedRating.getUser().getUsername());
            
            return new Response<>(0, dto);
        }
    }

    public Response<MovieRatingDto> getMovieRating(Integer movieId, UUID userId) {
        // Kiểm tra movie có tồn tại không
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFound("Movie not found with ID: " + movieId));
        
        // Lấy thông tin rating tổng hợp
        Double averageRating = ratingRepository.getAverageRatingByMovieId(movieId);
        Long totalRatings = ratingRepository.countByMovieId(movieId);
        
        // Lấy rating của user hiện tại (nếu có)
        Integer userRating = null;
        if (userId != null) {
            Rating userRatingEntity = ratingRepository.findByMovieIdAndUserId(movieId, userId).orElse(null);
            if (userRatingEntity != null) {
                userRating = userRatingEntity.getRating();
            }
        }
        
        MovieRatingDto dto = new MovieRatingDto();
        dto.setMovieId(movieId);
        dto.setMovieTitle(movie.getTitle());
        dto.setAverageRating(averageRating != null ? averageRating : 0.0);
        dto.setTotalRatings(totalRatings != null ? totalRatings.intValue() : 0);
        dto.setUserRating(userRating);
        
        return new Response<>(0, dto);
    }

    public void deleteRating(Integer movieId, UUID userId) {
        Rating rating = ratingRepository.findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));
        
        rating.setDeleted(true);
        ratingRepository.save(rating);
    }

    public RatingDto getUserRating(Integer movieId, UUID userId) {
        Rating rating = ratingRepository.findByMovieIdAndUserId(movieId, userId).orElse(null);
        
        if (rating == null) {
            return null;
        }
        
        RatingDto dto = mapper.map(rating, RatingDto.class);
        dto.setUsername(rating.getUser().getUsername());
        
        return dto;
    }
} 