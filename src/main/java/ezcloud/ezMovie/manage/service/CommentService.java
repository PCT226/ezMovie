package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.service.UserService;
import ezcloud.ezMovie.exception.MovieNotFound;
import ezcloud.ezMovie.manage.model.dto.CommentDto;
import ezcloud.ezMovie.manage.model.enities.Comment;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.repository.CommentRepository;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ModelMapper mapper;

    public Page<CommentDto> getCommentsByMovieId(Integer movieId, Pageable pageable) {
        // Kiểm tra movie có tồn tại không
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFound("Movie not found with ID: " + movieId);
        }
        
        Page<Comment> comments = commentRepository.findByMovieIdOrderByCreatedAtDesc(movieId, pageable);
        return comments.map(comment -> {
            CommentDto dto = mapper.map(comment, CommentDto.class);
            dto.setUsername(comment.getUser().getUsername());
            return dto;
        });
    }

    public Response<CommentDto> createComment(Integer movieId, String content, UUID userId) {
        // Kiểm tra movie có tồn tại không
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFound("Movie not found with ID: " + movieId));
        
        // Kiểm tra user có tồn tại không
        User user = userService.findUserById(userId);
        
        Comment comment = new Comment();
        comment.setMovie(movie);
        comment.setUser(user);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment savedComment = commentRepository.save(comment);
        
        CommentDto dto = mapper.map(savedComment, CommentDto.class);
        dto.setUsername(savedComment.getUser().getUsername());
        
        return new Response<>(0, dto);
    }

    public Response<CommentDto> updateComment(Integer commentId, String content, UUID userId) {
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found with ID: " + commentId);
        }
        
        // Kiểm tra user có quyền sửa comment không
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this comment");
        }
        
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment updatedComment = commentRepository.save(comment);
        
        CommentDto dto = mapper.map(updatedComment, CommentDto.class);
        dto.setUsername(updatedComment.getUser().getUsername());
        
        return new Response<>(0, dto);
    }

    public void deleteComment(Integer commentId, UUID userId) {
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found with ID: " + commentId);
        }
        
        // Kiểm tra user có quyền xóa comment không
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }
        
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    public Long getCommentCountByMovieId(Integer movieId) {
        return commentRepository.countByMovieId(movieId);
    }
} 