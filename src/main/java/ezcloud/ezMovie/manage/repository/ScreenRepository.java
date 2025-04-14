package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Integer> {
    Page<Screen> findAllByIsDeleted(boolean isDeleted, Pageable pageable);
}
