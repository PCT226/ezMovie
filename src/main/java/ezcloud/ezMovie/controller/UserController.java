package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.model.dto.UserInfo;
import ezcloud.ezMovie.model.enities.User;
import ezcloud.ezMovie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "user")
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping(value = "/getAll")
    public ResponseEntity<List<UserInfo>> getAll(){
        return ResponseEntity.ok(userService.getAll());
    }
    @GetMapping(value = "/{id}")
    public ResponseEntity<UserInfo> getById(@PathVariable int id){
        return ResponseEntity.ok(userService.findById(id));
    }
    @PutMapping("/update")
    public ResponseEntity<?> updateUser( @RequestBody UserInfo userInfo) {
        try {
            UserInfo updatedUser = userService.updateUser(userInfo);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (UsernameNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (EmailAlreadyExistsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id){
        userService.deleteUser(id);
        return ResponseEntity.ok("Xóa thành công");
    }

}
