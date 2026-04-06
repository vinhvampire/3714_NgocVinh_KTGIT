package com.Group117.hrm_system.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    @GetMapping("/")
    public String rootRedirect(HttpServletRequest request) {
        // Nếu đã có jwt_token trong cookie thì redirect vào dashboard
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("jwt_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                    return "redirect:/dashboard";
                }
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // Trả về login.html
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/forgot-password/sent")
    public String showForgotPasswordSentPage() {
        return "forgot-password-sent";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage() {
        return "reset-password";
    }

    @GetMapping("/home")
    public String showHomePage() {
        // Lưu ý: Với JWT, bạn sẽ dùng JavaScript ở trang này
        // để gửi Token lên mỗi khi load dữ liệu.
        return "index";
    }
}