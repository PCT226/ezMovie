package ezcloud.ezMovie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String body) throws MessagingException {
        // Tạo MimeMessage để gửi email có HTML
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        // Thiết lập người nhận, tiêu đề và nội dung (với định dạng HTML)
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true); // true để kích hoạt HTML

        // Set email gửi đi
        helper.setFrom("thanhpro2206@gmail.com");

        // Gửi email
        mailSender.send(mimeMessage);
    }
}
