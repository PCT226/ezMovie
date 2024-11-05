package ezcloud.ezMovie.auth.repository;

import ezcloud.ezMovie.auth.model.enities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByEmail(String email);

    User findByUsername(String username);
    User findByVerificationCode(String verificationCode);
    User findByResetPasswordCode(String resetPasswordCode);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAll();

    Page<User> findAllByIsDeleted(boolean isDeleted, Pageable pageable);
}
