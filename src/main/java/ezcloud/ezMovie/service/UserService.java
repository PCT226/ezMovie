package ezcloud.ezMovie.service;

import ezcloud.ezMovie.model.enities.CustomUserDetail;
import ezcloud.ezMovie.model.enities.User;
import ezcloud.ezMovie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    public UserService(UserRepository userRepository) {
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
         User user= userRepository.findByEmail(email);
         if (user == null|| !user.isVerified()){
             throw new UsernameNotFoundException(email);
         }
         return new CustomUserDetail(user);
    }
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null|| !user.isVerified()) {
            throw new UsernameNotFoundException(email);
        }
        return new CustomUserDetail(user);
    }
    public User findByVerificationCode(String verificationCode) {
        return userRepository.findByVerificationCode(verificationCode);
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByResetPasswordCode(String resetCode) {
        return userRepository.findByResetPasswordCode(resetCode);
    }

    public User saveUser(User user){
        return userRepository.save(user);
    }
    public List<User> getAll(){
        return userRepository.findAll();
    }
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }


    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
