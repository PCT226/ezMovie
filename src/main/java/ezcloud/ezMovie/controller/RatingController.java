package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import ezcloud.ezMovie.manage.model.dto.MovieRatingDto;
import ezcloud.ezMovie.manage.model.dto.RatingDto;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.service.RatingService;
import ezcloud.ezMovie.rateLimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@Tag(name = "Rating", description = "Rating management APIs")
public class RatingController {

    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    @Autowired
    private RatingService ratingService;

    @PostMapping("/movie/{movieId}")
    @Operation(summary = "Create or update rating", description = "Create a new rating or update existing rating for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating created/updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rating value (must be 1-5)"),
        @ApiResponse(responseCode = "404", description = "Movie not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Response<RatingDto>> createOrUpdateRating(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId,
            @Parameter(description = "Rating value (1-5)") @RequestBody Integer rating) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to create/update rating");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Object principal = authentication.getPrincipal();
            UUID userId;
            if (principal instanceof CustomUserDetail) {
                userId = ((CustomUserDetail) principal).getUser().getId();
            } else {
                logger.warn("Invalid principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.debug("Creating/updating rating for movie {} by user {}", movieId, userId);
            
            Response<RatingDto> response = ratingService.createOrUpdateRating(movieId, rating, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating/updating rating for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get movie rating", description = "Get average rating and total ratings for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved movie rating"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ResponseEntity<Response<MovieRatingDto>> getMovieRating(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID userId = null;
            
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetail) {
                    userId = ((CustomUserDetail) principal).getUser().getId();
                    logger.debug("Getting movie rating for movie {} by user {}", movieId, userId);
                } else {
                    logger.debug("Getting movie rating for movie {} (anonymous user)", movieId);
                }
            } else {
                logger.debug("Getting movie rating for movie {} (anonymous user)", movieId);
            }
            
            Response<MovieRatingDto> response = ratingService.getMovieRating(movieId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting movie rating for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/movie/{movieId}")
    @Operation(summary = "Delete rating", description = "Delete user's rating for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rating deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Rating not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteRating(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to delete rating");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Object principal = authentication.getPrincipal();
            UUID userId;
            if (principal instanceof CustomUserDetail) {
                userId = ((CustomUserDetail) principal).getUser().getId();
            } else {
                logger.warn("Invalid principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.debug("Deleting rating for movie {} by user {}", movieId, userId);
            
            ratingService.deleteRating(movieId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting rating for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/movie/{movieId}/user")
    @Operation(summary = "Get user rating", description = "Get current user's rating for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user rating"),
        @ApiResponse(responseCode = "404", description = "User rating not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Response<RatingDto>> getUserRating(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to get user rating");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Object principal = authentication.getPrincipal();
            UUID userId;
            if (principal instanceof CustomUserDetail) {
                userId = ((CustomUserDetail) principal).getUser().getId();
            } else {
                logger.warn("Invalid principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.debug("Getting user rating for movie {} by user {}", movieId, userId);
            
            RatingDto rating = ratingService.getUserRating(movieId, userId);
            if (rating == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(new Response<>(0, rating));
        } catch (Exception e) {
            logger.error("Error getting user rating for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 