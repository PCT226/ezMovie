package ezcloud.ezMovie.auth.service;

import ezcloud.ezMovie.auth.model.dto.UserUpdate;
import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.exception.EmailNotFoundException;
import ezcloud.ezMovie.manage.model.enities.Screen;
import org.springframework.beans.factory.annotation.Autowired;
import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.auth.model.dto.UserInfo;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public UserUpdate updateUser(UUID id , UserUpdate userInfo){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

            if(userInfo.getUsername().isEmpty()||userInfo.getPhoneNumber().isEmpty()){
                throw new EmailAlreadyExistsException("Value not null");

            }
            user.setPhoneNumber(userInfo.getPhoneNumber());
            user.setUsername(userInfo.getUsername());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return mapper.map(user, UserUpdate.class);
    }
    public List<UserInfo> getAll(Pageable pageable){

        Page<User> users=userRepository.findAllByIsDeleted(false,pageable);
        return users.stream().map(user -> mapper.map(user, UserInfo.class))
                .collect(Collectors.toList());
    }
    public UserInfo findById(UUID id){
        User user=userRepository.findById(id)
                .orElseThrow(()->new UsernameNotFoundException("cc" + id));
        return mapper.map(user, UserInfo.class);
    }
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
       //userRepository.deleteById(id);
        user.setDeleted(true);
        userRepository.save(user);
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
            throw new EmailNotFoundException(email);
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
