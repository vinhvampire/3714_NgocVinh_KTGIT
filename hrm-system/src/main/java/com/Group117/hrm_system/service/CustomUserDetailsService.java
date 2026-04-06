package com.Group117.hrm_system.service;

import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TaiKhoan tk = repo.findByUsername(username);

        if (tk == null) {
            throw new UsernameNotFoundException("Không tìm thấy user: " + username);
        }

        // TRẢ VỀ USER CHUẨN:
        return new User(
                tk.getUsername(),
                tk.getPassword(), // Lấy pass hash từ DB, KHÔNG dùng "123456" cứng
                tk.isTrangThaiTaiKhoan(), // enabled: true nếu tài khoản hoạt động
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + tk.getRole()))
        );
    }
}