package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.enities.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen,Integer> {
}
