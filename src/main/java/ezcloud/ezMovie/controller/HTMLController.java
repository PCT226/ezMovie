package ezcloud.ezMovie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class HTMLController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Trả về tệp login.html trong thư mục templates
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // Trả về tệp register.html trong thư mục templates
    }

    // Các phương thức khác
}
