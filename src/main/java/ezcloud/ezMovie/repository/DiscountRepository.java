package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount,Integer> {
    Optional<Discount> findByCode(String code);
}
