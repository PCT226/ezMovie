package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.User;
import ezcloud.ezMovie.model.dto.UserInfo;
import ezcloud.ezMovie.model.enities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {
    User findByEmail(String email);

    User findByUsername(String username);
    User findByVerificationCode(String verificationCode);
    User findByResetPasswordCode(String resetPasswordCode);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAll();

    List<User> findAllByIsDeleted(boolean isDeleted);
}
