package com.Group117.hrm_system.config;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.entity.TaiKhoan;


public class UpgradingDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final TaiKhoanRepository taiKhoanRepository;

    public UpgradingDaoAuthenticationProvider(PasswordEncoder passwordEncoder,
                                              UserDetailsService userDetailsService,
                                              TaiKhoanRepository taiKhoanRepository) {
        super();
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        try {
            super.additionalAuthenticationChecks(userDetails, authentication);
        } catch (BadCredentialsException ex) {
            // Fallback: so sánh mật khẩu trực tiếp (trường hợp DB lưu plain text)
            String presentedPassword = (authentication.getCredentials() == null)
                    ? null
                    : authentication.getCredentials().toString();
            String storedPassword = userDetails.getPassword();

            if (presentedPassword != null && storedPassword != null) {
                String presentedTrimmed = presentedPassword.trim();
                String storedTrimmed = storedPassword.trim();

                if (presentedTrimmed.equals(storedTrimmed)) {
                    // Nếu mật khẩu chưa được mã hóa BCrypt → tự động upgrade
                    boolean isBcrypt = storedTrimmed.startsWith("$2a$")
                            || storedTrimmed.startsWith("$2b$")
                            || storedTrimmed.startsWith("$2y$");

                    if (!isBcrypt) {
                        TaiKhoan tk = taiKhoanRepository.findByUsername(userDetails.getUsername());
                        if (tk != null) {
                            tk.setPassword(getPasswordEncoder().encode(presentedTrimmed));
                            taiKhoanRepository.save(tk);
                        }
                    }
                    return; // xác thực thành công
                }
            }
            throw ex;
        }
    }
}