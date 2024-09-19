package ezcloud.ezMovie.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
}
