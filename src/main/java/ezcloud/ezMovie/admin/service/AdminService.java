package ezcloud.ezMovie.admin.service;

import ezcloud.ezMovie.admin.auth.AdminUserDetails;
import ezcloud.ezMovie.admin.model.entities.Admin;
import ezcloud.ezMovie.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading admin by email: {}", email);
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Admin not found with email: {}", email);
                    return new UsernameNotFoundException("Admin not found with email: " + email);
                });
        logger.debug("Found admin: {}", admin.getEmail());
        return new AdminUserDetails(admin);
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Admin createAdmin(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    public void deleteAdmin(UUID id) {
        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin not found with id: " + id);
        }
        adminRepository.deleteById(id);
    }
} 