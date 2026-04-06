package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        // 1. Lấy username từ SecurityContext (đã được JwtFilter nạp vào)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Tìm tài khoản, từ đó lấy ra thông tin nhân viên nhờ liên kết OneToOne
        TaiKhoan tk = taiKhoanRepository.findByUsername(username);

        if (tk == null || tk.getNhanVien() == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin nhân viên");
        }

        // 3. Trả về thông tin nhân viên
        NhanVien nv = tk.getNhanVien();
        return ResponseEntity.ok(nv);
    }
}