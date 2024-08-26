package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.enities.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue,Integer> {
}
