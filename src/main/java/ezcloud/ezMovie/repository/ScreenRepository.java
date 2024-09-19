package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen,Integer> {
    List<Screen> findAllByIsDeleted(boolean isDeleted);
}
