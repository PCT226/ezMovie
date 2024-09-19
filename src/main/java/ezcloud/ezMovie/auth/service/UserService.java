package ezcloud.ezMovie.auth.service;

import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.auth.model.dto.UserInfo;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper mapper;
    public UserService(UserRepository userRepository) {
    }

    public User saveUser(User user){
        return userRepository.save(user);
    }

    public UserInfo updateUser(UserInfo userInfo){
        User user = userRepository.findById(userInfo.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userInfo.getId()));
        if (userRepository.existsByEmail(userInfo.getEmail()) &&
                !user.getEmail().equals(userInfo.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + userInfo.getEmail());
        }

            user.setEmail(userInfo.getEmail());
            user.setPhoneNumber(userInfo.getPhoneNumber());
            user.setUsername(userInfo.getUsername());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return mapper.map(user, UserInfo.class);
    }
    public List<UserInfo> getAll(){
        List<User> users=userRepository.findAllByIsDeleted(false);
        return users.stream().map(user -> mapper.map(user, UserInfo.class))
                .collect(Collectors.toList());
    }
    public UserInfo findById(UUID id){
        User user=userRepository.findById(id).orElseThrow(()->new RuntimeException("User not found with id: " + id));
        return mapper.map(user, UserInfo.class);
    }
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }


    @Override
    public UserDetails loadUserByUsername(String email){
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



    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }


    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
