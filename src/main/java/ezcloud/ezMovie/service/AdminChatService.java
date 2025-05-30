package ezcloud.ezMovie.service;

import ezcloud.ezMovie.admin.model.entities.Admin;
import ezcloud.ezMovie.admin.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminChatService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin getFirstAdmin() {
        return adminRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No admin found"));
    }
} 