package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import ezcloud.ezMovie.manage.model.dto.CommentDto;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.service.CommentService;
import ezcloud.ezMovie.rateLimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@Tag(name = "Comment", description = "Comment management APIs")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentService commentService;

    // DTO for comment content
    public static class CommentContentRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get comments by movie ID", description = "Retrieve all comments for a specific movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved comments"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ResponseEntity<Page<CommentDto>> getCommentsByMovieId(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        try {
            logger.debug("Getting comments for movie {}, page {}, size {}", movieId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentDto> comments = commentService.getCommentsByMovieId(movieId, pageable);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("Error getting comments for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/movie/{movieId}")
    @Operation(summary = "Create a comment", description = "Create a new comment for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Movie not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Response<CommentDto>> createComment(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId,
            @Parameter(description = "Comment content") @RequestBody CommentContentRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to create comment");
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
            
            logger.debug("Creating comment for movie {} by user {}", movieId, userId);
            
            Response<CommentDto> response = commentService.createComment(movieId, request.getContent(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating comment for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update a comment", description = "Update an existing comment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your comment"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Response<CommentDto>> updateComment(
            @Parameter(description = "Comment ID") @PathVariable Integer commentId,
            @Parameter(description = "Updated comment content") @RequestBody CommentContentRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to update comment");
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
            
            logger.debug("Updating comment {} by user {}", commentId, userId);
            
            Response<CommentDto> response = commentService.updateComment(commentId, request.getContent(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment", description = "Delete an existing comment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your comment"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "Comment ID") @PathVariable Integer commentId) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized access attempt to delete comment");
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
            
            logger.debug("Deleting comment {} by user {}", commentId, userId);
            
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting comment {}: {}", commentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/movie/{movieId}/count")
    @Operation(summary = "Get comment count", description = "Get the total number of comments for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved comment count"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    public ResponseEntity<Long> getCommentCount(
            @Parameter(description = "Movie ID") @PathVariable Integer movieId) {
        
        try {
            logger.debug("Getting comment count for movie {}", movieId);
            Long count = commentService.getCommentCountByMovieId(movieId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Error getting comment count for movie {}: {}", movieId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 