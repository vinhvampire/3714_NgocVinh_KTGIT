package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.config.JwtService;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.service.ResetPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Cho phép gọi từ trình duyệt
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. API ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String user = request.get("username");
        String pass = request.get("password");

        System.out.println(">>> Đang thử đăng nhập cho user: " + user);

        try {
            // Xác thực thông tin đăng nhập từ Spring Security
            // Nếu sai mật khẩu hoặc user bị disable, dòng này sẽ ném ra AuthenticationException
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user, pass));

            // Tìm tài khoản và tạo Token
            TaiKhoan tk = taiKhoanRepository.findByUsername(user);
            String token = jwtService.generateToken(tk.getUsername(), tk.getRole());

            // Trả về thông tin cho Frontend lưu trữ
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", tk.getRole());
            response.put("username", tk.getUsername());

            System.out.println(">>> Đăng nhập THÀNH CÔNG cho user: " + user);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // In lỗi ra Console IntelliJ để biết tại sao bị 403
            System.err.println(">>> ĐĂNG NHẬP THẤT BẠI: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sai tài khoản hoặc mật khẩu: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println(">>> LỖI HỆ THỐNG: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi xử lý server"));
        }
    }

    // 2. API QUÊN MẬT KHẨU (GỬI MAIL) - Giữ nguyên logic cũ
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        // Không tiết lộ tài khoản có tồn tại hay không
        try {
            resetPasswordService.processForgotPassword(username);
        } catch (Exception ignored) { }
        return ResponseEntity.ok(Map.of("message", "Nếu tài khoản tồn tại, mã OTP đã được gửi."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (username == null || username.isBlank() ||
                otp == null || otp.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu thông tin bắt buộc."));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới tối thiểu 6 ký tự."));
        }

        TaiKhoan tk = taiKhoanRepository.findByUsername(username);
        if (tk == null ||
                tk.getResetToken() == null ||
                tk.getResetTokenExpiry() == null ||
                !tk.getResetToken().equals(otp) ||
                !tk.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không hợp lệ hoặc đã hết hạn."));
        }

        tk.setPassword(passwordEncoder.encode(newPassword));
        tk.setResetToken(null);
        tk.setResetTokenExpiry(null);
        taiKhoanRepository.save(tk);
        return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công."));
    }

    // 3. API XÁC NHẬN MÃ VÀ ĐỔI MẬT KHẨU MỚI - Giữ nguyên logic cũ
    // 3. API XÁC NHẬN MÃ VÀ ĐỔI MẬT KHẨU MỚI - ĐÃ KHÔI PHỤC LOGIC CHUẨN
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String token = request.get("token"); // Nhận token từ frontend
        String newPassword = request.get("newPassword");

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu username"));
        }
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu token"));
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu tối thiểu 6 ký tự"));
        }

        TaiKhoan tk = taiKhoanRepository.findByUsername(username);

        // Kiểm tra 4 điều kiện:
        // 1. User tồn tại
        // 2. Có token trong DB
        // 3. Token khớp với mã người dùng gửi lên
        // 4. Token còn hạn
        if (tk != null && tk.getResetToken() != null
                && tk.getResetToken().equals(token)
                && tk.getResetTokenExpiry().isAfter(LocalDateTime.now())) {

            // Mã hóa mật khẩu mới và lưu lại
            tk.setPassword(passwordEncoder.encode(newPassword));

            // Xóa mã reset sau khi dùng xong
            tk.setResetToken(null);
            tk.setResetTokenExpiry(null);
            taiKhoanRepository.save(tk);

            System.out.println(">>> RESET PASS thành công cho user: " + username);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Mã xác nhận không đúng hoặc đã hết hạn."));
    }
}