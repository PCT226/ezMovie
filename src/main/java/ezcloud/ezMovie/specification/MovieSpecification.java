package ezcloud.ezMovie.specification;

import ezcloud.ezMovie.manage.model.enities.Movie;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MovieSpecification {
    public static Specification<Movie> searchMovies(String title, String genre, String actor) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            if (genre != null && !genre.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("genre")), "%" + genre.toLowerCase() + "%"));
            }

            if (actor != null && !actor.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("actor")), "%" + actor.toLowerCase() + "%"));
            }

            // Nếu không có điều kiện nào, trả về true
            return predicates.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
