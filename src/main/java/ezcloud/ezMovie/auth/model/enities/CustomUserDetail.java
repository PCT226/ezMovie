package ezcloud.ezMovie.auth.model.enities;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@AllArgsConstructor
public class CustomUserDetail implements UserDetails {

    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getActualUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // hoặc user.isAccountNonExpired() nếu bạn có field đó
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isVerified(); // hoặc true nếu bạn chưa dùng xác thực email
    }

    @Override
    public String toString() {
        return "CustomUserDetail{" +
                "email='" + user.getEmail() + '\'' +
                ", role='" + user.getRole() + '\'' +
                '}';
    }
}
